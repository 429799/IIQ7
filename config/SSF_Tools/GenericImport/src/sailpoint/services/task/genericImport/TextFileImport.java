package sailpoint.services.task.genericImport;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import sailpoint.object.Attributes;
import sailpoint.tools.GeneralException;
import sailpoint.tools.RFC4180LineIterator;
import sailpoint.tools.RFC4180LineParser;

/**
 * 
 * @author christian.cairney
 * 
 * Text file import for the generic import class.
 * 
 * Attribute:  importFileName = File name of the import file
 *
 */
public class TextFileImport extends AbstractGenericImport implements GenericImport {

	public static final String IMPORT_TEXT_FILE_NAME = "importFileName";
	public static final String IMPORT_TEXT_FILE_DELIMITER = "importFileDelimiter";
	public static final String IMPORT_TEXT_FILE_HAS_HEADER = "importHasHeader";
	public static final String IMPORT_TEXT_FILE_REMARK_TOKEN = "importRemarkToken";
	public static final String IMPORT_TEXT_FILE_HEADER = "importHeader";
	public static final String IMPORT_TEXT_FILE_ENCODING = "importFileEncoding";
	
	private static final String DEFAULT_COLUMN_PREFIX = "column_";
	
	// Text importer configuration
	private boolean hasHeader = false;
	private List<String> header = null;
	private String fileDelimiter = ",";
	private String filename = null;
	private String remarkToken = "#!";
	private String encoding = null;

	//
	private InputStream stream;
	private RFC4180LineIterator lines = null;
	private RFC4180LineParser parser = null;

	private static Logger log = Logger.getLogger(TextFileImport.class);

	public TextFileImport() throws GeneralException {
		super();
		header = new ArrayList<String>();
	}

	@Override
	public void open() throws GeneralException {

		if (getAttributes().containsKey(IMPORT_TEXT_FILE_NAME)) {
			filename = getAttributes().getString(IMPORT_TEXT_FILE_NAME);
		} else {
			throw new GeneralException(
					"Cannot open file stream as no filename exists in key:"
							+ IMPORT_TEXT_FILE_NAME);
		}

		// If there are no values in the attributes map, then allow for the
		// defaults set in the class
		if (getAttributes().containsKey(IMPORT_TEXT_FILE_DELIMITER)) {
			fileDelimiter = getAttributes().getString(
					IMPORT_TEXT_FILE_DELIMITER);
		}

		if (getAttributes().containsKey(IMPORT_TEXT_FILE_HAS_HEADER)) {
			hasHeader = getAttributes().getBoolean(IMPORT_TEXT_FILE_HAS_HEADER);
		}

		if (getAttributes().containsKey(IMPORT_TEXT_FILE_REMARK_TOKEN)) {
			remarkToken = getAttributes().getString(
					IMPORT_TEXT_FILE_REMARK_TOKEN);
		}

		if (getAttributes().containsKey(IMPORT_TEXT_FILE_ENCODING)) {
			encoding = getAttributes().getString(IMPORT_TEXT_FILE_ENCODING);
		}
		
		if (getAttributes().containsKey(IMPORT_TEXT_FILE_HEADER) && hasHeader == false) {
			header = new LinkedList<String>(Arrays.asList(getAttributes().getString(
					IMPORT_TEXT_FILE_HEADER).split(";")));
		}
		// Init the stream
		parser = new RFC4180LineParser(fileDelimiter);
		lines = null;
		stream = null;

		// Get the input stream

		try {
			if (log.isDebugEnabled())
				log.debug("Opening filename: " + filename);
			File file = new File(filename);

			if (encoding != null) {
				stream = new BufferedInputStream(new FileInputStream(file));
			} else {
				stream = new BufferedInputStream(new FileInputStream(file));
			}
			if (log.isDebugEnabled())
				log.debug("Successfully opened the file.");

		} catch (Exception e) {
			throw new GeneralException("Could not open filename '" + filename
					+ "' in TextFileImport.open()", e);
		}

		if (encoding == null) {
			lines = new RFC4180LineIterator(new BufferedReader(
				new InputStreamReader(stream)));
		} else {
			try {
				lines = new RFC4180LineIterator(new BufferedReader(
					new InputStreamReader(stream, encoding)));
			} catch (UnsupportedEncodingException e) {
				throw new GeneralException("COuld not open strem due to illegal encoding setting of " + encoding, e);
			}
		}

		// Do we need to read the header?
		if (hasHeader) {

			// Look for the first usable line as the header
			
		}

	}

