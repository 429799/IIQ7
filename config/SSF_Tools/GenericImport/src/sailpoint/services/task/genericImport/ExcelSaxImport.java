package sailpoint.services.task.genericImport;

/* ====================================================================
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 ==================================================================== */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import sailpoint.tools.GeneralException;

/**
 * A Generic Import iterator for a MS Excel XL WorkSheet.
 * 
 * Modelled on the POI sample program XLS2CSVmra by Nick Burch and Excel to CSV
 * converter by Chris Lott.
 * 
 * Uses the SAX model to avoid excessive memory usage when importing large
 * spreadsheet.
 * 
 * @author Christian Cairney
 */
public class ExcelSaxImport extends AbstractGenericImport implements GenericImport {

	/**
	 * The type of the data value is indicated by an attribute on the cell. The
	 * value is usually in a "v" element within the cell.
	 */
	enum xssfDataType {
		BOOL, ERROR, FORMULA, INLINESTR, SSTINDEX, NUMBER,
	}

	private static Logger log = Logger.getLogger(ExcelSaxImport.class);

	private List<sailpoint.object.Attributes<String,Object>> rows = new ArrayList<sailpoint.object.Attributes<String,Object>>();
	private sailpoint.object.Attributes<String, Object> header = new sailpoint.object.Attributes<String,Object>();
	private String sheetName;
	private File xlFile;
	private int headerRow;
	private boolean hasHeader;

	public static final String IMPORT_EXCEL_FILENAME = "excelFilename";
	public static final String IMPORT_EXCEL_HAS_HEADER = "excelHasHeader";
	public static final String IMPORT_EXCEL_HEADER_ROW = "excelHeaderRow";
	public static final String IMPORT_EXCEL_HEADER = "excelHeader";
	public static final String IMPORT_EXCEL_SHEET_NAME = "excelSheetName";

	private int minColumns = -1;

	/**
     * 
     */
	public ExcelSaxImport() throws GeneralException {
	}

	/**
	 * Parses and shows the content of one sheet using the specified styles and
	 * shared-strings tables.
	 *
	 * @param styles
	 * @param strings
	 * @param sheetInputStream
	 */
	public void processSheet(StylesTable styles, ReadOnlySharedStringsTable strings,
			InputStream sheetInputStream) throws IOException, ParserConfigurationException, SAXException {

		InputSource sheetSource = new InputSource(sheetInputStream);
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		SAXParser saxParser = saxFactory.newSAXParser();
		XMLReader sheetParser = saxParser.getXMLReader();
		ContentHandler handler = new WorkSheetHandler(styles, strings, this.minColumns);
		sheetParser.setContentHandler(handler);
		sheetParser.parse(sheetSource);
	}

	/**
	 * Initiates the processing of the XLS workbook file to CSV.
	 *
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */

	public void process() throws IOException, OpenXML4JException, ParserConfigurationException, SAXException {

		OPCPackage xlsxPackage = OPCPackage.open(xlFile.getPath(), PackageAccess.READ);

		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(xlsxPackage);
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		int index = 0;
		while (iter.hasNext()) {
			InputStream stream = iter.next();
			if (sheetName.equals(iter.getSheetName()) || sheetName == null) {
				if (log.isDebugEnabled())
					log.debug("Sheet name: " + sheetName + " (" + index + ")");
				processSheet(styles, strings, stream);
			}
			stream.close();
			++index;
		}
		xlsxPackage.close();

	}

	@Override
	public void open() throws GeneralException {

		// Get the filename
		if (getAttributes().containsKey(IMPORT_EXCEL_FILENAME)) {
			xlFile = new File(getAttributes().getString(IMPORT_EXCEL_FILENAME));
			if (!xlFile.exists()) {
				throw new GeneralException("Not found or not a file: " + xlFile.getPath());
			}
		} else {
			throw new GeneralException("No filename has been specified for the Excel importer.");
		}

		// get the sheet name
		if (getAttributes().containsKey(IMPORT_EXCEL_SHEET_NAME)) {
			sheetName = getAttributes().getString(IMPORT_EXCEL_SHEET_NAME);
		} else {
			throw new GeneralException("No Excel Sheet has been specified.");
		}

		// Get the header details
		if (getAttributes().containsKey(IMPORT_EXCEL_HAS_HEADER)) {
			hasHeader = getAttributes().getBoolean(IMPORT_EXCEL_HAS_HEADER);
			if (hasHeader) {
				if (getAttributes().containsKey(IMPORT_EXCEL_HEADER_ROW)) {
					headerRow = getAttributes().getInt(IMPORT_EXCEL_HEADER_ROW);
				} else {
					throw new GeneralException("Header has been specified but no header row indicated");
				}
			} else {
				// Get the manual header list
				if (getAttributes().containsKey(IMPORT_EXCEL_HEADER)) {
					String[] headerStringValues = getAttributes().getString(IMPORT_EXCEL_HEADER).split(
							AbstractGenericImport.STRING_DELIMITER);
					for (int i = 0; i < headerStringValues.length; i++) {
						header.put(String.valueOf(i), headerStringValues[i]);
					}

				} else {
					throw new GeneralException(
							"Configuration specifies no header in sheet and no manual configuration of header in this config.");
				}
			}
		}

		try {
			process();
		} catch (IOException e) {
			throw new GeneralException("IOException in process()", e);
		} catch (OpenXML4JException e) {
			throw new GeneralException("OpenXML4JException in process()", e);
		} catch (ParserConfigurationException e) {
			throw new GeneralException("ParserConfigurationException in process()", e);
		} catch (SAXException e) {
			throw new GeneralException("SAXException in process()", e);
		}

		
		if (header != null) {
			if (log.isDebugEnabled()) log.debug("Header: " + header.toString());
		} else {
			log.debug("No header returned.");
		}
		

	}

