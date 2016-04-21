package sailpoint.services.task.genericImport;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Attributes;
import sailpoint.tools.GeneralException;
import sailpoint.tools.JdbcUtil;
/**
 * JdbcImport used the JdbcUtil.ARGS_ constants for the JDBC connection
 * parameters.
 * 
 * @author christian.cairney
 * @version 1.0
 * @since 0.1
 */
public class JdbcImport extends AbstractGenericImport implements GenericImport {
	
	public static final String IMPORT_JDBC_SQL = "sqlQuery";
	public static final String IMPORT_JDBC_GROUP_BY = "resultSetGroupBy";
	
	private Connection con = null;
	
	// JDBC importer configuration
	private ArrayList<String> header = null;
	private ResultSet rs = null;
	
	// Our friend, the logger
	private static Logger log = Logger.getLogger(JdbcImport.class);

	/**
	 * Constructor
	 * 
	 * @throws GeneralException
	 */
	public JdbcImport() throws GeneralException {
		super();
		header = new ArrayList<String>();
	}

	/**
	 * Open method for JDBC
	 */
	public void open() throws GeneralException {

		log.debug("Starting JdbcImport.open()");
		
		SailPointContext context = SailPointFactory.getCurrentContext();
		
		Attributes<String,Object> attributes = getAttributes();
		if (attributes.containsKey(JdbcUtil.ARG_PASSWORD)) 
			attributes.put(JdbcUtil.ARG_PASSWORD,  
					context.decrypt((String)attributes.get(JdbcUtil.ARG_PASSWORD)));
		
		con = JdbcUtil.getConnection(getAttributes());
		Statement stmt;
		try {
			stmt = con.createStatement();
			
			String query = getAttributes().get(IMPORT_JDBC_SQL).toString();
			
			if (log.isDebugEnabled()) log.debug("  Issueing QUERY: " + query);
			
			rs = stmt.executeQuery(query);
			
		} catch (SQLException e) {
			throw new GeneralException("Exception when opening new JDBC connection in JdbcImport", e);
		}
		
		// Get the header
		ResultSetMetaData rsmd;
		try {
			rsmd = rs.getMetaData(); 
			for (int i=1; i <= rsmd.getColumnCount(); i++) {
				
				String columnName = rsmd.getColumnName(i);
				String columnLabel = rsmd.getColumnLabel(i);
				if (columnLabel != null) {
					header.add((String) columnLabel);
				} else {
					header.add((String) columnName);
				}
			}
			
			if (log.isDebugEnabled()) {
				log.debug("  Column headers returned are:");
				for (String column : header) {
					log.debug("    " + column);
				}
			}
		} catch (SQLException e) {
			throw new GeneralException("Could not get JDBC Meta Data in JdbcImport", e);
		}
		
		log.debug("Exiting JdbcImport.open()");
	}

	@Override
	public void close() throws GeneralException {
		try {
			if (rs != null)	rs.close();
			if (con != null) con.close();
		} catch (SQLException e) {
			throw new GeneralException("Could not close database in JdbcImport", e);
		}
		
	}

	@Override
	public Iterator<HashMap<String,Object>> iterator() {

		Iterator<HashMap<String,Object>> it = null;
		it = new ImportIterator();		

		return it;

	}

	private class ImportIterator implements Iterator<HashMap<String,Object>> {

		private int recordNo = 0;
		private boolean hasNext = false;

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
				if (rs.next()) {
					hasNext = true;
					recordNo++;
					if (log.isDebugEnabled())
						log.debug("Read in record number " + String.valueOf(recordNo));

				} else {
					hasNext = false;
				}
			} catch (SQLException e) {
				log.error("Cannot getNextLine in JdbcImport iterator. Returning fale for hasNext.", e);
				hasNext = false;
			}
			
		}

		@Override
		public Attributes<String, Object> next() throws NoSuchElementException {

			Attributes<String, Object> values = null;

			if (hasNext) {
				if (log.isDebugEnabled())
					log.debug("Read record number " + String.valueOf(recordNo));

				// Make sure the number of columns read in is the same as the
				// number
				// of columns in the header

				// Determine how many columns to process, may be the total
				// number of
				// tokens columns in the header, or if the parsed columns is
				// less, use
				// that instead.

				//int maxColumns = header.size();

				// Build the values hashmap, map the column numbers
				// with the names in the column header.

				values = new Attributes<String, Object>();
				for (String columnName : header) {

					// The value is of type object, not fixed to
					// string as in the text delimited import
					Object value = null;
					try {
						value = rs.getObject(columnName);
					} catch (SQLException e) {
						// TODO: Ok, so we're going to ignore this exception for now, may work around
						// this later and throw the exception back to the callee as a general 
						// exception.
						log.error("next() returned an error when retrieving column " + columnName, e);
					} 

					if (log.isDebugEnabled()) {
						if (value != null) {
							log.debug("Adding to values hashmap, key:" + columnName
								+ ", value: " + value.toString());
						} else {
							log.debug("Value for hashmap, key:" + columnName
									+ " is null");	
						}
					}
					
					if (value != null) values.put(columnName, value);

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