	@Override
	public void close() throws GeneralException {
		if (stream != null) {
			if (log.isDebugEnabled())
				log.debug("Closing file stream.");
			try {
				stream.close();
			} catch (IOException e) {
				throw new GeneralException("Could not close the file stream", e);

			}
		} else {
			if (log.isDebugEnabled())
				log.debug("Cannot close file stream as it is null.");
		}
	}

	@Override
	public Iterator<HashMap<String,Object>> iterator() {

		Iterator<HashMap<String,Object>> it = null;
		it = new ImportIterator();


		// Check to see if there is a header here, then grab that
		if (it != null && hasHeader) {
			Attributes<String, Object> headerLine = (Attributes<String, Object>) it
					.next();
			
			for (int c=0; c < headerLine.size(); c++) {
				header.add(headerLine.getString(DEFAULT_COLUMN_PREFIX + String.valueOf(c)));
			}
			if (log.isDebugEnabled()) log.debug("Header: " + header.toString());
		}

		return it;

	}

	private class ImportIterator implements Iterator<HashMap<String,Object>> {

		private int lineNo = 0;
		private boolean hasNext = false;
		private String line = null;

		public ImportIterator()  {

			getNextLine();

		}

		@Override
		public boolean hasNext() {

			return hasNext;
		}

		// get the next line and set the hasNext status
		private void getNextLine() {

			try {
				line = lines.readLine();

				lineNo++;
				if (log.isDebugEnabled())
					log.debug("Read in line number " + String.valueOf(lineNo)
							+ ": " + line);

				if (line != null && line.length() == 0) {

					// If it was a 0 length line, or before the header line then
					// loop and get the next record.

					while (line != null
							&& (line.length() == 0 || line
									.startsWith(remarkToken))) {

						if (log.isDebugEnabled())
							log.debug("Ignored line number: "
									+ String.valueOf(lineNo) + ", " + line);
						line = lines.readLine();
						lineNo++;
					}
				}
			} catch (IOException e) {
				if (log.isDebugEnabled()) log.debug("Got an IO Error, so returning no line");
				line = null;
			}

			if (line == null) {
				if (log.isDebugEnabled())
					log.debug("hasNext = false");
				hasNext = false;
			} else {
				if (log.isDebugEnabled())
					log.debug("hasNext = true");
				hasNext = true;
 			}
		}

		@Override
		public Attributes<String, Object> next() throws NoSuchElementException {

			Attributes<String, Object> values = null;

			if (line != null) {
				if (log.isDebugEnabled())
					log.debug("Read line number " + String.valueOf(lineNo)
							+ ": " + line);

				// Check to make sure we have some content, if we don't then
				// don't
				// process the line.
				ArrayList<String> tokens;
				try {
					tokens = parser.parseLine(line);
				} catch (GeneralException e) {

					// Urgh!
					//
					// I only get a GeneralException here.. no idea what
					// really got thrown... ah well..
					// I have to raise an error here, so it'll be
					// NoSuchElementException, bit
					// of a red herring though... ho hum.
					//
					// TODO: Look at the source and see what the parseLine
					// exception
					// really is.
					//

					throw new NoSuchElementException(
							"Got an error in iterator.next() call when parsing the line into tokens.  General Exception reported: "
									+ e.getMessage());

				}

				// Make sure the number of columns read in is the same as the
				// number
				// of columns in the header

				// Determine how many columns to process, may be the total
				// number of
				// tokens columns in the header, or if the parsed columns is
				// less, use
				// that instead.

				int maxColumns = header.size();
				if (maxColumns == 0)
					maxColumns = tokens.size();
				if (tokens.size() < maxColumns)
					maxColumns = tokens.size();

				// Build the values hashmap, map the column numbers
				// with the names in the column header.

				values = new Attributes<String, Object>();
				for (int c = 0; c < maxColumns; c++) {

					String value = tokens.get(c);

					// Determine the column name, we may not have a header
					// yet...
					String columnName = null;
					if (header.size() > 0) {
						columnName = header.get(c);
					} else {
						columnName = DEFAULT_COLUMN_PREFIX + String.valueOf(c);
					}

					if (log.isDebugEnabled())
						log.debug("Adding to values hashmap, key:" + columnName
								+ ", value: " + value);

					values.put(columnName, value);

				}

			} else {
				throw new NoSuchElementException("Does not have next");
			}

			getNextLine();
			
			return values;
		}

		@Override
		public void remove() {
			// We don't support removes, no point on a read only CSV file?
			throw new UnsupportedOperationException();
		}

	}

}
