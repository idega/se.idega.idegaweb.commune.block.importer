package se.idega.idegaweb.commune.block.importer.business;

import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.*;
import com.idega.business.IBOServiceBean;
import com.idega.data.IDOLookup;
import com.idega.user.data.*;
import java.rmi.RemoteException;
import java.util.*;
import javax.ejb.*;
import javax.transaction.*;
import se.idega.idegaweb.commune.business.CommuneUserBusiness;
import se.idega.idegaweb.commune.school.business.SchoolCommuneBusiness;
import se.idega.idegaweb.commune.school.data.*;
import se.idega.util.PIDChecker;

/**
 * Reads records in a format specified in {@link
 * se.idega.idegaweb.commune.block.importer.business.NackaStudentTimeImportFileHandler}.
 * <p>
 * Last modified: $Date: 2004/02/20 16:36:50 $ by $Author: tryggvil $
 *
 * @author <a href="http://www.staffannoteberg.com">Staffan Nöteberg</a>
 * @version $Revision: 1.8 $
 * @see com.idega.block.importer.data.ImportFile
 * @see com.idega.block.importer.business.ImportFileHandler
 * @see com.idega.block.school.data.SchoolTime
 */
public class NackaStudentTimeImportFileHandlerBean extends IBOServiceBean
    implements NackaStudentTimeImportFileHandler {

    final private static int SSN_COL = 0;
    final private static int SCHOOL_COL = 1;
    final private static int HOURS_COL = 2;   

    private ImportFile importFile = null;
    public Group rootGroup = null;
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
    public boolean handleRecords () {
        log ("Importing records: " + getClass ().getName ());
        boolean readSuccess = true;
        failedRecords.clear ();
        final SessionContext sessionContext = getSessionContext();
        final UserTransaction transaction = sessionContext.getUserTransaction();
        final Set unknownSchools = new HashSet ();
        try {
            transaction.begin();
            // get all home and business objects needed in traversal below
            final CommuneUserBusiness communeUserBusiness
                    = (CommuneUserBusiness) getServiceInstance
                    (CommuneUserBusiness.class);
            final UserHome userHome = communeUserBusiness.getUserHome ();
            final SchoolBusiness schoolBusiness = (SchoolBusiness)
                    getServiceInstance (SchoolBusiness.class);
            final SchoolHome schoolHome = schoolBusiness.getSchoolHome ();
            final PIDChecker pidChecker = PIDChecker.getInstance ();
            final SchoolTimeHome schoolTimeHome
                    = (SchoolTimeHome) IDOLookup.getHome (SchoolTime.class);
            final SchoolSeason schoolSeason = getPreviousSeason ();
            final Date now = new Date ();
            // traverse all records
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
                    final String ssn
                            = getDigitString ((String) fields.get (SSN_COL));
                    User user = null;
                    try {
                        user = userHome.findByPersonalID (ssn);
                    } catch (FinderException finderException) {
                        if (ssn.length () == 12 && pidChecker.isValid (ssn)) {
                            user = communeUserBusiness .createSpecialCitizenByPersonalIDIfDoesNotExist(ssn, "", "", ssn);
                        }
                        logWarning
                                ("NackaStudentTimeImportFileHandlerget: created"
                                 + " new special citizen with ssn " + ssn);
                    }

                    // 2. find school
                    final String schoolName = (String) fields.get (SCHOOL_COL);
                    School school = findBySchool (schoolName, schoolHome);
                    if (school == null) {
                        final String aliasedName
                                = getIWApplicationContext().getIWMainApplication()
                                .getBundle("se.idega.idegaweb.commune")
                                .getProperty (schoolName + "_alias");
                        if (aliasedName != null) {
                            school = findBySchool (aliasedName, schoolHome);
                        }
                    }
                    if (school == null) {
                        logWarning (record + " - Unknown school");
                        failedRecords.add (record + " - Unknown school");
                        unknownSchools.add (schoolName);
                        readSuccess = false;
                    }

                    // 3. get number of hours spent per week
                    final int hours = getIntInBeginningOfString
                            ((String) fields.get (HOURS_COL));

                    // 4. store in db
                    if (user != null) {
                        final SchoolTime schoolTime = schoolTimeHome.create ();
                        schoolTime.setUser (user);
                        if (school != null) { schoolTime.setSchool (school); }
                        schoolTime.setHours (hours);
                        schoolTime.setSeason (schoolSeason);
                        schoolTime.setRegistrationTime (now);
                        schoolTime.store ();
                    }
                }
            }
        } catch (final Exception e) {
            // something really unexpected occured - rollback everything
            e.printStackTrace ();
            readSuccess = false;
        } finally {
            try {
                if (readSuccess) {
                    transaction.commit ();
                } else {
                    logWarning (getClass ().getName ()
                                        + " import failed:");
                    for (Iterator i = failedRecords.iterator ();
                         i.hasNext ();) {
                        final String message = (String) i.next ();
                        logWarning ("> " + message);
                    }

                    logWarning ("Unknown Schools:");
                    for (Iterator i = unknownSchools.iterator ();
                         i.hasNext ();) {
                        final String message = (String) i.next ();
                        logWarning ("> " + message);
                    }

                    transaction.rollback ();
                }
            } catch (Exception e) {
                e.printStackTrace ();
                readSuccess = false;
            }
        }

        return readSuccess;
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

    private static String getDigitString (final String rawString) {
        final StringBuffer digits = new StringBuffer ();
        for (int i = 0; i < rawString.length (); i++) {
            if (Character.isDigit (rawString.charAt (i))) {
                digits.append (rawString.charAt (i));
            }
        }
        return digits.toString ();
    }

    private School findBySchool (final String schoolName,
                                 final SchoolHome schoolHome) {
        School result = null;
        try {
            result = schoolHome.findBySchoolName (schoolName);
        } catch (FinderException finderException) {
            // nothing, just return null
        }
        return result;
    }

	private SchoolSeason getPreviousSeason () throws RemoteException,
                                                     FinderException {
        final SchoolCommuneBusiness schoolCommuneBusiness
                = (SchoolCommuneBusiness) getServiceInstance
                (SchoolCommuneBusiness.class);
        final int currentSeasonId
                = schoolCommuneBusiness.getCurrentSchoolSeasonID ();
        final int previousSeasonId
                = schoolCommuneBusiness.getPreviousSchoolSeasonID
                (currentSeasonId);
        final SchoolSeasonHome schoolSeasonHome
                = (SchoolSeasonHome) getIDOHome (SchoolSeason.class);
        final SchoolSeason previousSeason = schoolSeasonHome.findByPrimaryKey
                (new Integer (previousSeasonId));
        return previousSeason;
	}
}
