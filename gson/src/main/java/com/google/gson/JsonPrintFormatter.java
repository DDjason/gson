/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Formats Json in a nicely indented way with a specified print margin.
 * This printer tries to keep elements on the same line as much as possible
 * while respecting right margin.
 *
 * @author Inderjeet Singh
 */
final class JsonPrintFormatter implements JsonFormatter {

  private final int printMargin;
  private final int indentationSize;
  private final int rightMargin;
  private final boolean escapeHtmlChars;

  public static final int DEFAULT_PRINT_MARGIN = 80;
  public static final int DEFAULT_INDENTATION_SIZE = 2;
  public static final int DEFAULT_RIGHT_MARGIN = 4;

  JsonPrintFormatter() {
    this(true);
  }
  
  JsonPrintFormatter(boolean escapeHtmlChars) {
    this(DEFAULT_PRINT_MARGIN, DEFAULT_INDENTATION_SIZE, DEFAULT_RIGHT_MARGIN, escapeHtmlChars);
  }

  JsonPrintFormatter(int printMargin, int indentationSize, int rightMargin,
      boolean escapeHtmlChars) {
    this.printMargin = printMargin;
    this.indentationSize = indentationSize;
    this.rightMargin = rightMargin;
    this.escapeHtmlChars = escapeHtmlChars;
  }

  private class JsonWriter {
    private final Appendable writer;
    private StringBuilder line;
    private int level;
    JsonWriter(Appendable writer) {
      this.writer = writer;
      level = 0;
      line = new StringBuilder();
    }

    void key(String key) {
      getLine().append('"');
      getLine().append(key);
      getLine().append('"');
    }

    void value(String value) {
      getLine().append(value);
    }

    void fieldSeparator() throws IOException {
      getLine().append(':');
      breakLineIfNeeded();
    }

    void elementSeparator() throws IOException {
      getLine().append(',');
      breakLineIfNeeded();
    }

    void beginObject() throws IOException {
      ++level;
      breakLineIfNeeded();
      getLine().append('{');
    }

    void endObject() {
      getLine().append('}');
      --level;
    }

    void beginArray() throws IOException {
      ++level;
      breakLineIfNeeded();
      getLine().append('[');
    }

    void endArray() {
      getLine().append(']');
      --level;
    }

    private void breakLineIfNeeded() throws IOException {
      if (getLine().length() > printMargin - rightMargin) {
        finishLine();
      }
    }

    private void finishLine() throws IOException {
      if (line != null) {
        writer.append(line).append("\n");
      }
      line = null;
    }

    private StringBuilder getLine() {
      if (line == null) {
        createNewLine();
      }
      return line;
    }

    private void createNewLine() {
      line = new StringBuilder();
      for (int i = 0; i < level; ++i) {
        for (int j = 0; j < indentationSize; ++j) {
          line.append(' ');
        }
      }
    }
  }

  private class PrintFormattingVisitor implements JsonElementVisitor {
    private final Map<Integer, Boolean> firstArrayElement;
    private final Map<Integer, Boolean> firstObjectMember;
    private final JsonWriter writer;
    private final boolean serializeNulls;
    private int level = 0;

    PrintFormattingVisitor(JsonWriter writer, boolean serializeNulls) {
      this.writer = writer;
      this.serializeNulls = serializeNulls;
      this.firstArrayElement = new HashMap<Integer, Boolean>();
      this.firstObjectMember = new HashMap<Integer, Boolean>();
    }

    private void addCommaCheckingFirst(Map<Integer, Boolean> first) throws IOException {
      if (first.get(level) != Boolean.FALSE) {
        first.put(level, false);
      } else {
        writer.elementSeparator();
      }
    }

    public void startArray(JsonArray array) throws IOException {
      firstArrayElement.put(++level, true);
      writer.beginArray();
    }

    public void visitArrayMember(JsonArray parent, JsonPrimitive member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst(firstArrayElement);
      writer.value(member.toString());
    }

    public void visitArrayMember(JsonArray parent, JsonArray member, 
        boolean first) throws IOException {
      addCommaCheckingFirst(firstArrayElement);
    }

    public void visitArrayMember(JsonArray parent, JsonObject member, 
        boolean first) throws IOException {
      addCommaCheckingFirst(firstArrayElement);
    }

    public void visitNullArrayMember(JsonArray parent, boolean isFirst) throws IOException {
      addCommaCheckingFirst(firstArrayElement);
    }

    public void endArray(JsonArray array) {
      level--;
      writer.endArray();
    }

    public void startObject(JsonObject object) throws IOException {
      firstObjectMember.put(level, true);
      writer.beginObject();
    }

    public void visitObjectMember(JsonObject parent, String memberName, JsonPrimitive member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst(firstObjectMember);
      writer.key(memberName);
      writer.fieldSeparator();
      writer.value(member.toString());
    }

    public void visitObjectMember(JsonObject parent, String memberName, JsonArray member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst(firstObjectMember);
      writer.key(memberName);
      writer.fieldSeparator();
    }

    public void visitObjectMember(JsonObject parent, String memberName, JsonObject member, 
        boolean isFirst) throws IOException {
      addCommaCheckingFirst(firstObjectMember);
      writer.key(memberName);
      writer.fieldSeparator();
    }

    public void visitNullObjectMember(JsonObject parent, String memberName, 
        boolean isFirst) throws IOException {
      if (serializeNulls) {
        visitObjectMember(parent, memberName, (JsonObject) null, isFirst);
      }
    }
    
    public void endObject(JsonObject object) {
      writer.endObject();
    }

    public void visitPrimitive(JsonPrimitive primitive) {
      writer.value(primitive.toString());
    }

    public void visitNull() {
      writer.value("null");
    }
  }

  public void format(JsonElement root, Appendable writer, 
      boolean serializeNulls) throws IOException {
    if (root == null) {
      return;
    }
    JsonWriter jsonWriter = new JsonWriter(writer);
    JsonElementVisitor visitor = new JsonEscapingVisitor(
        new PrintFormattingVisitor(jsonWriter, serializeNulls), escapeHtmlChars);    
    JsonTreeNavigator navigator = new JsonTreeNavigator(visitor, serializeNulls);
    navigator.navigate(root);
    jsonWriter.finishLine();
  }
}
