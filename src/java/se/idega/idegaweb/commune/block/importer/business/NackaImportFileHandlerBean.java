package se.idega.idegaweb.commune.block.importer.business;
import is.idega.idegaweb.member.business.MemberFamilyLogic;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import se.idega.idegaweb.commune.business.CommuneUserBusiness;

import com.idega.block.importer.data.ImportFile;
//import com.idega.block.process.business.CaseBusiness;
import com.idega.business.IBOServiceBean;
import com.idega.core.location.business.AddressBusiness;
import com.idega.core.location.data.Address;
import com.idega.core.location.data.AddressHome;
import com.idega.core.location.data.AddressType;
import com.idega.core.location.data.Commune;
import com.idega.core.location.data.CommuneHome;
import com.idega.core.location.data.Country;
import com.idega.core.location.data.CountryHome;
import com.idega.core.location.data.PostalCode;
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
 * <p>
 * Title: NackaImportFileHandlerBean
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright (c) 2002
 * </p>
 * <p>
 * Company: Idega Software
 * </p>
 * 
 * @author <a href="mailto:eiki@idega.is">Eirikur Sveinn Hrafnsson</a>
 * @version 1.1
 */

public class NackaImportFileHandlerBean extends IBOServiceBean implements NackaImportFileHandler {

	private static final String RELATIVE_TYPE_COLUMN = "02003";
	private static final String RELATIVE_PIN_COLUMN = "02001";
	//private static final String LONG_LAT_COLUMN = "01025";
	private static final String POSTAL_NAME_COLUMN = "01035";
	private static final String POSTAL_CODE_COLUMN = "01034";
	private static final String ADDRESS_COLUMN = "01033";
	private static final String PIN_COLUMN = "01001";
	private static final String LAST_NAME_COLUMN = "01014";
	private static final String FIRST_PART_OF_LAST_NAME_COLUMN = "01013";
	private static final String FIRST_NAME_COLUMN = "01012";
	private static final String PREFERRED_FIRST_NAME_INDEX_COLUMN = "01011";
	private static final String SECRECY_MARKING_COLUMN = "01003";

	private static final String REGISTRATION_DATE_COLUMN = "01021";

	private static final String DEACTIVATE_TYPE_COLUMN = "01004";
	//	private static final String DEACTIVATE_REASON_COLUMN = "01006";
	private static final String DEACTIVATE_DATE_COLUMN = "01007";
	//	#UP<WS>01004<WS>66H<LF>
	//	#UP<WS>01006<WS>AV<LF>
	//	#UP<WS>01007<WS>20350601<LF>
	private static final String DEACTIVATION_CONCERNS_CURRENT_PERSON = "H";
	//	private static final String DEACTIVATION_CONCERNS_RELATIVE = "R";
	private static final String DEACTIVATION_TYPE_DEATH = "66";
	private static final String DEACTIVATION_TYPE_MOVED_TO_ANOTHER_COMMUNE = "41";
	private static final String DEACTIVATION_TYPE_MOVED_TO_ANOTHER_COUNTRY = "43";

	//	When 01004 is 66 means deceased

	private static final String FOREIGN_ADDRESS_1_COLUMN = "01071";
	private static final String FOREIGN_ADDRESS_2_COLUMN = "01072";
	private static final String FOREIGN_ADDRESS_3_COLUMN = "01073";
	private static final String FOREIGN_ADDRESS_COUNTRY_COLUMN = "01077";

	private static final String COUNTY_CODE_COLUMN = "01022"; // for
	// stockholmarea
	// 01
	private static final String COMMUNE_CODE_COLUMN = "01023"; // for nacka 82

	//private static final String NACKA_CODE="0182";
	private String HOME_COMMUNE_CODE;
	CommuneHome communeHome;
	Commune homeCommune;

	//private static final String TEST_GROUP_ID_PARAMETER_NAME =
	// "citizen_test_group_id";
	private static final String FIX_PARAMETER_NAME = "run_fix";

	private int startRecord = 0;

	private static final String RELATIONAL_SECTION_STARTS = "02000";
	private static final String RELATIONAL_SECTION_ENDS = "02999";
	private static final String CITIZEN_INFO_SECTION_STARTS = "03000";
	private static final String CITIZEN_INFO_SECTION_ENDS = "03999";
	private static final String HISTORIC_SECTION_STARTS = "04000";
	private static final String HISTORIC_SECTION_ENDS = "04999";
	private static final String SPECIALCASE_RELATIONAL_SECTION_STARTS = "06000";
	private static final String SPECIALCASE_RELATIONAL_SECTION_ENDS = "06999";

	private static final String RELATION_TYPE_CHILD = "B";
	private static final String RELATION_TYPE_SPOUSE = "M";
	private static final String RELATION_TYPE_CUSTODY = "VF"; //custody
															  // relation
	// (child?)
	private static final String RELATION_TYPE_CUSTODY2 = "V"; //custody
	// relation ,
	// usually a
	// newborn
	// refering to
	// his parents
	// (reverse
	// custodian)
	private static final String RELATION_TYPE_FATHER = "FA";
	private static final String RELATION_TYPE_MOTHER = "MO";