	@Override
	public void close() throws GeneralException {
		// Nothing really to do here... so ... whatever...!

	}

	@Override
	public Iterator iterator() {

		Iterator it = null;
		it = new ImportIterator();

		return it;

	}

	private class ImportIterator implements Iterator {

		private int recordNo = 0;

		/**
		 * Constructor for this iterator
		 */
		public ImportIterator() {

			if (log.isDebugEnabled()) 
				log.debug("Instantiating the ImportIterator.  Row size: " + String.valueOf(rows.size()) + 
						", current record numnber: " + String.valueOf(recordNo));
			
		}

		/**
		 * hasNext
		 * 
		 */
		@Override
		public boolean hasNext() {
			return (recordNo < rows.size());
		}

		@Override
		/**
		 * 
		 */
		public sailpoint.object.Attributes<String, Object> next() throws NoSuchElementException {

			log.debug("Entering next()");
			
			sailpoint.object.Attributes<String,Object> row = rows.get(recordNo);
			
			if (log.isDebugEnabled()) log.debug("Row information: " + row.toString());
			
			
			if (header != null &&  header.size() > 0) {
				
				for (int i=0; i < header.size(); i++) {
					
					String key = String.valueOf(i);
					

					if (row.containsKey(key)) {
						if (log.isDebugEnabled()) log.debug("Transforming column: " + key + " to " + (String) header.get(key));
						row.put((String) header.get(key), row.get(key));
						row.remove(key);
					}
				}
			}
			
			recordNo++;
			if (log.isDebugEnabled()) log.debug("Exiting next() with " + row.toString());
			
			return row;
		}

