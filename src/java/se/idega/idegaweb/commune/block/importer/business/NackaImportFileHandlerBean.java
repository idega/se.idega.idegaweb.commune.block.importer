/*
 * $Id: NackaImportFileHandlerBean.java,v 1.96 2006/04/09 12:05:08 laddi Exp $
 * 
 * Copyright (C) 2002 Idega Software hf. All Rights Reserved.
 * 
 * This software is the proprietary information of Idega hf. Use is subject to
 * license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

import is.idega.block.family.business.FamilyLogic;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.idega.idegaweb.commune.business.CommuneUserBusiness;
import com.idega.block.importer.data.ImportFile;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.business.IBOServiceBean;
import com.idega.core.location.business.AddressBusiness;
import com.idega.core.location.data.Address;
import com.idega.core.location.data.AddressCoordinate;
import com.idega.core.location.data.AddressCoordinateHome;
import com.idega.core.location.data.AddressHome;
import com.idega.core.location.data.AddressType;
import com.idega.core.location.data.Commune;
import com.idega.core.location.data.CommuneHome;
import com.idega.core.location.data.Country;
import com.idega.core.location.data.CountryHome;
import com.idega.core.location.data.PostalCode;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.presentation.IWContext;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.user.data.UserHome;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;
import com.idega.util.datastructures.HashtableMultivalued;
import com.idega.util.text.TextSoap;

/**
 * NackaImportFileHandlerBean
 * 
 * Last modified: $Date: 2006/04/09 12:05:08 $ by $Author: laddi $
 * 
 * @author <a href="mailto:eiki@idega.com">eiki </a>
 * @version $Revision: 1.96 $
 */
public class NackaImportFileHandlerBean extends IBOServiceBean implements NackaImportFileHandler {

	private String HOME_COMMUNE_CODE;

	private AddressBusiness addressBiz;

	private Commune homeCommune;

	private CommuneHome communeHome;

	private CommuneUserBusiness comUserBiz;

	private Collection TFlist = null;

	private Map deceasedMap;

	private Map relationsMap;

	private Map userPropertiesMap;

	private Map coordinateMap = null;

	private HashMap unhandledActions;

	private ArrayList failedRecords;

	private Gender female;

	private Gender male;

	private User performer = null;

	private UserHome home;

	private FamilyLogic relationBiz;

	private UserTransaction transaction2;

	private ImportFile file;

	private int startRecord = 0;

	private boolean fix = false;

	private boolean importAddresses = true;

	private boolean importRelations = true;

	private boolean importUsers = true;

	private boolean secretPerson = false;

	public NackaImportFileHandlerBean() {
	}