	private Map userPropertiesMap;
	private Map relationsMap;
	private Map deceasedMap;
	private UserHome home;
	private AddressBusiness addressBiz;
	private MemberFamilyLogic relationBiz;
	private CommuneUserBusiness comUserBiz;
	//private CaseBusiness caseBiz;
	//private GroupHome groupHome;
	//private Group nackaGroup;
	//private Group nackaSpecialGroup;
	private ImportFile file;
	//private UserTransaction transaction;
	private UserTransaction transaction2;

	private boolean importUsers = true;
	private boolean importAddresses = true;
	private boolean importRelations = true;
	//private boolean importAddresses = false;//temp
	//private boolean importRelations = false;//temp
	private boolean fix = false;
	private boolean secretPerson = false;

	private ArrayList failedRecords;
	//private ArrayList citizenIds;

	private Gender male;
	private Gender female;

	//not needed..yet?
	/*
	 * private final String USER_SECTION_STARTS = "01001"; private final String
	 * USER_SECTION_ENDS = RELATIONAL_SECTION_STARTS; private final String
	 * IMMIGRATION_SECTION_STARTS = "05001"; private final String
	 * IMMIGRATION_SECTION_ENDS = SPECIALCASE_RELATIONAL_SECTION_STARTS;
	 */

	public NackaImportFileHandlerBean() {
	}

	//This method is syncronized because this implementation
	// is not reentrant (does not allow simultaneous invocation by many callers)
	public synchronized boolean handleRecords() {
		/** @todo temporary workaround* */
		//((NackaImportFileHandler)handler).setOnlyImportRelations(true);
		//((NackaImportFileHandler)handler).setStartRecord(52000);
		//((NackaImportFileHandler)handler).setImportRelations(false);
		// status = handler.handleRecords();

		//transaction = this.getSessionContext().getUserTransaction();
		transaction2 = this.getSessionContext().getUserTransaction();
		Timer clock = new Timer();
		clock.start();

		try {
			//Initialize the default/home commune
			homeCommune = getCommuneHome().findDefaultCommune();
			this.HOME_COMMUNE_CODE = homeCommune.getCommuneCode();

			//initialize business beans and data homes
			comUserBiz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
			relationBiz = (MemberFamilyLogic) this.getServiceInstance(MemberFamilyLogic.class);
			home = comUserBiz.getUserHome();
			addressBiz = (AddressBusiness) this.getServiceInstance(AddressBusiness.class);
			//caseBiz = (CaseBusiness)
			// this.getServiceInstance(CaseBusiness.class);

			failedRecords = new ArrayList();
			//citizenIds = new ArrayList();
			relationsMap = new HashMap();
			deceasedMap = new HashMap();

			//nackaGroup = comUserBiz.getRootCitizenGroup();
			//nackaSpecialGroup = comUserBiz.getRootSpecialCitizenGroup();
			String fixer = this.getIWApplicationContext().getApplicationSettings().getProperty(FIX_PARAMETER_NAME);

			log("NackaImportFileHandler [STARTING] time: " + IWTimestamp.getTimestampRightNow().toString());

			if ("TRUE".equals(fixer)) {
				log("NackaImportFileHandler [WARNING] FIX (run_fix) is set to TRUE");
				fix = true;

			}

			//comUserBiz.getUserHome().create();
			//groupHome = comUserBiz.getGroupHome();
			//if the transaction failes all the users and their relations are
			// removed
			//transaction.begin();

			//iterate through the records and process them
			String item;

			int count = 0;
			while (!(item = (String) file.getNextRecord()).equals("")) {
				count++;

				if (count > startRecord) {
					if (!processRecord(item))
						failedRecords.add(item);
				}

				if ((count % 250) == 0) {
					log(
						"NackaImportFileHandler processing RECORD [" + count + "] time: " + IWTimestamp.getTimestampRightNow().toString());
				}
				item = null;
			}

			clock.stop();
			log(
				"Time to handleRecords: "
					+ clock.getTime()
					+ " ms  OR "
					+ ((int) (clock.getTime() / 1000))
					+ " s. Not done yet starting with relation");

			// System.gc();
			//success commit changes
			//transaction.commit();

			//store family relations
			if (importRelations) {
				storeRelations();
			}
			if (fix) {
				//	moveNonCitizensToTestGroup();
			}

			return true;
		}
		catch (Exception ex) {
			ex.printStackTrace();

			/*
			 * try { transaction.rollback(); } catch (SystemException e) {
			 * e.printStackTrace();
			 */

			return false;
		}

	}

