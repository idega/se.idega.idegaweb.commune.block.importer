package se.idega.idegaweb.commune.block.importer.business;
import java.util.Collection;

/**
 * <p>Title: ImportFileHandler</p>
 * <p>Description: An business interface for handling of classes implementing the ImportFile interface</p>
 * <p>Copyright: (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is">Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */

public interface ImportFileHandler {

public boolean handleRecords(Collection records);




}