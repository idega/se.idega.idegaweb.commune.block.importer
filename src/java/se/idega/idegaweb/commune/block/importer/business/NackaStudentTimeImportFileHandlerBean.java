package se.idega.idegaweb.commune.block.importer.business;

import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.*;
import com.idega.business.IBOServiceBean;
import com.idega.user.data.*;
import java.rmi.RemoteException;
import java.util.*;
import javax.ejb.*;
import javax.transaction.*;
import se.idega.idegaweb.commune.business.CommuneUserBusiness;
import se.idega.util.PIDChecker;

/**
 * Reads records in a format specified in {@link
 * se.idega.idegaweb.commune.block.importer.business.NackaStudentTimeImportFileHandler}.
 * <p>
 * Last modified: $Date: 2003/03/27 11:20:54 $ by $Author: staffan $
 *
 * @author <a href="http://www.staffannoteberg.com">Staffan Nöteberg</a>
 * @version $Revision: 1.1 $
 * @see com.idega.block.importer.data.ImportFile
 * @see com.idega.block.importer.business.ImportFileHandler
 */
public class NackaStudentTimeImportFileHandlerBean extends IBOServiceBean
    implements NackaStudentTimeImportFileHandler {

    final private static int SSN_COL = 0;
    final private static int SCHOOL_COL = 1;
    final private static int HOURS_COL = 2;   

    private ImportFile importFile = null;
    private Group rootGroup = null;
    final private List failedRecords = new ArrayList ();
    

    public void setImportFile (final ImportFile importFile) {
        this.importFile = importFile;
    }
    
    public void setRootGroup (final Group rootGroup) {
        this.rootGroup = rootGroup;
    }
    
    /**
     * Reads records in a format specified in {@link se.idega.idegaweb.commune.block.importer.business.NackaStudentTimeImportFileHandler}.
     * Relies on the fact that {@link #setImportFile} has been invoked correctly
     * prior to invocation of this method.
     */
    public boolean handleRecords () throws RemoteException {
        System.err.println ("¤¤¤ start handling records");
        boolean result = false;
        failedRecords.clear ();
        final SessionContext sessionContext = getSessionContext();
        final UserTransaction transaction = sessionContext.getUserTransaction();
        try {
            transaction.begin();
            final CommuneUserBusiness communeUserBusiness
                    = (CommuneUserBusiness) getServiceInstance
                    (CommuneUserBusiness.class);
            final UserHome userHome = communeUserBusiness.getUserHome ();
            final SchoolBusiness schoolBusiness = (SchoolBusiness)
                    getServiceInstance (SchoolBusiness.class);
            final SchoolHome schoolHome = schoolBusiness.getSchoolHome ();
            final PIDChecker pidChecker = PIDChecker.getInstance ();
            for (String record = (String) importFile.getNextRecord ();
                 record != null && record.trim ().length () > 0;
                 record = (String) importFile.getNextRecord ()) {
                final List fields
                        = importFile.getValuesFromRecordString (record);
                if (fields.size () >= 3 && fields.get (HOURS_COL) != null
                    && Character.isDigit
                    (fields.get (HOURS_COL).toString ().charAt (0))) {
                    // this is an acceptable record

                    // 1. find or create user
                    final String ssn = (String) fields.get (SSN_COL);
                    User user = null;
                    try {
                        user = userHome.findByPersonalID (ssn);
                    } catch (FinderException finderException) {
                        if (pidChecker.isValid (ssn)) {
                            user = communeUserBusiness .createSpecialCitizenByPersonalIDIfDoesNotExist(ssn, "", "", ssn);
                        }
                        System.err.println (getClass ().getName () + ": created"
                                            + " new special citizen with ssn "
                                            + ssn);
                    }

                    // 2. find school
                    final String schoolName = (String) fields.get (SCHOOL_COL);
                    try {
                        final School school
                                = schoolHome.findBySchoolName (schoolName);
                        final int hours = getIntInBeginningOfString
                                ((String) fields.get (HOURS_COL));
                        // TODO: store in DB
                    } catch (FinderException finderException) {
                        System.err.println (getClass ().getName ()
                                            + ": couldn't import record with"
                                            + " unknown school '" + schoolName
                                            + "' record=" + record);
                        failedRecords.add (record + " : "
                                           + finderException.getMessage ());
                    }
                }
            }
            transaction.commit ();
            result = true;
        } catch (final Exception e) {
            // something really unexpected occured - rollback everything
            e.printStackTrace ();
            try {
                transaction.rollback ();
            } catch (final SystemException systemException) {
                systemException.printStackTrace();
            }
        }

        return result;
    }
    
    public List getFailedRecords () {
        return failedRecords;
    }
    
    private static int getIntInBeginningOfString (final String rawString) {
        final StringBuffer digits = new StringBuffer ();
        for (int i = 0; i < rawString.length ()
                     && Character.isDigit (rawString.charAt (i)); i++) {
            digits.append (rawString.charAt (i));
        }
        return Integer.parseInt (digits.toString ());
    }

    /*
	private SchoolSeason getPreviousSeason () {
        final SchoolCommuneBusiness schoolCommuneBusiness = (SchoolCommuneBusiness) getServiceInstance (SchoolCommuneBusiness.class);
        final int currentSeasonId = schoolCommuneBusiness.getCurrentSchoolSeasonID ();
        final int previousSeasonId = schoolCommuneBusiness.getPreviousSchoolSeasonID (currentSeasonId);
        final SchoolSeasonHome schoolSeasonHome = (SchoolSeasonHome) getIDOHome (SchoolSeason.class);
        final SchoolSeason previousSeason = schoolSeasonHome.findByPrimaryKey (previousSeasonId);
        return previousSeason;
	}

    */
}
