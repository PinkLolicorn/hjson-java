/*******************************************************************************
 * Copyright (c) 2015-2017 Christian Zangl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package eu.okaeri.hjson;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

class HjsonWriter {

  private final IHjsonDsfProvider[] dsfProviders;
  private final boolean outputComments;
  private final boolean outputEmptyLines;
  private final boolean bracesSameLine;
  private final boolean allowCondense;
  private final boolean allowMultiVal;
  private final boolean emitRootBraces;
  private final String space;
  private final String commentSpace;
  private final String eol;

  private static final Pattern NEEDS_ESCAPE_NAME = Pattern.compile("[,{\\[}\\]\\s:#\"']|//|/\\*");

  public HjsonWriter(HjsonOptions options) {
    if (options!=null) {
      dsfProviders=options.getDsfProviders();
      bracesSameLine=options.bracesSameLine();
      allowCondense=options.getAllowCondense();
      allowMultiVal=options.getAllowMultiVal();
      space=options.getSpace();
      commentSpace=options.getCommentSpace();
      outputComments=options.getOutputComments();
      outputEmptyLines=options.getOutputEmptyLines();
      emitRootBraces=options.getEmitRootBraces();
      eol=options.getNewLine();
    } else {
      dsfProviders=new IHjsonDsfProvider[0];
      bracesSameLine=false;
      allowCondense=true;
      allowMultiVal=true;
      emitRootBraces=false;
      space="  ";
      commentSpace="";
      outputComments=false;
      outputEmptyLines=false;
      eol=JsonValue.eol;
    }
  }

  public HjsonWriter(HjsonOptions options, boolean outputComments) {
    this((options==null?new HjsonOptions():options).setOutputComments(outputComments));
  }

  private void nl(Writer tw, int level) throws IOException {
    tw.write(this.eol);
    indent(tw, level);
  }

  private void indent(Writer tw, int level) throws IOException {
    for (int i=0; i<level; i++)
      tw.write(space);
  }

  private void indentComment(Writer tw) throws IOException {
    tw.write(commentSpace);
  }

  // Backwards compatibility. Could be removed.
  public void save(JsonValue value, Writer tw, int level, String separator, boolean noIndent) throws IOException {
    save(value, tw, level, separator, noIndent, false);
  }

  public void save(JsonValue value, Writer tw, int level, String separator, boolean noIndent, boolean forceQuotes) throws IOException {
    if (value==null) {
      tw.write(separator);
      tw.write("null");
      return;
    }

    // check for DSF
    final String dsfValue=HjsonDsf.stringify(dsfProviders, value);
    if (dsfValue!=null) {
      tw.write(separator);
      tw.write(dsfValue);
      return;
    }

    // Handle special cases at level 0.
    if (level==0) {
      // Write the empty lines, if applicable.
      if (outputEmptyLines) {
        for (int i=0; i<value.getNumLines(); i++) {
          tw.write('\n');
        }
      }
      // Write the header, if applicable.
      if (outputComments && value.hasBOLComment()) {
        writeHeader(tw, value, level);
      }
    }

    switch (value.getType()) {
      case OBJECT:
        JsonObject obj=value.asObject();
        writeObject(obj, tw, level, separator, noIndent);
        break;
      case ARRAY:
        JsonArray arr=value.asArray();
        writeArray(arr, tw, level, separator, noIndent);
        break;
      case BOOLEAN:
        tw.write(separator);
        tw.write(value.isTrue()?"true":"false");
        break;
      case STRING:
        tw.write(separator);
        writeString(value.asString(), tw, level, forceQuotes);
        break;
      default:
        tw.write(separator);
        if (forceQuotes) tw.write('"');
        tw.write(value.toString());
        if (forceQuotes) tw.write('"');
        break;
    }

    // Write any following comments.
    if (outputComments && value.hasEOLComment()) {
      writeEOLComment(tw, value, level);
    }
  }
  private void writeObject(JsonObject obj, Writer tw, int level, String separator, boolean noIndent) throws IOException {
    // Start the beginning of the container.
    boolean emitBraces = emitBraces(obj, level);
    if (!emitBraces) openContainer(tw, noIndent, obj.isCondensed(), level, separator, '{');

    int index=0;
    for (JsonObject.Member pair : obj) {
      if (outputEmptyLines) {
        for (int i=0; i<pair.getValue().getNumLines(); i++) {
          tw.write('\n');
        }
      }
      if (outputComments && pair.getValue().hasBOLComment()) {
        writeBOLComment(tw, pair.getValue(), level);
      }

      handleContainerLines(tw, obj.isCondensed(), index, level, obj.getLineLength());
      tw.write(escapeName(pair.getName()));
      tw.write(":");
      boolean forceQuoteValue = forceQuoteValue(pair.getValue(), obj, outputComments);
      save(pair.getValue(), tw, level+1, " ", false, forceQuoteValue);
      index++;
    }
    boolean forceNl=false;
    // Put interior comments at the bottom.
    if (outputComments && obj.hasInteriorComment()) {
      writeInteriorComment(tw, obj, level);
      forceNl=true;
    }
    // We've reached the end of the container. Close it off.
    if (!emitBraces) closeContainer(tw, forceNl, obj.isCondensed(), obj.size(), level, '}');
  }

  private void writeArray(JsonArray arr, Writer tw, int level, String separator, boolean noIndent) throws IOException {
    // Start the beginning of the container.
    openContainer(tw, noIndent, arr.isCondensed(), level, separator, '[');

    int n=arr.size();
    for (int i=0; i<n; i++) {
      JsonValue element = arr.get(i);
      if (outputEmptyLines) {
        for (int j=0; j<element.getNumLines(); j++) {
          tw.write('\n');
        }
      }
      if (this.outputComments && element.hasBOLComment()) {
        writeBOLComment(tw, element, level);
      }
      handleContainerLines(tw, arr.isCondensed(), i, level, arr.getLineLength());
      // Multiple strings in an array would require quotes.
      boolean forceQuoteArray = forceQuoteArray(element, arr, outputComments);
      save(element, tw, level+1, "", true, forceQuoteArray);
    }
    boolean forceNl=false;
    // Put the interior comments at the bottom.
    if (outputComments && arr.hasInteriorComment()) {
      writeInteriorComment(tw, arr, level);
      forceNl=true;
    }
    // We've reached the end of the container. Close it off.
    closeContainer(tw, forceNl, arr.isCondensed(), n, level, ']');
  }

  private boolean emitBraces(JsonObject obj, int level) {
    return emitRootBraces && level==0 && !(obj.hasBOLComment() && outputComments);
  }

  private void openContainer(Writer tw, boolean noIndent, boolean condensed, int level, String separator, char openWith) throws IOException {
    if (!noIndent) { if (bracesSameLine || condensed) tw.write(separator); else nl(tw, level); }
    tw.write(openWith);
  }

  private void writeHeader(Writer tw, JsonValue value, int level) throws IOException {
    writeComment(value.getBOLComment(), tw, level);
    nl(tw, level);
  }

  private void writeBOLComment(Writer tw, JsonValue value, int level) throws IOException {
    nl(tw, level+1);
    writeComment(value.getBOLComment(), tw, level+1);
  }

  private void writeInteriorComment(Writer tw, JsonValue value, int level) throws IOException {
    nl(tw, level+1);
    writeComment(value.getInteriorComment(), tw, level+1);
  }

  private void writeEOLComment(Writer tw, JsonValue value, int level) throws IOException {
    if (level==0) {
      // if level==0, this is a footer.
      nl(tw, level);
      writeComment(value.getEOLComment(), tw, level);
    } else {
      // At EOL; no need to space more or less than once.
      tw.write(' ');
      tw.write(value.getEOLComment());
    }
  }

  private void handleContainerLines(Writer tw, boolean compact, int index, int level, int lineLength) throws IOException {
    if (!allowMultiVal) {
      nl(tw, level+1);
    } else if (lineLength==0 || index%lineLength==0) {
      // NL every (lineLength) # lines.
      if (!(compact && allowCondense)) {
        nl(tw, level+1);
      } else if (index>0) { // Manually separate.
        tw.write(", ");
      } else {
        tw.write(' ');
      }
    } else {
      tw.write(", ");
    }
  }

  private void closeContainer(Writer tw, boolean forceNl, boolean compact, int size, int level, char closeWith) throws IOException {
    if (forceNl) {
      nl(tw, level);
    } else if (size>0) {
      if (compact && allowCondense && allowMultiVal) tw.write(' ');
      else nl(tw, level);
    }
    tw.write(closeWith);
  }

  private static String escapeName(String name) {
    if (name.length()==0 || NEEDS_ESCAPE_NAME.matcher(name).find())
      return "\""+JsonWriter.escapeString(name)+"\"";
    else
      return name;
  }

  private void writeString(String value, Writer tw, int level, boolean forceQuotes) throws IOException {
    if (value.length()==0) { tw.write("\"\""); return; }

    char left=value.charAt(0), right=value.charAt(value.length()-1);
    char left1=value.length()>1?value.charAt(1):'\0';
    boolean doEscape=false;
    char[] valuec=value.toCharArray();
    for(char ch : valuec) {
      if (needsQuotes(ch)) { doEscape=true; break; }
    }

    if (doEscape ||
      HjsonParser.isWhitespace(left) || HjsonParser.isWhitespace(right) ||
      left=='"' ||
      left=='\'' ||
      left=='#' ||
      left=='/' && (left1=='*' || left1=='/') ||
      JsonValue.isPunctuatorChar(left) ||
      HjsonParser.tryParseNumber(value, true)!=null ||
      startsWithKeyword(value))
    {
      // If the String contains no control characters, no quote characters, and no
      // backslash characters, then we can safely slap some quotes around it.
      // Otherwise we first check if the String can be expressed in multiline
      // format or we must replace the offending characters with safe escape
      // sequences.

      boolean noEscape=true;
      for(char ch : valuec) { if (needsEscape(ch)) { noEscape=false; break; } }
      if (noEscape) { tw.write("\""+value+"\""); return; }

      boolean noEscapeML=true, allWhite=true;
      for(char ch : valuec) {
        if (needsEscapeML(ch)) { noEscapeML=false; break; }
        else if (!HjsonParser.isWhitespace(ch)) allWhite=false;
      }
      if (noEscapeML && !allWhite && !value.contains("'''")) writeMLString(value, tw, level);
      else tw.write("\""+JsonWriter.escapeString(value)+"\"");
    }
    else {
      if (forceQuotes) tw.write('"');
      tw.write(value);
      if (forceQuotes) tw.write('"');
    }
  }

  private void writeMLString(String value, Writer tw, int level) throws IOException {
    String[] lines=value.replace("\r", "").split("\n", -1);

    if (lines.length==1) {
      tw.write("'''");
      tw.write(lines[0]);
      tw.write("'''");
    }
    else {
      level++;
      nl(tw, level);
      tw.write("'''");

      for (String line : lines) {
        nl(tw, line.length()>0?level:0);
        tw.write(line);
      }
      nl(tw, level);
      tw.write("'''");
    }
  }

  private void writeComment(String comment, Writer tw, int level) throws IOException {
    String[] lines = comment.split("\r?\n");
    // The first line is already indented. No nl() needed.
    indentComment(tw);
    tw.write(lines[0]);

    // The rest of the lines are not.
    for (int i=1; i<lines.length; i++) {
      nl(tw, level);
      indentComment(tw);
      tw.write(lines[i]);
    }
  }

  private static boolean startsWithKeyword(String text) {
    int p;
    if (text.startsWith("true") || text.startsWith("null")) p=4;
    else if (text.startsWith("false")) p=5;
    else return false;
    while (p<text.length() && HjsonParser.isWhitespace(text.charAt(p))) p++;
    if (p==text.length()) return true;
    char ch=text.charAt(p);
    return ch==',' || ch=='}' || ch==']' || ch=='#' || ch=='/' && (text.length()>p+1 && (text.charAt(p+1)=='/' || text.charAt(p+1)=='*'));
  }

  private static boolean forceQuoteArray(JsonValue value, JsonArray array, boolean outputComments) {
    return value.isString() && (array.isCondensed() || array.getLineLength()>1 || (outputComments && value.hasEOLComment()));
  }

  // Technically different methods.
  private static boolean forceQuoteValue(JsonValue value, JsonObject object, boolean outputComments) {
    return value.isString() && (object.isCondensed() || object.getLineLength()>1 || (outputComments && value.hasEOLComment()));
  }

  private static boolean needsQuotes(char c) {
    switch (c) {
      case '\t':
      case '\f':
      case '\b':
      case '\n':
      case '\r':
        return true;
      default:
        return false;
    }
  }

  private static boolean needsEscape(char c) {
    switch (c) {
      case '\"':
      case '\\':
        return true;
      default:
        return needsQuotes(c);
    }
  }

  private static boolean needsEscapeML(char c) {
    switch (c) {
      case '\n':
      case '\r':
      case '\t':
        return false;
      default:
        return needsQuotes(c);
    }
  }
}