	private boolean processRecord(String record) throws RemoteException {
		userPropertiesMap = new HashMap();

		record = TextSoap.findAndCut(record, "#UP ");

		if (importUsers || importAddresses) {
			//Family relations
			userPropertiesMap.put(
				RELATIONAL_SECTION_STARTS,
				getArrayListWithMapsFromStringFragment(record, RELATIONAL_SECTION_STARTS, RELATIONAL_SECTION_ENDS));
			//Special case relations
			userPropertiesMap.put(
				SPECIALCASE_RELATIONAL_SECTION_STARTS,
				getArrayListWithMapsFromStringFragment(record, SPECIALCASE_RELATIONAL_SECTION_STARTS, SPECIALCASE_RELATIONAL_SECTION_ENDS));
			//Citizen info
			userPropertiesMap.put(
				CITIZEN_INFO_SECTION_STARTS,
				getArrayListWithMapsFromStringFragment(record, CITIZEN_INFO_SECTION_STARTS, CITIZEN_INFO_SECTION_ENDS));
			//Historic info
			userPropertiesMap.put(
				HISTORIC_SECTION_STARTS,
				getArrayListWithMapsFromStringFragment(record, HISTORIC_SECTION_STARTS, HISTORIC_SECTION_ENDS));

			//the rest e.g. User info and immigration stuff
			Map tmp = getPropertiesMapFromString(record, " ");
			if (tmp == null || tmp.isEmpty()) {
				return false;
			}
			userPropertiesMap.putAll(tmp);

			//log("storeUserInfo");
			storeUserInfo();
		}
		else if (!importUsers && !importAddresses && importRelations) { //only
																		// store
			// relations
			//the rest e.g. User info and immigration stuff
			userPropertiesMap.putAll(getPropertiesMapFromString(record, " ")); //PIN
			// number
			// etc.
			//Family relations
			userPropertiesMap.put(
				RELATIONAL_SECTION_STARTS,
				getArrayListWithMapsFromStringFragment(record, RELATIONAL_SECTION_STARTS, RELATIONAL_SECTION_ENDS));
		}

		/**
		 * family and other releation stuff
		 */
		if (importRelations && !secretPerson) {
			addRelations();
		}

		userPropertiesMap = null;
		record = null;

		return true;
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

	protected Map getPropertiesMapFromString(String propsString, String seperator) {
		Map map = new HashMap();
		String line = null;
		String property = null;
		String value = null;
		int index = -1;

		LineNumberReader reader = new LineNumberReader(new StringReader(propsString));

		try {

			while ((line = reader.readLine()) != null) {
				//TEMP HARDCODING ...
				if (line.indexOf("#ANTAL_POSTER") != -1) {
					return new HashMap();
				}
				//
				
				if ((index = line.indexOf(seperator)) != -1) {
					property = line.substring(0, index);
					value = line.substring(index + 1, line.length());
					map.put(property, value);
					//log("Param:"+property+" Value:"+value);
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

	protected boolean storeUserInfo() throws RemoteException {

		boolean updateName = false;
		User user = null;

		//variables
		String firstName = getUserProperty(FIRST_NAME_COLUMN, "");
		//String middleName = getUserProperty(MIDDLE_NAME_COLUMN, "");
		String middleName = "";
		String lastNameFirstPart = getUserProperty(FIRST_PART_OF_LAST_NAME_COLUMN, null); 
		String lastName = getUserProperty(LAST_NAME_COLUMN, "");
		String preferredNameIndex = getUserProperty(PREFERRED_FIRST_NAME_INDEX_COLUMN);
		String PIN = getUserProperty(PIN_COLUMN);
		
		if (lastNameFirstPart != null ) {
			if (preferredNameIndex == null) {
				preferredNameIndex = "10";
			}
			lastName = lastNameFirstPart + " " + lastName;
		}
		
		String dateOfRegistrationString = getUserProperty(REGISTRATION_DATE_COLUMN);
		IWTimestamp dateOfRegistration = null;
		if (dateOfRegistrationString != null) {
			dateOfRegistration = getDateFromString(dateOfRegistrationString);
		}

		String deactivationType = getUserProperty(DEACTIVATE_TYPE_COLUMN);
		IWTimestamp dateOfDeactivation = null;
		//boolean personHasDied = false;
		boolean isMovingFromHomeCommune = false;
		if (deactivationType != null) {
			String dateOfDeactiovationString = getUserProperty(DEACTIVATE_DATE_COLUMN);
			if (dateOfDeactiovationString != null) {
				dateOfDeactivation = getDateFromString(dateOfDeactiovationString);
			}

			// check if the deactivation concerns the current person
			if (deactivationType.endsWith(DEACTIVATION_CONCERNS_CURRENT_PERSON)) {
				//why is this person deactivated
				if (deactivationType.startsWith(DEACTIVATION_TYPE_DEATH)) {
					//personHasDied = true;
					deceasedMap.put(PIN, dateOfDeactivation);
				}
				else if (deactivationType.startsWith(DEACTIVATION_TYPE_MOVED_TO_ANOTHER_COMMUNE)) {
					isMovingFromHomeCommune = true;
				}
				else if (deactivationType.startsWith(DEACTIVATION_TYPE_MOVED_TO_ANOTHER_COUNTRY)) {
					isMovingFromHomeCommune = true;
				}
			}

		}

		String countyNumber = getUserProperty(COUNTY_CODE_COLUMN);
		String communeNumber = getUserProperty(COMMUNE_CODE_COLUMN);
		String communeCode = countyNumber+communeNumber;
		//System.out.println(firstName+" "+middleName+" "+lastName);
		//System.out.print(" ...CommuneCode = "+communeCode);
		Commune commune=null;
		try {
			commune = getCommuneHome().findByCommuneCode(communeCode);
		}
		catch (FinderException e1) {
			logWarning("Commune with code:"+communeCode+" (countyNumber+communeNumber) not found in database");
		}
		String secrecy = getUserProperty(SECRECY_MARKING_COLUMN);
		boolean secretPerson = "J".equals(secrecy);

		//TODO should not be necessary because of deactivation check
		isMovingFromHomeCommune = isMovingFromHomeCommune || !HOME_COMMUNE_CODE.equals(communeCode);

		//System.out.println(" ...PIN = "+PIN);
		if (secrecy != null && !secrecy.equals(""))
			log("SECRET PERSON = " + PIN + " Code :" + secrecy + ".");

		if (PIN == null)
			return false;

		Gender gender = getGenderFromPin(PIN);
		IWTimestamp dateOfBirth = getBirthDateFromPin(PIN);

		/**
		 * basic user info
		 */

		try {
			user = comUserBiz.createOrUpdateCitizenByPersonalID(firstName, middleName, lastName, PIN, gender, dateOfBirth);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		//preferred name handling.
		if (preferredNameIndex != null) {

			StringBuffer fullname = new StringBuffer();
			fullname.append(firstName).append(" ").append(middleName).append(" ").append(lastName);
			//log("Name : "+fullname.toString());

			if (!"10".equals(preferredNameIndex) && !"12".equals(preferredNameIndex) && !"13".equals(preferredNameIndex)) {
				String preferredName = getPreferredName(preferredNameIndex, firstName, middleName, lastName);
				if (middleName.equals("")) {
					middleName = firstName;
				}
				else {
					if (middleName.startsWith(" ")) {
						middleName = firstName + middleName;
					}
					else {
						middleName = firstName + " " + middleName;
					}

				}

				firstName = preferredName;
				middleName = TextSoap.findAndCut(middleName, preferredName);
				middleName = TextSoap.findAndReplace(middleName, "  ", " ");
				lastName = TextSoap.findAndCut(lastName, preferredName);
				lastName = TextSoap.findAndReplace(lastName, "  ", " ");

				updateName = true;

			}
			else if ("12".equals(preferredNameIndex)) {
				//stupid rule set first name as firstname AND Second name
				StringBuffer full = new StringBuffer();
				full.append(firstName).append(" ").append(middleName).append(" ").append(lastName);
				String fullName = full.toString();
				fullName = TextSoap.findAndReplace(fullName, "  ", " ");
				
				String preferredName1 = getValueAtIndexFromNameString(1, fullName);
				String preferredName2 = getValueAtIndexFromNameString(2, fullName);

				firstName = preferredName1 + " " + preferredName2;
				firstName = TextSoap.findAndReplace(firstName, "  ", " ");
				middleName = TextSoap.findAndCut(middleName, preferredName2);
				middleName = TextSoap.findAndReplace(middleName, "  ", " ");
				lastName = TextSoap.findAndCut(lastName, preferredName2);
				lastName = TextSoap.findAndReplace(lastName, "  ", " ");

				updateName = true;
			}
			else if ("13".equals(preferredNameIndex)) {
				//even stupider set first name as firstname AND third name
				StringBuffer full = new StringBuffer();
				full.append(firstName).append(" ").append(middleName).append(" ").append(lastName);
				String fullName = full.toString();
				fullName = TextSoap.findAndReplace(fullName, "  ", " ");

				String preferredName1 = getValueAtIndexFromNameString(1, fullName);
				String preferredName2 = getValueAtIndexFromNameString(3, fullName);
				
				firstName = preferredName1 + " " + preferredName2;
				firstName = TextSoap.findAndReplace(firstName, "  ", " ");

				// Remember MIDDLE NAME is always "" in the beginnig ...
				// Removing lastName since last name should only be changed when moving name to firstName
				middleName = TextSoap.findAndCut(fullName, lastName);
				middleName = TextSoap.findAndCut(middleName, preferredName1);
				middleName = TextSoap.findAndCut(middleName, preferredName2);
				middleName = TextSoap.findAndReplace(middleName, "  ", " ");

				lastName = TextSoap.findAndCut(lastName, preferredName2);
				lastName = TextSoap.findAndReplace(lastName, "  ", " ");

				updateName = true;

			}
			else if ("10".equals(preferredNameIndex))
			{
				//System.out.println("Testing preferredNameIndex = 10");
				String fullName = firstName;
				fullName = TextSoap.findAndReplace(fullName, "  ", " ");
				
				String preferredName1 = getValueAtIndexFromNameString(1, fullName);
				//System.out.println("Testing preferredName1 = "+preferredName1);
				firstName = preferredName1;

				middleName = TextSoap.findAndCut(fullName, preferredName1);
				middleName = TextSoap.findAndReplace(middleName, "  ", " ");
				//System.out.println("Testing firstName = "+firstName);
				//System.out.println("Testing middleName = "+middleName);
				//System.out.println("Testing lastName = "+lastName);
				
				updateName = true;
				
			}

			//fullname = new StringBuffer();
			//fullname.append(firstName).append("
			// ").append(middleName).append(" ").append(lastName);

			//log("Index : "+preferredNameIndex+" Modified
			// name : "+fullname.toString());

		}

		if (lastName.startsWith("Van ") && !updateName) {
			StringBuffer half = new StringBuffer();
			half.append(firstName).append(" ").append(middleName);
			String halfName = half.toString();
			firstName = getValueAtIndexFromNameString(1, halfName);
			middleName = halfName.substring(Math.min(halfName.indexOf(" ") + 1, halfName.length()), halfName.length());
			middleName = TextSoap.findAndReplace(middleName, "  ", " ");
			//lastName //unchanged

			updateName = true;
		}

		if (updateName) { //needed because createUser uses the method
						  // setFullName
			// that splits the name with it's own rules

			if (firstName != null) {
				if (firstName.endsWith(" "))
					firstName = firstName.substring(0, firstName.length() - 1);
			}

			if (middleName != null) {
				if (middleName.startsWith(" "))
					middleName = middleName.substring(1, middleName.length());
				if (middleName.endsWith(" "))
					middleName = middleName.substring(0, middleName.length() - 1);
			}

			if (lastName != null) {
				if (lastName.startsWith(" "))
					lastName = lastName.substring(1, lastName.length());
				if (lastName.endsWith(" "))
					lastName = lastName.substring(0, lastName.length() - 1);
			}

			user.setFirstName(firstName);
			user.setMiddleName(middleName);
			user.setLastName(lastName);
		}

		if (!secretPerson) {
			/**
			 * addresses
			 */
			//main address
			//country id 187 name Sweden isoabr: SE

			String addressLine = getUserProperty(ADDRESS_COLUMN);

			String foreignAddressLine1 = getUserProperty(FOREIGN_ADDRESS_1_COLUMN);

			if ((addressLine != null) && importAddresses) {
				try {

					String streetName = addressBiz.getStreetNameFromAddressString(addressLine);
					String streetNumber = addressBiz.getStreetNumberFromAddressString(addressLine);
					String postalCode = getUserProperty(POSTAL_CODE_COLUMN);
					String postalName = getUserProperty(POSTAL_NAME_COLUMN);

					Address address = comUserBiz.getUsersMainAddress(user);
					Country sweden = ((CountryHome) getIDOHome(Country.class)).findByIsoAbbreviation("SE");
					PostalCode code = addressBiz.getPostalCodeAndCreateIfDoesNotExist(postalCode, postalName, sweden);

					boolean addAddress = false; /** @todo is this necessary?* */

					if (address == null) {
						AddressHome addressHome = addressBiz.getAddressHome();
						address = addressHome.create();
						AddressType mainAddressType = addressHome.getAddressType1();
						address.setAddressType(mainAddressType);
						addAddress = true;
					}

					address.setCountry(sweden);
					address.setPostalCode(code);
					//address.setProvince("Nacka" );//set as 01 ?
					//address.setCity("Stockholm" );//set as 81?
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

			} //foreign adress
			else if (foreignAddressLine1 != null) {
				String foreignAddressLine2 = getUserProperty(FOREIGN_ADDRESS_2_COLUMN, "");
				String foreignAddressLine3 = getUserProperty(FOREIGN_ADDRESS_3_COLUMN, "");
				String foreignAddressCountry = getUserProperty(FOREIGN_ADDRESS_COUNTRY_COLUMN, "");
				final String space = " ";
				StringBuffer addressBuf = new StringBuffer();
				addressBuf
					.append(foreignAddressLine1)
					.append(space)
					.append(foreignAddressLine2)
					.append(space)
					.append(foreignAddressLine3)
					.append(space)
					.append(foreignAddressCountry);

				try {

					String streetName = TextSoap.findAndReplace(addressBuf.toString(), "  ", " ");

					Address address = comUserBiz.getUsersMainAddress(user);

					boolean addAddress = false; /** @todo is this necessary?* */

					if (address == null) {
						AddressHome addressHome = addressBiz.getAddressHome();
						address = addressHome.create();
						AddressType mainAddressType = addressHome.getAddressType1();
						address.setAddressType(mainAddressType);
						addAddress = true;
					}

					address.setStreetName(streetName);
					address.setStreetNumber("");

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

		}

		//extra address
		//special address
		//special extra address
		//previous address

		/**
		 * citizen info (commune stuff) longitude/lattitude
		 */
		//String longAndLat = getUserProperty(LONG_LAT_COLUMN);

		/**
		 * Main group relation add to the Nacka root group or move from it
		 */
		if (secretPerson) {
			user.setDescription("secret");
			//remove family ties!
			relationBiz.removeAllFamilyRelationsForUser(user);
			//remove address
			try {
				user.removeAllAddresses();
			}
			catch (IDORemoveRelationshipException e) {
				//e.printStackTrace();
			}

			if (fix) {

				//this will force a new record in the relation table
				try {
					// try to get the current user
					User currentUser;
					try {
						currentUser = IWContext.getInstance().getCurrentUser();
					}
					catch (Exception ex) {
						currentUser = null;
					}

					comUserBiz.getRootProtectedCitizenGroup().removeUser(user, currentUser);
				}
				catch (Exception e) {
				}
			}

			comUserBiz.moveCitizenToProtectedCitizenGroup(user);
		}
		else if (isMovingFromHomeCommune) {
			//		this will force a new record in the relation table
			if (fix) {

				//this will force a new record in the relation table
				try {
					// try to get the current user
					User currentUser;
					try {
						currentUser = IWContext.getInstance().getCurrentUser();
					}
					catch (Exception ex) {
						currentUser = null;
					}

					comUserBiz.getRootOtherCommuneCitizensGroup().removeUser(user, currentUser);
				}
				catch (Exception e) {
				}
			}

			if (dateOfDeactivation != null) {
				comUserBiz.moveCitizenFromCommune(user, dateOfDeactivation.getTimestamp());
			}
			else {
				comUserBiz.moveCitizenFromCommune(user);
			}

		}
		else {

			if (fix) {
				//this will force a new record in the relation table
				try {
					// try to get the current user
					User currentUser;
					try {
						currentUser = IWContext.getInstance().getCurrentUser();
					}
					catch (Exception ex) {
						currentUser = null;
					}

					comUserBiz.getRootCitizenGroup().removeUser(user, currentUser);
				}
				catch (Exception e) {
				}
			}

			if (dateOfRegistration != null) {
				comUserBiz.moveCitizenToCommune(user, dateOfRegistration.getTimestamp());
			}
			else {
				comUserBiz.moveCitizenToCommune(user);
			}

		}

		//get rid of test data
		if (fix) {

			//citizenIds.add(user.getPrimaryKey());
			/*
			 * try { user.removeAllEmails(); user.removeAllPhones(); } catch
			 * (Exception ex) { ex.printStackTrace(); }
			 */
		}
		
		/*
		if (personHasDied) {
			if (dateOfDeactivation != null) {
				comUserBiz.setUserAsDeceased(((Integer) user.getPrimaryKey()), dateOfDeactivation.getDate());
			}
			else {
				comUserBiz.setUserAsDeceased(((Integer) user.getPrimaryKey()), IWTimestamp.RightNow().getDate());
			}
		}*/

		/**
		 * Save the user to the database
		 */
		user.store();

		
		//finished with this user
		user = null;
		return true;
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
	 * Method removePreferredNameFromStringsAndReturnIt searches for the
	 * preffered name by the supplied index and removes it from the names and
	 * then returns it.
	 * 
	 * @param fullName
	 * @return the prefferedName
	 */
	private String getPreferredName(String preferredNameIndex, String firstName, String middleName, String lastName) {
		String preferredName = null;
		int index = Integer.parseInt(preferredNameIndex);
		index = index / 10;

		if (index == 1)
			return firstName; // if the first name is the preferred
		// name

		StringBuffer full = new StringBuffer();
		full.append(firstName).append(" ").append(middleName).append(" ").append(lastName);

		//log("Original name : "+full.toString());

		StringTokenizer tokens = new StringTokenizer(full.toString());
		int pos = 1;
		while (tokens.hasMoreTokens() && (pos <= index)) {
			preferredName = tokens.nextToken();
			pos++;
		}

		return preferredName;
	}

	protected void addRelations() {
		ArrayList relatives = (ArrayList) userPropertiesMap.get(RELATIONAL_SECTION_STARTS);
		relationsMap.put(getUserProperty(PIN_COLUMN), relatives);
	}

	protected void storeRelations() throws RemoteException {
		ArrayList errors = new ArrayList();
		HashtableMultivalued parentRelations = new HashtableMultivalued();
		//log("Skipping relations ... : setting relationMap == null");
		//relationsMap = null;
		log("NackaImportFileHandler [STARTING - RELATIONS] time: " + IWTimestamp.getTimestampRightNow().toString());

		//get keys <- pins
		//get user bean
		//get relative bean
		//if found link with RelationBusiness
		//else skip relative and log somewhere

		Timer clock = new Timer();
		clock.start();
		int count = 0;

		if (relationsMap != null) {
			Iterator iter = relationsMap.keySet().iterator();
			User user;
			User relative;
			String relativePIN;
			String PIN = "";
			String relationType;

			try {
				//begin transaction
				transaction2.begin();

				while (iter.hasNext()) {
					++count;
					if ((count % 250) == 0) {
						log(
							"NackaImportFileHandler storing relations ["
								+ count
								+ "] time: "
								+ IWTimestamp.getTimestampRightNow().toString());
					}

					PIN = (String) iter.next();
					user = null;

					ArrayList relatives = (ArrayList) relationsMap.get(PIN);
					if (relatives != null) {
						user = home.findByPersonalID(PIN);

						boolean secretPerson = false;
						secretPerson = "secret".equals(user.getDescription());

						if (!secretPerson) {
							Iterator iter2 = relatives.iterator();
							while (iter2.hasNext()) {
								Map relativeMap = (Map) iter2.next();
								relativePIN = (String) relativeMap.get(RELATIVE_PIN_COLUMN);
								relationType = (String) relativeMap.get(RELATIVE_TYPE_COLUMN);

								/**
								 * @todo use this second parameter if first is
								 * missing??? ask kjell
								 */
								//if( relativePIN == null ) relativePIN =
								// (String)
								// item.get("02002"));
								if (relativePIN != null) {
									try {

										if (relationType != null) {
											relative = home.findByPersonalID(relativePIN);

											secretPerson = "secret".equals(relative.getDescription());

											if (!secretPerson) {
												if (relationType.equals(this.RELATION_TYPE_CHILD)) {
													//relationBiz.setAsChildFor(relative,user);
													//for custodian check
													parentRelations.put(user.getPrimaryKey(), relative.getPrimaryKey());

												}
												else if (relationType.equals(this.RELATION_TYPE_SPOUSE)) {
													relationBiz.setAsSpouseFor(relative, user);
												}
												else if (relationType.equals(this.RELATION_TYPE_FATHER)) {
													//relationBiz.setAsChildFor(user,relative);
													parentRelations.put(relative.getPrimaryKey(), user.getPrimaryKey());
												}
												else if (relationType.equals(this.RELATION_TYPE_MOTHER)) {
													//relationBiz.setAsChildFor(user,relative);
													parentRelations.put(relative.getPrimaryKey(), user.getPrimaryKey());
												}
												else if (relationType.equals(this.RELATION_TYPE_CUSTODY)) { //custody
													relationBiz.setAsCustodianFor(user, relative);
												}
												else if (relationType.equals(this.RELATION_TYPE_CUSTODY2)) { //custody
													relationBiz.setAsCustodianFor(relative, user); //backwards
												}
											}
											else {
												errors.add(
													"NackaImporter: Error Relation type not defined for relative ( pin "
														+ relativePIN
														+ " ) of user: "
														+ PIN);
												//log("NackaImporter:
												// Error
												// Relation type not defined
												// for relative ( pin
												// "+relativePIN+" ) of user:
												// "+PIN);
											}
										}

										//other types
									}
									catch (CreateException ex) {
										errors.add("NackaImporter : Error adding relation for user: " + PIN);
										//log("NackaImporter :
										// Error adding
										// relation for user: "+PIN);
										//ex.printStackTrace();
									}
									catch (FinderException ex) {
										errors.add(
											"NackaImporter : Error relative (pin "
												+ relativePIN
												+ ") not found in database for user: "
												+ PIN);
										//log("NackaImporter :
										// Error relative
										// (pin "+relativePIN+") not found in
										// database for
										// user: "+PIN);
										//ex.printStackTrace();
									}
								} //if relativepin !=null
								else {
									errors.add("NackaImporter : Error relative has no PIN and skipping for parent user: " + PIN);
									//log("NackaImporter :
									// Error relative has
									// no PIN and skipping for parent user:
									// "+PIN);
								}
							} //end while iter2
						} //end if secret
					} //end if relative
				} //end while iter

				handleCustodyAndChildRelations(parentRelations);

				//success commit
				transaction2.commit();

				Iterator err = errors.iterator();
				while (err.hasNext()) {
					String item = (String) err.next();
					log(item);
				}

			}
			catch (Exception e) {
				if (e instanceof RemoteException) {
					throw (RemoteException) e;
				}
				else if (e instanceof FinderException) {
					log("NackaImporter : Error user (pin " + PIN + ") not found in database must be an incomplete database");
					log("NackaImporter : Rollbacking");
					try {
						transaction2.rollback();
					}
					catch (SystemException ec) {
						ec.printStackTrace();
					}
				}
				else {
					e.printStackTrace();
				}
			}
		} //end if relationmap !=null
		else {
			log("NackaImporter : No relations read");
		}

		log("NackaImportFileHandler [STARTING - DECEASED RELATIONS] (without transaction) time: " + IWTimestamp.getTimestampRightNow().toString());
		if (deceasedMap != null) {
			Iterator iter = deceasedMap.keySet().iterator();
			User user;
			IWTimestamp dateOfDeactivation = null;
			String PIN;
			try {
				while (iter.hasNext()) {
					++count;
					if ((count % 250) == 0) {
						log(
								"NackaImportFileHandler storing deceased relations ["
								+ count
								+ "] time: "
								+ IWTimestamp.getTimestampRightNow().toString());
					}
					//objects = (Object[]) iter.next();
					PIN = (String) iter.next();
					dateOfDeactivation = (IWTimestamp) deceasedMap.get(PIN);
					user = home.findByPersonalID(PIN);
					//log("NackaImportFileHandler - setUserAsDeceased ( "+user.getPrimaryKey()+" )");
					if (dateOfDeactivation != null) {
						comUserBiz.setUserAsDeceased(((Integer) user.getPrimaryKey()), dateOfDeactivation.getDate());
					}
					else {
						comUserBiz.setUserAsDeceased(((Integer) user.getPrimaryKey()), IWTimestamp.RightNow().getDate());
					}
				}
			} catch (FinderException e) {
				e.printStackTrace();
			}
		}		else {
			log("NackaImporter : No deceased relations read");
		}
			
			
		clock.stop();
		log("Time to store relations: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000)) + " s");

	}

	protected String getUserProperty(String propertyName) {
		return (String) userPropertiesMap.get(propertyName);
	}

	protected String getUserProperty(String propertyName, String StringToReturnIfNotSet) {
		String value = getUserProperty(propertyName);
		if (value == null)
			value = StringToReturnIfNotSet;
		return value;
	}

	public void setImportFile(ImportFile file) {
		this.file = file;
	}

	public void setOnlyImportRelations(boolean onlyImportRelations) {
		setImportRelations(true);
		setImportUsers(false);
		setImportAddresses(false);
	}

	public void setImportRelations(boolean importRelations) {
		this.importRelations = importRelations;
	}

	public void setImportUsers(boolean importUsers) {
		this.importUsers = importUsers;
	}

	public void setImportAddresses(boolean importAddresses) {
		this.importAddresses = importAddresses;
	}

	public void setStartRecord(int startRecord) {
		this.startRecord = startRecord;
	}

	private Gender getGenderFromPin(String pin) {
		//pin format = 190010221208 second last number is the gender
		//even number = female
		//odd number = male
		try {
			GenderHome home = (GenderHome) this.getIDOHome(Gender.class);
			if (Integer.parseInt(pin.substring(10, 11)) % 2 == 0) {
				if (female == null) {
					female = home.getFemaleGender();
				}
				return female;
			}
			else {
				if (male == null) {
					male = home.getMaleGender();
				}
				return male;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null; //if something happened
		}
	}

	private IWTimestamp getBirthDateFromPin(String pin) {
		//pin format = 190010221208 yyyymmddxxxx
		int dd = Integer.parseInt(pin.substring(6, 8));
		int mm = Integer.parseInt(pin.substring(4, 6));
		int yyyy = Integer.parseInt(pin.substring(0, 4));
		IWTimestamp dob = new IWTimestamp(dd, mm, yyyy);
		return dob;
	}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#setRootGroup(Group)
	 */
	public void setRootGroup(Group group) {
	}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
	 */
	public List getFailedRecords() {
		return failedRecords;
	}

	/*
	 * Commented out since it is never used... private Group
	 * getCitizenTestGroup() {
	 * 
	 * Group rootGroup = null;
	 * 
	 * try{
	 * 
	 * //create the default group final IWApplicationContext iwc =
	 * getIWApplicationContext(); final IWMainApplicationSettings settings =
	 * iwc.getApplicationSettings(); String groupId = (String)
	 * settings.getProperty(TEST_GROUP_ID_PARAMETER_NAME); if (groupId != null) {
	 * final GroupHome groupHome = comUserBiz.getGroupHome(); rootGroup =
	 * groupHome.findByPrimaryKey(new Integer(groupId)); } else {
	 * logError("trying to store Citizen Test group"); //@todo this
	 * seems a wrong way to do things final GroupTypeHome typeHome =
	 * (GroupTypeHome) getIDOHome(GroupType.class); final GroupType type =
	 * typeHome.create(); final GroupBusiness groupBusiness =
	 * comUserBiz.getGroupBusiness(); rootGroup = groupBusiness.createGroup(
	 * "Citizen Test", "The Citizen Test Group.",
	 * type.getGeneralGroupTypeString());
	 * settings.setProperty(TEST_GROUP_ID_PARAMETER_NAME, (Integer)
	 * rootGroup.getPrimaryKey()); } } catch(Exception e){ e.printStackTrace(); }
	 * 
	 * return rootGroup; }
	 */

	/*
	 * Commented out since it is never used... private void
	 * moveNonCitizensToTestGroup(){ //breyta grouprelationidinu i id test
	 * gruppunnar
	 * 
	 * //gera frekar aukadalk merkja tha sem eru inni Group testGroup =
	 * getCitizenTestGroup();
	 * 
	 * if(testGroup!=null && !citizenIds.isEmpty()){
	 * 
	 * String testGroupId = ((Integer)testGroup.getPrimaryKey()).toString();
	 * StringBuffer sql = new StringBuffer(); sql.append("update table
	 * ic_group_relation"). append(" set IC_GROUP_ID =").append(testGroupId).
	 * append(" where RELATED_IC_GROUP_ID not in (").
	 * append(IDOUtil.getInstance().convertListToCommaseparatedString(citizenIds)).
	 * append(") and IC_GROUP_ID=").
	 * append(nackaGroup.getPrimaryKey().toString());
	 * 
	 * Connection conn= null; Statement stmt= null;
	 * 
	 * javax.transaction.TransactionManager t =
	 * com.idega.transaction.IdegaTransactionManager.getInstance();
	 * 
	 * try { t.begin();
	 * 
	 * conn = ConnectionBroker.getConnection(); stmt = conn.createStatement();
	 * ResultSet RS = stmt.executeQuery(sql.toString());
	 * 
	 * RS.close(); stmt.close();
	 * 
	 * t.commit(); //t.rollback(); } catch(Exception e) { try { t.rollback(); }
	 * catch(javax.transaction.SystemException ex) { ex.printStackTrace(); }
	 * e.printStackTrace(); } finally{ if(stmt != null){ try { stmt.close(); }
	 * catch (SQLException e) { e.printStackTrace(); } } if (conn != null){
	 * ConnectionBroker.freeConnection(conn); } }
	 * 
	 * 
	 * }else{ System.err.println("NackaImporter: Test Group is NULL!"); }
	 * 
	 *  
	 */

	private String getValueAtIndexFromNameString(int index, String name) {
		int i = 1;
		StringTokenizer tokens = new StringTokenizer(name);
		String value = null;
		while (tokens.hasMoreTokens() && i <= index) {
			value = tokens.nextToken();
			i++;
		}

		return value;
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

						User parent = comUserBiz.getUser(parentId);
						User child = comUserBiz.getUser(childId);

						//Collection custodians =
						// parent.getRelatedBy(relationBiz.getCustodianRelationType()
						// );

						if (coll == null || coll.isEmpty()) {
							//and as custodian
							parent.addUniqueRelation(
								((Integer) (this.convertUserToGroup(child).getPrimaryKey())).intValue(),
								relationBiz.getCustodianRelationType());
						}

						relationBiz.setAsParentFor(parent, child);

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

		}
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
	
	protected CommuneHome  getCommuneHome(){
		if(communeHome==null){
			try {
				communeHome=(CommuneHome)getIDOHome(Commune.class);
			}
			catch (RemoteException e) {
				log(e);
			}
		}
		return communeHome;
	}
}