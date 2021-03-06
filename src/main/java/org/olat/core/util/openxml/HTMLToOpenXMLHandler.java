/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.util.openxml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.olat.core.util.openxml.OpenXMLDocument.ListParagraph;
import org.olat.core.util.openxml.OpenXMLDocument.Style;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Convert HTML code to OpenXML
 * 
 * 
 * Initial date: 05.09.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class HTMLToOpenXMLHandler extends DefaultHandler {

	private boolean latex = false;
	private StringBuilder textBuffer;
	
	private final OpenXMLDocument factory;
	
	private List<Node> content = new ArrayList<Node>();
	private Deque<StyleStatus> styleStack = new ArrayDeque<StyleStatus>();
	
	private Table currentTable;
	private Element currentParagraph;
	private ListParagraph currentListParagraph;
	
	public HTMLToOpenXMLHandler(OpenXMLDocument document, Element paragraph) {
		this.factory = document;
		this.currentParagraph = paragraph;
	}
	
	/**
	 * Flush the text if a new paragraph is created. Trailing text is flushed
	 * in the previous paragraph.
	 * @param create
	 * @return
	 */
	private Element getCurrentParagraph(boolean create) {
		if(create || currentParagraph == null) {
			//flush the text
			if(textBuffer != null) {
				flushText();
				addContent(currentParagraph);
			}
			currentParagraph = factory.createParagraphEl();
		}
		return currentParagraph;
	}
	
	private Element getCurrentListParagraph(boolean create) {
		if(create || currentParagraph == null) {
			//flush the text
			if(textBuffer != null) {
				flushText();
				addContent(currentParagraph);
			}
			currentParagraph = factory.createListParagraph(currentListParagraph);
		}
		return currentParagraph;
	}
	
	private void closeParagraph() {
		flushText();
		currentParagraph = addContent(currentParagraph);
		textBuffer = null;
		latex = false;
	}
	
	private Element addContent(Node element) {
		if(element == null) return null;
		
		if(currentTable != null) {
			currentTable.getCurrentCell().appendChild(element);
		} else {
			content.add(element);
		}
		return null;
	}
	
	private void flushText() {
		if(textBuffer == null) return;
		
		if(latex) {
			//begin a new paragraph
			if(currentParagraph != null) {
				currentParagraph = addContent(currentParagraph);
			}
			List<Node> nodes = factory.convertLaTeX(textBuffer.toString());
			for(Node node:nodes) {
				addContent(node);
			}
		} else {
			Element currentRun = getCurrentRun();
			String content = textBuffer.toString();
			if(content.length() > 0 && Character.isSpaceChar(content.charAt(0))) {
				currentRun.appendChild(factory.createPreserveSpaceEl());
			}
			currentRun.appendChild(factory.createTextEl(content));
			if(content.length() > 1 && Character.isSpaceChar(content.charAt(content.length() - 1))) {
				currentRun.appendChild(factory.createPreserveSpaceEl());
			}
		}
		latex = false;
		textBuffer = null;
	}
	
	/**
	 * Get or create a run on the current paragraph
	 * @return
	 */
	private Element getCurrentRun() {
		Element paragraphEl;
		if(currentParagraph == null) {
			paragraphEl = currentParagraph = factory.createParagraphEl();
		} else {
			paragraphEl = currentParagraph;
		}
		Node lastChild = paragraphEl.getLastChild();
		if(lastChild != null && "w:r".equals(lastChild.getNodeName())) {
			return (Element)lastChild;
		}
		return (Element)paragraphEl.appendChild(factory.createRunEl(null));
	}

	private Style[] setTextPreferences(String cssStyles) {
		if(cssStyles == null) {
			return setTextPreferences();
		} else {
			List<Style> styles = new ArrayList<Style>(4);
			if(cssStyles.contains("bold")) styles.add(Style.bold);
			if(cssStyles.contains("italic")) styles.add(Style.italic);
			if(cssStyles.contains("underline")) styles.add(Style.underline);
			if(cssStyles.contains("line-through")) styles.add(Style.strike);
			return setTextPreferences(styles.toArray(new Style[styles.size()]));
		}
	}
	
	/**
	 * Create a new run with preferences
	 */
	private Style[] setTextPreferences(Style... styles) {
		Node runPrefs = getRunForTextPreferences();
		factory.createRunPrefsEl(runPrefs, styles);
		return styles;
	}
	
	private Style[] unsetTextPreferences(Style... styles) {
		Node runPrefs = getRunForTextPreferences();
		factory.createRunReversePrefsEl(runPrefs, styles);
		return styles;
	}
	
	private Node getRunForTextPreferences() {
		Element paragraphEl = getCurrentParagraph(false);
		
		Node runPrefs = null;
		Node run = paragraphEl.getLastChild();
		if(run != null && "w:r".equals(run.getNodeName())) {
			Node prefs = run.getLastChild();
			if("w:rPr".equals(prefs.getNodeName())){
				runPrefs = prefs;
			}
		}
		
		if(runPrefs == null) {
			run = paragraphEl.appendChild(factory.createRunEl(null));
			runPrefs = run.appendChild(factory.createRunPrefsEl());
		}
	
		if(!"w:rPr".equals(runPrefs.getNodeName())){
			runPrefs = run.appendChild(factory.createRunPrefsEl());
		}
		return runPrefs;
	}
	
	public Style[] getCurrentStyle() {
		if(styleStack.isEmpty()) return null;
		return styleStack.getLast().getStyles();
	}
	
	public Style[] popStyle(String tag) {
		StyleStatus status = styleStack.pollLast();
		if(status != null && status.getTag().equals(tag)) {
			return status.getStyles();
		}
		return null;
	}
	
	private void setImage(String path) {
		Element imgEl = factory.createImageEl(path);
		if(imgEl != null) {
			Element runEl = factory.createRunEl(Collections.singletonList(imgEl));
			Element paragrapheEl = getCurrentParagraph(false);
			paragrapheEl.appendChild(runEl);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		String tag = localName.toLowerCase();
		if("p".equalsIgnoreCase(tag)) {
			getCurrentParagraph(true);
		} else if("span".equalsIgnoreCase(tag)) {
			flushText();

			Style[] styles = null;
			String cl = attributes.getValue("class");
			if("math".equals(cl)) {
				latex = true;
			} else {
				String cssStyles = attributes.getValue("style");
				styles = setTextPreferences(cssStyles);
			}
			styleStack.add(new StyleStatus(tag, styles));
		} else if("br".equals(tag)) {
			closeParagraph();
		} else if("em".equalsIgnoreCase(tag)) {
			flushText();
			Style[] styles = setTextPreferences(Style.italic);
			styleStack.add(new StyleStatus(tag, styles));
		} else if("strong".equalsIgnoreCase(tag)) {
			flushText();
			Style[] styles = setTextPreferences(Style.bold);
			styleStack.add(new StyleStatus(tag, styles));
		} else if("img".equals(tag)) {
			String path = attributes.getValue("src");
			setImage(path);
		} else if("table".equalsIgnoreCase(tag)) {
			closeParagraph();
			currentTable = new Table();
		} else if("tr".equals(tag)) {
			currentTable.addRowEl();
		} else if("td".equals(tag) || "th".equals(tag)) {
			int colspan = OpenXMLUtils.getSpanAttribute("colspan", attributes);
			int rowspan = OpenXMLUtils.getSpanAttribute("rowspan", attributes);
			currentTable.addCellEl(colspan, rowspan);
		} else if("ul".equals(tag) || "ol".equals(tag)) {
			currentListParagraph = factory.createListParagraph();
		} else if("li".equals(tag)) {
			getCurrentListParagraph(true);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		if(textBuffer == null) {
			textBuffer = new StringBuilder();
		}
		textBuffer.append(ch, start, length);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) {
		String tag = localName.toLowerCase();
		if("p".equals(tag)) {
			closeParagraph();
		//flush text nodes to current paragraph
		} else if("span".equals(tag) ) {
			flushText();
			Style[] currentStyles = popStyle(tag);
			unsetTextPreferences(currentStyles);
		} else if("em".equalsIgnoreCase(tag)) {
			flushText();
			unsetTextPreferences(Style.italic);
			popStyle(tag);
		} else if("strong".equalsIgnoreCase(tag)) {
			flushText();
			unsetTextPreferences(Style.bold);
			popStyle(tag);
		}  else if("table".equals(tag)) {
			if(currentTable != null) {
				content.add(currentTable.getTableEl());
			}
			currentTable = null;
			currentParagraph = null;
		} else if("td".equals(tag) || "th".equals(tag)) {
			flushText();
			currentParagraph = addContent(currentParagraph);
		} else if("tr".equals(tag)) {
			if(currentTable != null) {
				currentTable.closeRow();
			}
			textBuffer = null;
			latex = false;
			currentParagraph = null;
		} else if("ul".equals(tag) || "ol".equals(tag)) {
			closeParagraph();
			currentListParagraph = null;
		} else if("li".equals(tag)) {
			//do nothing
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
		//clean up trailing text and pack it in a last paragraph
		closeParagraph();

		for(Node node:content) {
			factory.getCursor().appendChild(node);
		}
	}
	
	private static class StyleStatus {
		private final String tag;
		private final Style[] styles;
		
		public StyleStatus(String tag, Style[] styles) {
			this.tag = tag;
			this.styles = styles;
		}
		
		public String getTag() {
			return tag;
		}
		
		public Style[] getStyles() {
			return styles;
		}
	}
	
	private class Table {
		private final Element tableEl;
		
		private int nextCol;
		private Node currentRowEl;
		private Element currentCellEl;
		
		private Span[] rowSpans = new Span[128];
		
		public Table() {
			tableEl = factory.createTable();
		}

		public Element getTableEl() {
			return tableEl;
		}
		
		public Node addRowEl() {
			for(int i=rowSpans.length; i-->0; ) {
				if(rowSpans[i] != null) {
					rowSpans[i].unDone();
				}
			}
			
			nextCol = 0;
			currentRowEl = tableEl.getOwnerDocument().createElement("w:tr");	
			return  tableEl.appendChild(currentRowEl);
		}
		
		public void closeRow() {
			closeCell(rowSpans.length-1);
		}
		
		/*
<w:tc>
	<w:tcPr>
    <w:gridSpan w:val="2" />
    <w:vMerge w:val="restart" />
		 */
		public Node addCellEl(int colSpan, int rowSpan) {
			nextCol += closeCell(nextCol);
			
			currentCellEl = currentRowEl.getOwnerDocument().createElement("w:tc");
			
			Node prefs = null;
			if(colSpan > 1) {
				prefs = currentCellEl.appendChild(currentCellEl.getOwnerDocument().createElement("w:tcPr"));
				Element gridSpan = (Element)prefs.appendChild(prefs.getOwnerDocument().createElement("w:gridSpan"));
				gridSpan.setAttribute("w:val", Integer.toString(colSpan));
			}
			
			if(rowSpan > 1) {
				prefs = prefs != null ? prefs : currentCellEl.appendChild(currentCellEl.getOwnerDocument().createElement("w:tcPr"));
				Element vMerge = (Element)prefs.appendChild(prefs.getOwnerDocument().createElement("w:vMerge"));
				vMerge.setAttribute("w:val", "restart");
			}
			
			if(colSpan == 1 && rowSpan == 1) {
				rowSpans[nextCol] = Span.OneOnOne;
			} else {
				rowSpans[nextCol] = new Span(colSpan, rowSpan);
			}

			nextCol += (colSpan <= 1 ? 1 : colSpan);
			return currentRowEl.appendChild(currentCellEl);
		}
		
		public int closeCell(int lastIndex) {
			for(int i=lastIndex+1; i-->0; ) {
				Span span = rowSpans[i];
				if(span != null) {
					if(span.getRowSpan() > 1 && !span.isDone()) {
						currentCellEl = (Element)currentRowEl.appendChild(currentRowEl.getOwnerDocument().createElement("w:tc"));
						Node prefs = currentCellEl.appendChild(currentCellEl.getOwnerDocument().createElement("w:tcPr"));

						if(span.getColSpan() > 1) {
							Element gridSpan = (Element)prefs.appendChild(prefs.getOwnerDocument().createElement("w:gridSpan"));
							gridSpan.setAttribute("w:val", Integer.toString(span.getColSpan()));
						}
						prefs.appendChild(prefs.getOwnerDocument().createElement("w:vMerge"));
						
						currentCellEl.appendChild(currentCellEl.getOwnerDocument().createElement("w:p"));
						span.decrementRowSpan();
						return span.getColSpan();
					} else {
						break;
					}
				}
			}
			return 0;
		}

		public Element getCurrentCell() {
			return currentCellEl;
		}
	}

	private static class Span {

		public static final Span OneOnOne = new Span(1,1);
		
		private int colspan;
		private int rowspan;
		private boolean done = true;
		
		private Span(int colspan, int rowspan) {
			this.colspan = colspan;
			this.rowspan = rowspan;
		}

		public int getColSpan() {
			return colspan;
		}
		
		public int getRowSpan() {
			return rowspan;
		}
		
		public void decrementRowSpan() {
			rowspan--;
		}
		
		public boolean isDone() {
			return done;
		}
		
		public void unDone() {
			done = false;
		}
	}
}