		@Override
		public void remove() {
			// We don't support removes, because I don't want to.
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Derived from http://poi.apache.org/spreadsheet/how-to.html#xssf_sax_api
	 * <p/>
	 * Also see Standard ECMA-376, 1st edition, part 4, pages 1928ff, at
	 * http://www.ecma-international.org/publications/standards/Ecma-376.htm
	 * <p/>
	 * A web-friendly version is http://openiso.org/Ecma/376/Part4
	 */
	class WorkSheetHandler extends DefaultHandler {

		/**
		 * Table with styles
		 */
		private StylesTable stylesTable;

		/**
		 * Table with unique strings
		 */
		private ReadOnlySharedStringsTable sharedStringsTable;

		/**
		 * Number of columns to read starting with leftmost
		 */
		private final int minColumnCount;

		// Set when V start element is seen
		private boolean vIsOpen;

		// Set when cell start element is seen;
		// used when cell close element is seen.
		private xssfDataType nextDataType;

		// Used to format numeric cell values.
		private short formatIndex;
		private String formatString;
		private final DataFormatter formatter;

		private int thisColumn = -1;
		// The last column printed to the output stream
		private int lastColumnNumber = -1;

		private sailpoint.object.Attributes<String, Object> rowData;
		private int rowNumber = 0;

		// Gathers characters as they are seen.
		private StringBuffer value;

		/**
		 * Accepts objects needed while parsing.
		 *
		 * @param styles
		 *            Table of styles
		 * @param strings
		 *            Table of shared strings
		 * @param cols
		 *            Minimum number of columns to show
		 */
		public WorkSheetHandler(StylesTable styles, ReadOnlySharedStringsTable strings, int cols) {

			this.stylesTable = styles;
			this.sharedStringsTable = strings;
			this.minColumnCount = cols;
			this.value = new StringBuffer();
			this.nextDataType = xssfDataType.NUMBER;
			this.formatter = new DataFormatter();
			this.rowData = new sailpoint.object.Attributes();

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String uri, String localName, String name, Attributes attributes)
				throws SAXException {

			if ("inlineStr".equals(name) || "v".equals(name)) {
				vIsOpen = true;
				// Clear contents cache
				value.setLength(0);
			}
			// c => cell
			else if ("c".equals(name)) {
				// Get the cell reference
				String r = attributes.getValue("r");
				int firstDigit = -1;
				for (int c = 0; c < r.length(); ++c) {
					if (Character.isDigit(r.charAt(c))) {
						firstDigit = c;
						break;
					}
				}
				thisColumn = nameToColumn(r.substring(0, firstDigit));

				// Set up defaults.
				this.nextDataType = xssfDataType.NUMBER;
				this.formatIndex = -1;
				this.formatString = null;

				String cellType = attributes.getValue("t");
				String cellStyleStr = attributes.getValue("s");

				if ("b".equals(cellType))
					nextDataType = xssfDataType.BOOL;
				else if ("e".equals(cellType))
					nextDataType = xssfDataType.ERROR;
				else if ("inlineStr".equals(cellType))
					nextDataType = xssfDataType.INLINESTR;
				else if ("s".equals(cellType))
					nextDataType = xssfDataType.SSTINDEX;
				else if ("str".equals(cellType))
					nextDataType = xssfDataType.FORMULA;
				else if (cellStyleStr != null) {
					// It's a number, but almost certainly one
					// with a special style or format
					int styleIndex = Integer.parseInt(cellStyleStr);
					XSSFCellStyle style = stylesTable.getStyleAt(styleIndex);
					this.formatIndex = style.getDataFormat();
					this.formatString = style.getDataFormatString();
					if (this.formatString == null)
						this.formatString = BuiltinFormats.getBuiltinFormat(this.formatIndex);
				}
			}

		}

		/**
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		public void endElement(String uri, String localName, String name) throws SAXException {

			String thisStr = null;
			// v => contents of a cell
			if ("v".equals(name)) {
				// Process the value contents as required.
				// Do now, as characters() may be called more than once
				switch (nextDataType) {

				case BOOL:
					char first = value.charAt(0);
					// thisStr = first == '0' ? "FALSE" : "TRUE";
					Boolean arg = first == '0' ? false : true;
					rowData.put(String.valueOf(this.thisColumn), arg);
					break;

				case ERROR:
					thisStr = "\"ERROR:" + value.toString() + '"';
					rowData.put(String.valueOf(this.thisColumn), thisStr);
					break;
				case FORMULA:
					// A formula could result in a string value,
					// so always add double-quote characters.
					thisStr =  value.toString() ;
					rowData.put(String.valueOf(this.thisColumn), thisStr);
					break;

				case INLINESTR:
					// TODO: have seen an example of this, so it's untested.
					XSSFRichTextString rtsi = new XSSFRichTextString(value.toString());
					thisStr = rtsi.toString() ;
					rowData.put(String.valueOf(this.thisColumn), thisStr);
					break;

				case SSTINDEX:
					String sstIndex = value.toString();
					try {
						int idx = Integer.parseInt(sstIndex);
						XSSFRichTextString rtss = new XSSFRichTextString(sharedStringsTable.getEntryAt(idx));
						thisStr = rtss.toString() ;
						rowData.put(String.valueOf(this.thisColumn), thisStr);
					} catch (NumberFormatException ex) {
						throw new SAXException("endElement: Failed to parse SST index '" + sstIndex + "': "
								+ ex.toString());
					}
					break;

				case NUMBER:
					String n = value.toString();
					if (this.formatString != null) {
						thisStr = formatter.formatRawCellContents(Double.parseDouble(n), this.formatIndex,
								this.formatString);
						rowData.put(String.valueOf(this.thisColumn), thisStr);
					} else {
						thisStr = n;
						rowData.put(String.valueOf(this.thisColumn), n);
					}
					break;

				default:
					throw new SAXException("endElement: Could not handle data type " + nextDataType
							+ ", handler needed!");
				}

				if (lastColumnNumber == -1) {
					lastColumnNumber = 0;
				}

				// Update column
				if (thisColumn > -1)
					lastColumnNumber = thisColumn;

			} else if ("row".equals(name)) {

				rowNumber++;
				if (rowNumber > headerRow) {
					rows.add(rowData);
				} else {
					header = rowData;
				}

				rowData = new sailpoint.object.Attributes();
				lastColumnNumber = -1;

			}

		}

		/**
		 * Captures characters only if a suitable element is open. Originally
		 * was just "v"; extended for inlineStr also.
		 */
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (vIsOpen)
				value.append(ch, start, length);
		}

		/**
		 * Converts an Excel column name like "C" to a zero-based index.
		 *
		 * @param name
		 * @return Index corresponding to the specified name
		 */
		private int nameToColumn(String name) {
			int column = -1;
			for (int i = 0; i < name.length(); ++i) {
				int c = name.charAt(i);
				column = (column + 1) * 26 + c - 'A';
			}
			return column;
		}

	}

}
