package se.idega.idegaweb.commune.block.importer.data;

import java.io.File;

import com.idega.block.importer.data.GenericImportFile;
import com.idega.block.importer.data.ImportFile;

/**
 * 
 * @author bluebottle
 *
 */
public class SKVImportFile extends GenericImportFile implements ImportFile {

	public SKVImportFile() {
		super();
		setRecordDilimiter("#POST_SLUT");
		setAddNewLineAfterRecord(true);
	}

	public SKVImportFile(File file) {
		this();
		setFile(file);
	}
}