	// This method is syncronized because this implementation
	// is not reentrant (does not allow simultaneous invocation by many callers)
	public synchronized boolean handleRecords() {
		this.transaction2 = this.getSessionContext().getUserTransaction();
		try {
			this.performer = IWContext.getInstance().getCurrentUser();
		}
		catch (Exception ex) {
			this.performer = null;
		}
		Timer clock = new Timer();
		clock.start();
		try {
			// Initialize the default/home commune
			this.homeCommune = getCommuneHome().findDefaultCommune();
			this.HOME_COMMUNE_CODE = this.homeCommune.getCommuneCode();
			// initialize business beans and data homes
			this.comUserBiz = (CommuneUserBusiness) getServiceInstance(CommuneUserBusiness.class);
			this.relationBiz = (FamilyLogic) getServiceInstance(FamilyLogic.class);
			this.home = this.comUserBiz.getUserHome();
			this.addressBiz = (AddressBusiness) getServiceInstance(AddressBusiness.class);
			this.failedRecords = new ArrayList();
			this.relationsMap = new HashMap();
			this.deceasedMap = new HashMap();
			this.unhandledActions = new HashMap();
			this.coordinateMap = new HashMap();
			this.TFlist = new Vector();
			Collection row1 = new Vector();
			row1.add("Personal ID changes");
			this.TFlist.add(row1);
			/*String fixer = getIWApplicationContext().getApplicationSettings().getProperty(FIX_PARAMETER_NAME);
			log("NackaImportFileHandler [STARTING] time: " + IWTimestamp.getTimestampRightNow().toString());
			if ("TRUE".equals(fixer)) {
				log("NackaImportFileHandler [WARNING] FIX (run_fix) is set to TRUE");
				fix = true;
			}*/

			// iterate through the records and process them
			String item;
			int count = 0;
			while (!(item = (String) this.file.getNextRecord()).equals("")) {
				count++;
				if (count > this.startRecord) {
					if (!processRecord(item)) {
						this.failedRecords.add(item);
					}
				}
				if ((count % 250) == 0) {
					log("NackaImportFileHandler processing RECORD [" + count + "] time: "
							+ IWTimestamp.getTimestampRightNow().toString());
				}
				item = null;
			}
			clock.stop();
			log("Time to handleRecords: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000))
					+ " s. Not done yet starting with relation");

			if (this.importRelations) {
				storeRelations();
			}

			Set errorKeys = this.unhandledActions.keySet();
			if (errorKeys == null || errorKeys.isEmpty()) {
				log("NackaImportFileHandler : NO UNHANDLED ACTIONS :D");
			}
			else {
				Iterator iter = errorKeys.iterator();
				StringBuffer logString = new StringBuffer(
						"NackaImportFileHandler : \nUnhandled actions during import : ");
				String key;
				int value;
				int totalValue = 0;
				while (iter.hasNext()) {
					key = (String) iter.next();
					value = ((Integer) this.unhandledActions.get(key)).intValue();
					totalValue += value;
					logString.append("\n  " + key + "              : " + value);
				}
				logString.append("\nTotal unhandled actions : " + totalValue
						+ " (were handled with the default action)");
				log(logString.toString());
			}
			if (this.TFlist.size() > 1) {
				// Header is line 1... if nothing else... then nothing to report
				getImportBusiness().addExcelReport(this.file.getFile(), "report", this.TFlist, "\n");
			}
			return true;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	private boolean processRecord(String record) throws RemoteException {
		this.userPropertiesMap = new HashMap();
		record = TextSoap.findAndCut(record, "#UP ");
		if (this.importUsers || this.importAddresses) {
			// Family relations
			this.userPropertiesMap.put(ImportFileFieldConstants.RELATIONAL_SECTION_STARTS,
					getArrayListWithMapsFromStringFragment(record, ImportFileFieldConstants.RELATIONAL_SECTION_STARTS,
							ImportFileFieldConstants.RELATIONAL_SECTION_ENDS));
			// Special case relations
			this.userPropertiesMap.put(ImportFileFieldConstants.SPECIALCASE_RELATIONAL_SECTION_STARTS,
					getArrayListWithMapsFromStringFragment(record,
							ImportFileFieldConstants.SPECIALCASE_RELATIONAL_SECTION_STARTS,
							ImportFileFieldConstants.SPECIALCASE_RELATIONAL_SECTION_ENDS));
			// Citizen info
			this.userPropertiesMap.put(ImportFileFieldConstants.CITIZEN_INFO_SECTION_STARTS,
					getArrayListWithMapsFromStringFragment(record,
							ImportFileFieldConstants.CITIZEN_INFO_SECTION_STARTS,
							ImportFileFieldConstants.CITIZEN_INFO_SECTION_ENDS));
			// Historic info
			this.userPropertiesMap.put(ImportFileFieldConstants.HISTORIC_SECTION_STARTS,
					getArrayListWithMapsFromStringFragment(record, ImportFileFieldConstants.HISTORIC_SECTION_STARTS,
							ImportFileFieldConstants.HISTORIC_SECTION_ENDS));
			// the rest e.g. User info and immigration stuff
			Map tmp = getPropertiesMapFromString(record, " ");
			if (tmp == null || tmp.isEmpty()) {
				return false;
			}
			this.userPropertiesMap.putAll(tmp);
			// log("storeUserInfo");
			storeUserInfo();
		}
		else if (!this.importUsers && !this.importAddresses && this.importRelations) { // only
			// store
			// relations
			// the rest e.g. User info and immigration stuff
			this.userPropertiesMap.putAll(getPropertiesMapFromString(record, " ")); // PIN
			// number
			// etc.
			// Family relations
			this.userPropertiesMap.put(ImportFileFieldConstants.RELATIONAL_SECTION_STARTS,
					getArrayListWithMapsFromStringFragment(record, ImportFileFieldConstants.RELATIONAL_SECTION_STARTS,
							ImportFileFieldConstants.RELATIONAL_SECTION_ENDS));
		}
		/**
		 * family and other releation stuff
		 */
		if (this.importRelations && !this.secretPerson) {
			addRelations();
		}
		this.userPropertiesMap = null;
		record = null;
		return true;
	}

	protected boolean storeUserInfo() throws RemoteException {
		String actionType = getUserProperty(ImportFileFieldConstants.ACTION_TYPE_COLUMN);
		String[][] actions = new String[0][0];
		
		if (actionType != null) {
			actions = parseAction(actionType);
		}

		User user = null;
		String PIN = getUserProperty(ImportFileFieldConstants.PIN_COLUMN);
		String refPIN = getUserProperty(ImportFileFieldConstants.REFERENCE_PIN_COLUMN);
		if (PIN == null) {
			return false;
		}
		boolean secretPerson = isSecretPerson();
		try {
			user = this.comUserBiz.getUser(PIN);
		}
		catch (Exception e) {
			logError("NackaImportFileHandler :  user not found (" + PIN + ")");
		}
		String action;
		String concerns;
		String prefix;
		if (user == null) {
			if (refPIN != null) { // could be a pid change
				Collection coll = new Vector();
				coll.add(PIN + "\t(new PID)");
				coll.add("");
				coll.add("");
				coll.add(refPIN + "\t(referenced PID)");
				coll.add("Actions = " + actionType);
				this.TFlist.add(coll);
				return true;
			}
			else {
				/** FULL IMPORT */
				System.out.println("FullImport, user = " + user);
				return fullImport();
			}
		}
		else if (secretPerson) {
			/** SECRET */
			// System.out.println("Secret person, actionType = "+actionType);
			handleSecretPerson(user);
		}
		
		if (actions.length == 0) {
			/** FULL IMPORT */
			if (!fullImport()) {
				return false;
			}
		}
		else {
			if (user != null) {
				if (refPIN != null) {
					Address address = this.comUserBiz.getUsersMainAddress(user);
					Collection coll = new Vector();
					coll.add(PIN + "\t(new PID)");
					coll.add(user.getName());
					if (address != null) {
						coll.add(address.getStreetAddress());
					}
					else {
						coll.add("");
					}
					coll.add(refPIN + "\t(referenced PID)");
					coll.add("Actions = " + actionType);
					this.TFlist.add(coll);
					return true;
				}
			}
			for (int i = 0; i < actions.length; i++) {
				action = actions[i][0];
				concerns = actions[i][1];
				prefix = actions[i][2];
				if (action == null) {
					System.out.println("... action = null, set to \"\"");
					action = "";
				}
				// System.out.println("Action = "+action+" ("+concerns+")");
				if (action.equals(ImportFileFieldConstants.ACTION_TYPE_BIRTH)) {
					/** BIRTH */
					if (ImportFileFieldConstants.ACTION_CONCERNS_CURRENT_PERSON.equals(concerns)) {
						if (!fullImport()) {
							return false;
						}
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_MOVED_TO_ANOTHER_COUNTRY)) {
					/** MOVING TO ANOTHER COUNTRY */
					// Lelegt hondlun her, vantar td. a� fletta i gegnum types
					// og skoda betur
					if (ImportFileFieldConstants.ACTION_CONCERNS_CURRENT_PERSON.equals(concerns)) {
						if (!handleMoving(user)) {
							return false;
						}
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_MOVED)) {
					/** MOVING */
					// Lelegt hondlun her, vantar td. a� fletta i gegnum types
					// og skoda betur
					if (ImportFileFieldConstants.ACTION_CONCERNS_CURRENT_PERSON.equals(concerns)) {
						if (!handleMoving(user)) {
							return false;
						}
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_DEATH)) {
					/** DEATH */
					if (ImportFileFieldConstants.ACTION_CONCERNS_CURRENT_PERSON.equals(concerns)) {
						String dateOfDeactivationString = getUserProperty(ImportFileFieldConstants.DEACTIVATION_DATE_COLUMN);
						IWTimestamp dateOfDeactivation = null;
						if (dateOfDeactivationString != null) {
							dateOfDeactivation = getDateFromString(dateOfDeactivationString);
						}
						this.deceasedMap.put(PIN, dateOfDeactivation);
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_MIDDLE_NAME)) {
					/** CHANGING THE FIRST PART OF THE LAST NAME */
					String firstName = user.getFirstName();
					String middleName = user.getMiddleName();
					String lastName = (getUserProperty(ImportFileFieldConstants.LAST_NAME_COLUMN) != null) ? getUserProperty(ImportFileFieldConstants.LAST_NAME_COLUMN)
							: user.getLastName();
					String preferredNameIndex = getUserProperty(ImportFileFieldConstants.PREFERRED_FIRST_NAME_INDEX_COLUMN);
					String firstPartOfLast = getUserProperty(ImportFileFieldConstants.FIRST_PART_OF_LAST_NAME_COLUMN,
							null);
					lastName = handleDoubleLastName(lastName, firstPartOfLast);
					getImportBusiness().handleNames(user, firstName, middleName, lastName, preferredNameIndex, true);
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_LAST_NAME)) {
					/** CHANGING THE LAST NAME */
					String firstName = user.getFirstName();
					String middleName = user.getMiddleName();
					String lastName = (getUserProperty(ImportFileFieldConstants.LAST_NAME_COLUMN) != null) ? getUserProperty(
							ImportFileFieldConstants.LAST_NAME_COLUMN, "")
							: user.getLastName();
					String preferredNameIndex = getUserProperty(ImportFileFieldConstants.PREFERRED_FIRST_NAME_INDEX_COLUMN);
					String firstPartOfLast = getUserProperty(ImportFileFieldConstants.FIRST_PART_OF_LAST_NAME_COLUMN,
							null);
					lastName = handleDoubleLastName(lastName, firstPartOfLast);
					getImportBusiness().handleNames(user, firstName, middleName, lastName, preferredNameIndex, true);
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_MARRIAGE)) {
					String relPIN = getUserProperty(ImportFileFieldConstants.RELATIVE_PIN_COLUMN);
					User spouse = null;
					try {
						spouse = this.comUserBiz.getUser(relPIN);
					}
					catch (Exception e) {
						logError("NackaImportFileHandler : cant find spouse");
					}
					if (spouse != null) {
						if (ImportFileFieldConstants.ACTION_PREFIX_CANCEL.equals(prefix)) {
							try {
								this.relationBiz.removeAsSpouseFor(spouse, user);
							}
							catch (RemoveException e1) {
								e1.printStackTrace();
							}
						}
						else {
							try {
								this.relationBiz.setAsSpouseFor(spouse, user);
							}
							catch (CreateException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_CHANGE_ADDRESS)) {
					if (!handleMoving(user)) {
						return false;
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_SPECIAL_FIRST_NAME)) {
					handleNames(user, true);
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_NEW_PERSONAL_ID)) {
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_DIVORCE)) {
					String relPIN = getUserProperty(ImportFileFieldConstants.RELATIVE_PIN_COLUMN);
					User spouse = null;
					try {
						spouse = this.comUserBiz.getUser(relPIN);
					}
					catch (Exception e) {
						logError("NackaImportFileHandler : cant find spouse");
					}
					if (spouse != null) {
						try {
							this.relationBiz.removeAsSpouseFor(spouse, user);
						}
						catch (RemoveException e1) {
							logError("NackaImportFileHandler : cannot remove spouse");
							return false;
						}
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_CITIZENSHIP)) {
					if (!fullImport()) {
						return false;
					}
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_SECRET)) {
					handleSecretPerson(user);
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_CUSTODY_RELATIONS)) {
					/** RELATIONS ONLY */
				}
				else if (action.equals(ImportFileFieldConstants.ACTION_TYPE_SPECIAL_CO_ADDRESS)) {
					if (ImportFileFieldConstants.ACTION_CONCERNS_CURRENT_PERSON.equals(concerns)) {
						if (!handleMoving(user)) {
							return false;
						}
					}
				}
				else {
					/** FULL IMPORT */
					if (this.unhandledActions.containsKey(actions[i][3])) {
						this.unhandledActions.put(actions[i][3], new Integer(
								((Integer) this.unhandledActions.get(actions[i][3])).intValue() + 1));
					}
					else {
						this.unhandledActions.put(actions[i][3], new Integer(1));
					}
					if (!fullImport()) {
						return false;
					}
				}
			}
		}
		// finished with this user
		user = null;
		return true;
	}
	
	protected void addRelations() {
		ArrayList relatives = (ArrayList) this.userPropertiesMap.get(ImportFileFieldConstants.RELATIONAL_SECTION_STARTS);
		this.relationsMap.put(getUserProperty(ImportFileFieldConstants.PIN_COLUMN), relatives);
	}

	protected Group convertUserToGroup(User user) {
		try {
			return user.getUserGroup();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean fullImport() throws RemoteException {
		User user = null;
		String actionType = getUserProperty(ImportFileFieldConstants.ACTION_TYPE_COLUMN);
		// variables
		String firstName = getUserProperty(ImportFileFieldConstants.FIRST_NAME_COLUMN, "");
		String middleName = "";
		String lastNameFirstPart = getUserProperty(ImportFileFieldConstants.FIRST_PART_OF_LAST_NAME_COLUMN, null);
		String lastName = getUserProperty(ImportFileFieldConstants.LAST_NAME_COLUMN, "");
		String preferredNameIndex = getUserProperty(ImportFileFieldConstants.PREFERRED_FIRST_NAME_INDEX_COLUMN);
		String pid = getUserProperty(ImportFileFieldConstants.PIN_COLUMN);
		if (lastNameFirstPart != null) {
			if (preferredNameIndex == null) {
				preferredNameIndex = "10";
			}
			lastName = lastNameFirstPart + " " + lastName;
		}
		String countyNumber = getUserProperty(ImportFileFieldConstants.COUNTY_CODE_COLUMN);
		String communeNumber = getUserProperty(ImportFileFieldConstants.COMMUNE_CODE_COLUMN);
		String communeCode = countyNumber + communeNumber;
		Commune commune = null;
		try {
			commune = getCommuneHome().findByCommuneCode(communeCode);
		}
		catch (FinderException e1) {
			logDebug("Commune with code:" + communeCode + " (countyNumber+communeNumber) not found in database");
		}
		String dateOfRegistrationString = getUserProperty(ImportFileFieldConstants.REGISTRATION_DATE_COLUMN);
		IWTimestamp dateOfRegistration = null;
		if (dateOfRegistrationString != null) {
			dateOfRegistration = getDateFromString(dateOfRegistrationString);
		}
		IWTimestamp dateOfDeactivation = null;
		boolean isMovingFromHomeCommune = false;
		if (actionType != null) {
			String dateOfActionString = getUserProperty(ImportFileFieldConstants.DEACTIVATION_DATE_COLUMN);
			if (dateOfActionString != null) {
				dateOfDeactivation = getDateFromString(dateOfActionString);
			}
			// check if the deactivation concerns the current person
			if (actionType.endsWith(ImportFileFieldConstants.ACTION_CONCERNS_CURRENT_PERSON)) {
				// why is this person deactivated
				if (actionType.startsWith(ImportFileFieldConstants.ACTION_TYPE_DEATH)) {
					this.deceasedMap.put(pid, dateOfDeactivation);
				}
				else if (actionType.startsWith(ImportFileFieldConstants.ACTION_TYPE_MOVED)) {
					if (commune != null && this.homeCommune != null) {
						if (!commune.equals(this.homeCommune)) {
							isMovingFromHomeCommune = true;
						}
					}
					else {
						isMovingFromHomeCommune = false;
					}
				}
				else if (actionType.startsWith(ImportFileFieldConstants.ACTION_TYPE_MOVED_TO_ANOTHER_COUNTRY)) {
					isMovingFromHomeCommune = true;
				}
			}
		}
		String secrecy = getUserProperty(ImportFileFieldConstants.SECRECY_MARKING_COLUMN);
		boolean secretPerson = "J".equals(secrecy);
		// TODO should not be necessary because of deactivation check
		isMovingFromHomeCommune = isMovingFromHomeCommune || !this.HOME_COMMUNE_CODE.equals(communeCode);
		if (secrecy != null && !secrecy.equals("")) {
			log("SECRET PERSON = " + pid + " Code :" + secrecy + ".");
		}
		if (pid == null) {
			return false;
		}
		Gender gender = getGenderFromPin(pid);
		IWTimestamp dateOfBirth = getBirthDateFromPin(pid);
		/**
		 * basic user info
		 */
		try {
			user = this.comUserBiz.createOrUpdateCitizenByPersonalID(firstName, middleName, lastName, pid, gender,
					dateOfBirth);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		user = getImportBusiness().handleNames(user, user.getFirstName(), user.getMiddleName(), user.getLastName(),
				preferredNameIndex, false);
		if (!secretPerson) {
			if (!handleAddress(user, countyNumber, commune)) {
				return false;
			}
		}
		/**
		 * Main group relation add to the Nacka root group or move from it
		 */
		if (secretPerson) {
			user.setDescription("secret");
			// remove family ties!
			this.relationBiz.removeAllFamilyRelationsForUser(user);
			// remove address
			try {
				user.removeAllAddresses();
			}
			catch (IDORemoveRelationshipException e) {
				// e.printStackTrace();
			}
			if (this.fix) {
				// this will force a new record in the relation table
				try {
					this.comUserBiz.getRootProtectedCitizenGroup().removeUser(user, this.performer);
				}
				catch (Exception e) {
				}
			}
			this.comUserBiz.moveCitizenToProtectedCitizenGroup(user, IWTimestamp.getTimestampRightNow(), this.performer);
		}
		else if (isMovingFromHomeCommune) {
			// this will force a new record in the relation table
			if (this.fix) {
				// this will force a new record in the relation table
				try {
					// try to get the current user
					User currentUser;
					try {
						currentUser = IWContext.getInstance().getCurrentUser();
					}
					catch (Exception ex) {
						currentUser = null;
					}
					this.comUserBiz.getRootOtherCommuneCitizensGroup().removeUser(user, currentUser);
				}
				catch (Exception e) {
				}
			}
			if (dateOfDeactivation != null) {
				this.comUserBiz.moveCitizenFromCommune(user, dateOfDeactivation.getTimestamp(), this.performer);
			}
			else {
				this.comUserBiz.moveCitizenFromCommune(user, IWTimestamp.getTimestampRightNow(), this.performer);
			}
		}
		else {
			if (this.fix) {
				// this will force a new record in the relation table
				try {
					// try to get the current user
					User currentUser;
					try {
						currentUser = IWContext.getInstance().getCurrentUser();
					}
					catch (Exception ex) {
						currentUser = null;
					}
					this.comUserBiz.getRootCitizenGroup().removeUser(user, currentUser);
				}
				catch (Exception e) {
				}
			}
			if (dateOfRegistration != null) {
				this.comUserBiz.moveCitizenToCommune(user, dateOfRegistration.getTimestamp(), this.performer);
			}
			else {
				this.comUserBiz.moveCitizenToCommune(user, IWTimestamp.getTimestampRightNow(), this.performer);
			}
		}

		/**
		 * Save the user to the database
		 */
		user.store();
		// finished with this user
		user = null;
		return true;
	}

	private String getAndCutFragmentFromRecord(String record, String start, String end) {
		int startIndex = record.indexOf(start);
		int endIndex = record.lastIndexOf(end);
		String fragment = null;
		if ((startIndex != -1) && (endIndex != -1)) {
			StringBuffer buf = new StringBuffer();
			buf.append(record.substring(0, startIndex));
			buf.append(record.substring(endIndex, record.length()));
			fragment = record.substring(startIndex, endIndex + end.length());
			record = buf.toString();
		}
		return fragment;
	}

	protected ArrayList getArrayListWithMapsFromStringFragment(String record, String fragmentStart, String fragmentEnd) {
		ArrayList list = null;
		String fragment = getAndCutFragmentFromRecord(record, fragmentStart, fragmentEnd);
		if (fragment != null) {
			LineNumberReader reader = new LineNumberReader(new StringReader(fragment));
			list = new ArrayList();
			String line = null;
			StringBuffer buf = null;
			try {
				while ((line = reader.readLine()) != null) {
					if (buf == null) {
						buf = new StringBuffer();
					}
					buf.append(line);
					buf.append('\n');
					if (line.indexOf(fragmentEnd) != -1) {
						list.add(getPropertiesMapFromString(buf.toString(), " "));
						buf = null;
					}
				}
				reader.close();
				reader = null;
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return list;
	}

	private IWTimestamp getBirthDateFromPin(String pin) {
		// pin format = 190010221208 yyyymmddxxxx
		int dd = Integer.parseInt(pin.substring(6, 8));
		int mm = Integer.parseInt(pin.substring(4, 6));
		int yyyy = Integer.parseInt(pin.substring(0, 4));
		IWTimestamp dob = new IWTimestamp(dd, mm, yyyy);
		return dob;
	}

	protected CommuneHome getCommuneHome() {
		if (this.communeHome == null) {
			try {
				this.communeHome = (CommuneHome) getIDOHome(Commune.class);
			}
			catch (RemoteException e) {
				log(e);
			}
		}
		return this.communeHome;
	}

	/**
	 * @param dateOfRegistrationString
	 */
	private IWTimestamp getDateFromString(String dateOfRegistrationString) {
		int year = Integer.parseInt(dateOfRegistrationString.substring(0, 4));
		int month = Integer.parseInt(dateOfRegistrationString.substring(4, 6));
		int day = Integer.parseInt(dateOfRegistrationString.substring(6, 8));
		IWTimestamp date = new IWTimestamp(day, month, year);
		return date;
	}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
	 */
	public List getFailedRecords() {
		return this.failedRecords;
	}

	private Gender getGenderFromPin(String pin) {
		// pin format = 190010221208 second last number is the gender
		// even number = female
		// odd number = male
		try {
			GenderHome home = (GenderHome) this.getIDOHome(Gender.class);
			if (Integer.parseInt(pin.substring(10, 11)) % 2 == 0) {
				if (this.female == null) {
					this.female = home.getFemaleGender();
				}
				return this.female;
			}
			else {
				if (this.male == null) {
					this.male = home.getMaleGender();
				}
				return this.male;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null; // if something happened
		}
	}

	protected Map getPropertiesMapFromString(String propsString, String seperator) {
		Map map = new HashMap();
		String line = null;
		String property = null;
		String value = null;
		int index = -1;
		LineNumberReader reader = new LineNumberReader(new StringReader(propsString));
		try {
			while ((line = reader.readLine()) != null) {
				// TEMP HARDCODING ...
				if (line.indexOf("#ANTAL_POSTER") != -1) {
					return new HashMap();
				}
				//
				if ((index = line.indexOf(seperator)) != -1) {
					property = line.substring(0, index);
					value = line.substring(index + 1, line.length());
					map.put(property, value);
					// log("Param:"+property+" Value:"+value);
				}
			}
			reader.close();
			reader = null;
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}

	protected String getUserProperty(String propertyName) {
		return (String) this.userPropertiesMap.get(propertyName);
	}

	protected String getUserProperty(String propertyName, String StringToReturnIfNotSet) {
		String value = getUserProperty(propertyName);
		if (value == null) {
			value = StringToReturnIfNotSet;
		}
		return value;
	}

	private boolean isSecretPerson() {
		String secrecy = getUserProperty(ImportFileFieldConstants.SECRECY_MARKING_COLUMN);
		String PIN = getUserProperty(ImportFileFieldConstants.PIN_COLUMN);
		if (secrecy != null && !secrecy.equals("")) {
			log("SECRET PERSON = " + PIN + " Code :" + secrecy + ".");
		}
		return "J".equals(secrecy);
	}

	/**
	 * Method to parse the string that is gotten from the ACTION_TYPE_COLUMN
	 * Returns a String[][], like so actions[counter][0] = key;
	 * actions[counter][1] = value; actions[counter][2] =
	 * Integer.toString(prefix);
	 * 
	 * @param actionType
	 * @return
	 */
	private String[][] parseAction(String actionType) {
		List actionTypes = TextSoap.FindAllWithSeparator(actionType, ",");
		String[][] actions = new String[0][0];
		if (actionTypes != null) {
			actions = new String[actionTypes.size()][4];
			Iterator iter = actionTypes.iterator();
			String action;
			String key, value;
			int iKey;
			int prefix;
			int index;
			int counter = 0;
			while (iter.hasNext()) {
				try {
					action = (String) iter.next();
					index = action.indexOf(ImportFileFieldConstants.ACTION_CONCERNS_CURRENT_PERSON);
					if (index == -1) {
						index = action.indexOf(ImportFileFieldConstants.ACTION_CONCERNS_RELATIVE);
					}
					if (index != -1) {
						key = action.substring(0, index);
						value = action.substring(index);
					}
					else {
						key = action;
						value = null;
					}
					iKey = Integer.parseInt(key);
					prefix = iKey / 100;
					iKey = iKey % 100;
					actions[counter][0] = Integer.toString(iKey);
					actions[counter][1] = value;
					actions[counter][2] = Integer.toString(prefix);
					actions[counter][3] = action;
				}
				catch (Exception e) {
					e.printStackTrace(System.err);
					logError("NackaImportFileHandler - Exception caught, continuing");
				}
				++counter;
			}
		}
		return actions;
	}


	protected void storeRelations() throws RemoteException {
		ArrayList errors = new ArrayList();
		HashtableMultivalued parentRelations = new HashtableMultivalued();
		// log("Skipping relations ... : setting relationMap == null");
		// relationsMap = null;
		log("NackaImportFileHandler [STARTING - RELATIONS] time: " + IWTimestamp.getTimestampRightNow().toString());
		// get keys <- pins
		// get user bean
		// get relative bean
		// if found link with RelationBusiness
		// else skip relative and log somewhere
		Timer clock = new Timer();
		clock.start();
		int count = 0;
		if (this.relationsMap != null) {
			Iterator iter = this.relationsMap.keySet().iterator();
			User user;
			User relative;
			String relativePIN;
			String PIN = "";
			String relationType;
			try {
				// begin transaction
				this.transaction2.begin();
				while (iter.hasNext()) {
					++count;
					if ((count % 250) == 0) {
						log("NackaImportFileHandler storing relations [" + count + "] time: "
								+ IWTimestamp.getTimestampRightNow().toString());
					}
					PIN = (String) iter.next();
					user = null;
					ArrayList relatives = (ArrayList) this.relationsMap.get(PIN);
					if (relatives != null) {
						user = this.home.findByPersonalID(PIN);
						boolean secretPerson = false;
						String relationStatus;
						secretPerson = "secret".equals(user.getDescription());
						if (!secretPerson) {
							Iterator iter2 = relatives.iterator();
							while (iter2.hasNext()) {
								Map relativeMap = (Map) iter2.next();
								relativePIN = (String) relativeMap.get(ImportFileFieldConstants.RELATIVE_PIN_COLUMN);
								relationType = (String) relativeMap.get(ImportFileFieldConstants.RELATIVE_TYPE_COLUMN);
								relationStatus = (String) relativeMap.get(ImportFileFieldConstants.RELATIVE_STATUS_COLUMN);
								/**
								 * @todo use this second parameter if first is
								 *       missing??? ask kjell
								 */
								// if( relativePIN == null ) relativePIN =
								// (String)
								// item.get("02002"));
								if (relativePIN != null) {
									try {
										if (relationType != null) {
											relative = this.home.findByPersonalID(relativePIN);
											secretPerson = "secret".equals(relative.getDescription());
											if (!secretPerson) {
												if (relationType.equals(ImportFileFieldConstants.RELATION_TYPE_CHILD)) {
													// relationBiz.setAsChildFor(relative,user);
													// for custodian check
													parentRelations.put(user.getPrimaryKey(), relative.getPrimaryKey());
												}
												else if (relationType.equals(ImportFileFieldConstants.RELATION_TYPE_SPOUSE)) {
													this.relationBiz.setAsSpouseFor(relative, user);
												}
												else if (relationType.equals(ImportFileFieldConstants.RELATION_TYPE_FATHER)) {
													// relationBiz.setAsChildFor(user,relative);
													parentRelations.put(relative.getPrimaryKey(), user.getPrimaryKey());
												}
												else if (relationType.equals(ImportFileFieldConstants.RELATION_TYPE_MOTHER)) {
													// relationBiz.setAsChildFor(user,relative);
													parentRelations.put(relative.getPrimaryKey(), user.getPrimaryKey());
												}
												else if (relationType.equals(ImportFileFieldConstants.RELATION_TYPE_CUSTODY)) { // custody
													if (ImportFileFieldConstants.RELATION_STATUS_CANCELLED.equals(relationStatus)) {
														System.out.println("removing custody relation ("
																+ user.getPersonalID() + ", "
																+ relative.getPersonalID() + ")");
														this.relationBiz.removeAsCustodianFor(user, relative);
													}
													else {
														this.relationBiz.setAsCustodianFor(user, relative);
													}
												}
												else if (relationType.equals(ImportFileFieldConstants.RELATION_TYPE_CUSTODY2)) { // custody
													if (ImportFileFieldConstants.RELATION_STATUS_CANCELLED.equals(relationStatus)) {
														System.out.println("removing (reverse) custody relation ("
																+ user.getPersonalID() + ", "
																+ relative.getPersonalID() + ")");
														this.relationBiz.removeAsCustodianFor(relative, user); // backwards
													}
													else {
														this.relationBiz.setAsCustodianFor(relative, user); // backwards
													}
												}
											}
											else {
												errors.add("NackaImporter: Error Relation type not defined for relative ( pin "
														+ relativePIN + " ) of user: " + PIN);
												// log("NackaImporter:
												// Error
												// Relation type not defined
												// for relative ( pin
												// "+relativePIN+" ) of user:
												// "+PIN);
											}
										}
										// other types
									}
									catch (CreateException ex) {
										errors.add("NackaImporter : Error adding relation for user: " + PIN);
										// log("NackaImporter :
										// Error adding
										// relation for user: "+PIN);
										// ex.printStackTrace();
									}
									catch (FinderException ex) {
										errors.add("NackaImporter : Error relative (pin " + relativePIN
												+ ") not found in database for user: " + PIN);
										// log("NackaImporter :
										// Error relative
										// (pin "+relativePIN+") not found in
										// database for
										// user: "+PIN);
										// ex.printStackTrace();
									}
								} // if relativepin !=null
								else {
									errors.add("NackaImporter : Error relative has no PIN and skipping for parent user: "
											+ PIN);
									// log("NackaImporter :
									// Error relative has
									// no PIN and skipping for parent user:
									// "+PIN);
								}
							} // end while iter2
						} // end if secret
					} // end if relative
				} // end while iter
				handleCustodyAndChildRelations(parentRelations);
				// success commit
				this.transaction2.commit();
				Iterator err = errors.iterator();
				while (err.hasNext()) {
					String item = (String) err.next();
					logError(item);
				}
			}
			catch (Exception e) {
				if (e instanceof RemoteException) {
					throw (RemoteException) e;
				}
				else if (e instanceof FinderException) {
					log("NackaImporter : Error user (pin " + PIN
							+ ") not found in database must be an incomplete database");
					log("NackaImporter : Rollbacking");
					try {
						this.transaction2.rollback();
					}
					catch (SystemException ec) {
						ec.printStackTrace();
					}
				}
				else {
					e.printStackTrace();
				}
			}
		} // end if relationmap !=null
		else {
			log("NackaImporter : No relations read");
		}
		clock.stop();
		log("Time to store relations: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000)) + " s");
		clock.start();
		log("NackaImportFileHandler [STARTING - DECEASED RELATIONS] (without transaction) time: "
				+ IWTimestamp.getTimestampRightNow().toString());
		if (this.deceasedMap != null) {
			Iterator iter = this.deceasedMap.keySet().iterator();
			User user;
			IWTimestamp dateOfDeactivation = null;
			String PIN;
			count = 0;
			try {
				while (iter.hasNext()) {
					++count;
					if ((count % 250) == 0) {
						log("NackaImportFileHandler storing deceased relations [" + count + "] time: "
								+ IWTimestamp.getTimestampRightNow().toString());
					}
					PIN = (String) iter.next();
					dateOfDeactivation = (IWTimestamp) this.deceasedMap.get(PIN);
					user = this.home.findByPersonalID(PIN);
					// log("NackaImportFileHandler - setUserAsDeceased (
					// "+user.getPrimaryKey()+" )");
					if (dateOfDeactivation != null) {
						this.comUserBiz.setUserAsDeceased(((Integer) user.getPrimaryKey()), dateOfDeactivation.getDate());
					}
					else {
						this.comUserBiz.setUserAsDeceased(((Integer) user.getPrimaryKey()), IWTimestamp.RightNow().getDate());
					}
				}
				log("NackaImportFileHandler stored deceased relations : Total = [" + count + "] time: "
						+ IWTimestamp.getTimestampRightNow().toString());
			}
			catch (FinderException e) {
				e.printStackTrace();
			}
		}
		else {
			log("NackaImporter : No deceased relations read");
		}
		clock.stop();
		log("Time to store deceased relations: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000))
				+ " s");
	}


	// /////////////////////////////////////////////////
	// handlers
	// /////////////////////////////////////////////////
	/**
	 * @param lastName
	 * @param firstPartOfLast
	 * @return
	 */
	private String handleDoubleLastName(String lastName, String firstPartOfLast) {
		if (lastName == null) {
			lastName = "";
		}
		if (firstPartOfLast == null || "".equals(firstPartOfLast)
				|| ImportFileFieldConstants.EMPTY_FIELD_CHARACTER.equals(firstPartOfLast)) {
			int index = lastName.indexOf(" ");
			if (index != -1 && lastName.length() > 0) {
				lastName = lastName.substring(index + 1);
			}
		}
		else {
			int index = lastName.indexOf(firstPartOfLast + " ");
			while (index == 0) {
				// Removing previous instances of firstPartOfLastNaem
				lastName = lastName.substring(index + firstPartOfLast.length() + 1);
				index = lastName.indexOf(firstPartOfLast + " ");
			}
			if (lastName != null && lastName.length() > 0) {
				lastName = firstPartOfLast + " " + lastName;
			}
			else {
				lastName = firstPartOfLast;
			}
		}
		return lastName;
	}

	/**
	 * @param user
	 * @param countyNumber
	 * @param commune
	 * @return
	 */
	private boolean handleAddress(User user, String countyNumber, Commune commune) {
		/**
		 * addresses
		 */
		// main address
		// country id 187 name Sweden isoabr: SE
		String addressLine = getUserProperty(ImportFileFieldConstants.ADDRESS_COLUMN);
		String coAddressLine = getUserProperty(ImportFileFieldConstants.CO_ADDRESS_COLUMN);
		String foreignAddressLine1 = getUserProperty(ImportFileFieldConstants.FOREIGN_ADDRESS_1_COLUMN);
		String addressKeyCode = getUserProperty(ImportFileFieldConstants.ADDRESS_KEY_CODE);
		if ((addressLine != null) && this.importAddresses) {
			try {
				String streetName = this.addressBiz.getStreetNameFromAddressString(addressLine);
				String streetNumber = this.addressBiz.getStreetNumberFromAddressString(addressLine);
				String postalCode = getUserProperty(ImportFileFieldConstants.POSTAL_CODE_COLUMN);
				String postalName = getUserProperty(ImportFileFieldConstants.POSTAL_NAME_COLUMN);
				Address address = this.comUserBiz.getUsersMainAddress(user);
				Country sweden = ((CountryHome) getIDOHome(Country.class)).findByIsoAbbreviation("SE");
				PostalCode code = null;
				if (postalName != null) {
					code = this.addressBiz.getPostalCodeAndCreateIfDoesNotExist(postalCode, postalName, sweden);
				}
				boolean addAddress = false;
				/** @todo is this necessary?* */
				if (address == null) {
					AddressHome addressHome = this.addressBiz.getAddressHome();
					address = addressHome.create();
					AddressType mainAddressType = addressHome.getAddressType1();
					address.setAddressType(mainAddressType);
					addAddress = true;
				}
				address.setCountry(sweden);
				if (code != null) {
					address.setPostalCode(code);
				}
				// address.setProvince("Nacka" );//set as 01 ?
				// address.setCity("Stockholm" );//set as 81?
				address.setProvince(countyNumber);
				if (commune != null) {
					address.setCity(commune.getCommuneName());
					address.setCommune(commune);
				}
				address.setStreetName(streetName);
				address.setStreetNumber(streetNumber);
				AddressCoordinate ac = getAddressCoordinate(addressKeyCode);
				if (ac != null) {
					address.setCoordinate(ac);
				}
				address.store();
				if (addAddress) {
					user.addAddress(address);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		else if (coAddressLine != null && this.importAddresses) {
			try {
				// String addressName = getUserProperty(CO_ADDRESS_NAME_COLUMN);
				// // Not used
				String streetName = this.addressBiz.getStreetNameFromAddressString(coAddressLine);
				String streetNumber = this.addressBiz.getStreetNumberFromAddressString(coAddressLine);
				String postalCode = getUserProperty(ImportFileFieldConstants.CO_POSTAL_CODE_COLUMN);
				String postalName = getUserProperty(ImportFileFieldConstants.CO_POSTAL_NAME_COLUMN);
				Address address = this.comUserBiz.getUsersCoAddress(user);
				Country sweden = ((CountryHome) getIDOHome(Country.class)).findByIsoAbbreviation("SE");
				PostalCode code = this.addressBiz.getPostalCodeAndCreateIfDoesNotExist(postalCode, postalName, sweden);
				boolean addAddress = false;
				/** @todo is this necessary?* */
				if (address == null) {
					AddressHome addressHome = this.addressBiz.getAddressHome();
					address = addressHome.create();
					AddressType coAddressType = addressHome.getAddressType2();
					address.setAddressType(coAddressType);
					addAddress = true;
				}
				address.setCountry(sweden);
				address.setPostalCode(code);
				// address.setProvince("Nacka" );//set as 01 ?
				// address.setCity("Stockholm" );//set as 81?
				address.setProvince(countyNumber);
				if (commune != null) {
					address.setCity(commune.getCommuneName());
					address.setCommune(commune);
				}
				address.setStreetName(streetName);
				address.setStreetNumber(streetNumber);
				address.store();
				if (addAddress) {
					user.addAddress(address);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}// foreign adress
		else if (foreignAddressLine1 != null) {
			String foreignAddressLine2 = getUserProperty(ImportFileFieldConstants.FOREIGN_ADDRESS_2_COLUMN, "");
			String foreignAddressLine3 = getUserProperty(ImportFileFieldConstants.FOREIGN_ADDRESS_3_COLUMN, "");
			String foreignAddressCountry = getUserProperty(ImportFileFieldConstants.FOREIGN_ADDRESS_COUNTRY_COLUMN, "");
			final String space = " ";
			StringBuffer addressBuf = new StringBuffer();
			addressBuf.append(foreignAddressLine1).append(space).append(foreignAddressLine2).append(space).append(
					foreignAddressLine3).append(space).append(foreignAddressCountry);
			try {
				String streetName = TextSoap.findAndReplace(addressBuf.toString(), "  ", " ");
				Address address = this.comUserBiz.getUsersMainAddress(user);
				boolean addAddress = false;
				/** @todo is this necessary?* */
				if (address == null) {
					AddressHome addressHome = this.addressBiz.getAddressHome();
					address = addressHome.create();
					AddressType mainAddressType = addressHome.getAddressType1();
					address.setAddressType(mainAddressType);
					addAddress = true;
				}
				address.setStreetName(streetName);
				address.setStreetNumber("");
				address.setCommune(null);
				address.store();
				if (addAddress) {
					user.addAddress(address);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	private AddressCoordinate getAddressCoordinate(String addressKeyCode) throws IDOLookupException {
		if (addressKeyCode != null) {
			if (this.coordinateMap.containsKey(addressKeyCode)) {
				// Can return null
				return (AddressCoordinate) this.coordinateMap.get(addressKeyCode);
			}
			else {
				AddressCoordinateHome ach = (AddressCoordinateHome) IDOLookup.getHome(AddressCoordinate.class);
				AddressCoordinate ac = null;
				try {
					ac = ach.findByCoordinate(addressKeyCode);
				}
				catch (FinderException f) {
					System.out.println("[NackaImportFileHandlreBean] Address Coordinate not found (possibly not imported yet : "
							+ addressKeyCode + ")");
				}
				this.coordinateMap.put(addressKeyCode, ac);
				return ac;
			}
		}
		return null;
	}

	private void handleCustodyAndChildRelations(HashtableMultivalued table) {
		if (table != null) {
			try {
				Iterator keys = table.keySet().iterator();
				while (keys.hasNext()) {
					Integer parentId = (Integer) keys.next();
					Collection coll = table.getCollection(parentId);
					Iterator colIt = coll.iterator();
					while (colIt.hasNext()) {
						Integer childId = (Integer) colIt.next();
						User parent = this.comUserBiz.getUser(parentId);
						User child = this.comUserBiz.getUser(childId);
						// Collection custodians =
						// parent.getRelatedBy(relationBiz.getCustodianRelationType()
						// );
						if (coll == null || coll.isEmpty()) {
							// and as custodian
							parent.addUniqueRelation(
									((Integer) (this.convertUserToGroup(child).getPrimaryKey())).intValue(),
									this.relationBiz.getCustodianRelationType());
						}
						this.relationBiz.setAsParentFor(parent, child);
					}
				}
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
			/*
			 * catch (FinderException e) { e.printStackTrace();
			 */
			catch (CreateException e) {
				e.printStackTrace();
				logError("NackaImportHandler Failed to create parent relation");
			}
			catch (EJBException e) {
				e.printStackTrace();
				logError("NackaImportHandler Failed to create parent relation");
			}
		}
	}

	/**
	 * @param user
	 * @return
	 */
	private boolean handleMoving(User user) throws RemoteException {
		String countyNumber = getUserProperty(ImportFileFieldConstants.COUNTY_CODE_COLUMN);
		String communeNumber = getUserProperty(ImportFileFieldConstants.COMMUNE_CODE_COLUMN);
		String communeCode = countyNumber + communeNumber;
		// System.out.println(firstName+" "+middleName+" "+lastName);
		// System.out.print(" ...CommuneCode = "+communeCode);
		Commune commune = null;
		try {
			commune = getCommuneHome().findByCommuneCode(communeCode);
		}
		catch (FinderException e1) {
			// logWarning("Commune with code:"+communeCode+"
			// (countyNumber+communeNumber) not found in database");
		}
		boolean isMovingFromHomeCommune = false;
		if (commune != null && this.homeCommune != null) {
			if (!commune.equals(this.homeCommune)) {
				isMovingFromHomeCommune = true;
			}
		}
		else {
			isMovingFromHomeCommune = false;
		}
		if (!handleAddress(user, countyNumber, commune)) {
			return false;
		}
		if (isMovingFromHomeCommune) {
			// this will force a new record in the relation table
			if (this.fix) {
				// this will force a new record in the relation table
				try {
					// try to get the current user
					User currentUser;
					try {
						currentUser = IWContext.getInstance().getCurrentUser();
					}
					catch (Exception ex) {
						currentUser = null;
					}
					this.comUserBiz.getRootOtherCommuneCitizensGroup().removeUser(user, currentUser);
				}
				catch (Exception e) {
				}
			}
			IWTimestamp dateOfDeactivation = null;
			String dateOfActionString = getUserProperty(ImportFileFieldConstants.DEACTIVATION_DATE_COLUMN);
			if (dateOfActionString != null) {
				dateOfDeactivation = getDateFromString(dateOfActionString);
			}
			if (dateOfDeactivation != null) {
				this.comUserBiz.moveCitizenFromCommune(user, dateOfDeactivation.getTimestamp(), this.performer);
			}
			else {
				this.comUserBiz.moveCitizenFromCommune(user, IWTimestamp.getTimestampRightNow(), this.performer);
			}
		}
		else {
			if (this.fix) {
				// this will force a new record in the relation table
				try {
					// try to get the current user
					User currentUser;
					try {
						currentUser = IWContext.getInstance().getCurrentUser();
					}
					catch (Exception ex) {
						currentUser = null;
					}
					this.comUserBiz.getRootCitizenGroup().removeUser(user, currentUser);
				}
				catch (Exception e) {
				}
			}
			String dateOfRegistrationString = getUserProperty(ImportFileFieldConstants.REGISTRATION_DATE_COLUMN);
			IWTimestamp dateOfRegistration = null;
			if (dateOfRegistrationString != null) {
				dateOfRegistration = getDateFromString(dateOfRegistrationString);
			}
			if (dateOfRegistration != null) {
				this.comUserBiz.moveCitizenToCommune(user, dateOfRegistration.getTimestamp(), this.performer);
			}
			else {
				this.comUserBiz.moveCitizenToCommune(user, IWTimestamp.getTimestampRightNow(), this.performer);
			}
		}
		return true;
	}

	/**
	 * @param updateName
	 * @param user
	 */
	private User handleNames(User user, boolean store) throws RemoteException {
		// variables
		String firstName = getUserProperty(ImportFileFieldConstants.FIRST_NAME_COLUMN, "");
		String middleName = "";
		String lastNameFirstPart = getUserProperty(ImportFileFieldConstants.FIRST_PART_OF_LAST_NAME_COLUMN, null);
		String lastName = getUserProperty(ImportFileFieldConstants.LAST_NAME_COLUMN, "");
		String preferredNameIndex = getUserProperty(ImportFileFieldConstants.PREFERRED_FIRST_NAME_INDEX_COLUMN);
		if (lastNameFirstPart != null) {
			lastName = lastNameFirstPart + " " + lastName;
		}
		return getImportBusiness().handleNames(user, firstName, middleName, lastName, preferredNameIndex, store);
	}

	/**
	 * @param user
	 * @throws RemoteException
	 */
	private void handleSecretPerson(User user) throws RemoteException {
		user.setDescription("secret");
		// remove family ties!
		this.relationBiz.removeAllFamilyRelationsForUser(user);
		// remove address
		try {
			user.removeAllAddresses();
		}
		catch (IDORemoveRelationshipException e) {
			// e.printStackTrace();
		}
		if (this.fix) {
			// this will force a new record in the relation table
			try {
				// try to get the current user
				User currentUser;
				try {
					currentUser = IWContext.getInstance().getCurrentUser();
				}
				catch (Exception ex) {
					currentUser = null;
				}
				this.comUserBiz.getRootProtectedCitizenGroup().removeUser(user, currentUser);
			}
			catch (Exception e) {
			}
		}
		this.comUserBiz.moveCitizenToProtectedCitizenGroup(user, IWTimestamp.getTimestampRightNow(), this.performer);
	}

	// /////////////////////////////////////////////////
	// setters
	// /////////////////////////////////////////////////
	public void setImportAddresses(boolean importAddresses) {
		this.importAddresses = importAddresses;
	}

	public void setImportFile(ImportFile file) {
		this.file = file;
	}

	public void setImportRelations(boolean importRelations) {
		this.importRelations = importRelations;
	}

	public void setImportUsers(boolean importUsers) {
		this.importUsers = importUsers;
	}

	public void setOnlyImportRelations(boolean onlyImportRelations) {
		setImportRelations(true);
		setImportUsers(false);
		setImportAddresses(false);
	}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#setRootGroup(Group)
	 */
	public void setRootGroup(Group group) {
	}

	public void setStartRecord(int startRecord) {
		this.startRecord = startRecord;
	}

	public NackaImportBusiness getImportBusiness() {
		try {
			return (NackaImportBusiness) IBOLookup.getServiceInstance(this.getIWApplicationContext(),
					NackaImportBusiness.class);
		}
		catch (IBOLookupException e) {
			throw new IBORuntimeException(e);
		}
	}
}