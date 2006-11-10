package se.idega.idegaweb.commune.block.importer.business;

/**
 * http://skatteverket.se/folkbokforing/navet/tekniskbeskrivning.html#73
 * 
 */

public class SKVConstants {

	protected static final String COLUMN_PIN = "01001";

	protected static final String COLUMN_SECRECY = "01003";

	protected static final String COLUMN_REFERENCE_PIN = "01005";

	protected static final String COLUMN_DEACTIVATION_CODE = "01006";
	
	protected static final String COLUMN_DEACTIVATION_DATE = "01007";

	protected static final String COLUMN_PREFERRED_FIRST_NAME_INDEX = "01011";

	protected static final String COLUMN_FIRST_NAME = "01012";

	protected static final String COLUMN_FIRST_PART_OF_LAST_NAME = "01013";

	protected static final String COLUMN_LAST_NAME = "01014";

	protected static final String COLUMN_DISPLAY_NAME = "01015";

	protected static final String COLUMN_REGISTRATION_DATE = "01021";

	protected static final String COLUMN_COUNTY_CODE = "01022";

	protected static final String COLUMN_COMMUNE_CODE = "01023";

	protected static final String COLUMN_ADDRESS_COORDINATE = "01025";

	protected static final String COLUMN_CO_ADDRESS = "01031";

	protected static final String COLUMN_ADDRESS1 = "01032";

	protected static final String COLUMN_ADDRESS2 = "01033";

	protected static final String COLUMN_POSTAL_CODE = "01034";

	protected static final String COLUMN_POSTAL_NAME = "01035";

	protected static final String COLUMN_EXTRA_CO_ADDRESS = "01051";

	protected static final String COLUMN_EXTRA_ADDRESS1 = "01052";

	protected static final String COLUMN_EXTRA_ADDRESS2 = "01053";

	protected static final String COLUMN_EXTRA_POSTAL_CODE = "01054";

	protected static final String COLUMN_EXTRA_POSTAL_NAME = "01055";

	protected static final String COLUMN_FOREIGN_ADDRESS1 = "01071";

	protected static final String COLUMN_FOREIGN_ADDRESS2 = "01072";

	protected static final String COLUMN_FOREIGN_ADDRESS3 = "01073";

	protected static final String COLUMN_FOREIGN_ADDRESS_COUNTRY = "01077";

	protected static final String COLUMN_CIVIL_STATUS_CODE = "01081";

	protected static final String COLUMN_CIVIL_STATUS_DATE = "01082";

	protected static final String COLUMN_BIRTH_COUNTY = "01091";

	protected static final String COLUMN_BIRTH_PARISH = "01092";

	protected static final String COLUMN_FOREIGN_BIRTH_CITY = "01093";

	protected static final String COLUMN_FOREIGN_BIRTH_COUNTRY = "01094";

	protected static final String COLUMN_IMMIGRATION_DATE = "01101";

	protected static final String COLUMN_RELATIONAL_SECTION_STARTS = "02000";

	protected static final String COLUMN_RELATIVE_PIN = "02001";

	protected static final String COLUMN_RELATIVE_ALTERNATIVE_PIN = "02002";

	protected static final String COLUMN_RELATIVE_TYPE = "02003";

	protected static final String COLUMN_RELATIVE_FIRST_NAME = "02005";

	protected static final String COLUMN_RELATIVE_MIDDLE_NAME = "02006";

	protected static final String COLUMN_RELATIVE_LAST_NAME = "02007";

	protected static final String COLUMN_RELATIONAL_SECTION_ENDS = "02999";

	protected static final String COLUMN_CITIZEN_INFO_SECTION_STARTS = "03000";

	protected static final String COLUMN_CITIZEN_INFO_CITIZENSHIP_CODE = "03001";

	protected static final String COLUMN_CITIZEN_INFO_CITIZENSHIP_DATE = "03002";

	protected static final String COLUMN_CITIZEN_INFO_SECTION_ENDS = "03999";

	protected static final String RELATION_TYPE_CHILD = "B";

	protected static final String RELATION_TYPE_MOTHER = "MO";

	protected static final String RELATION_TYPE_FATHER = "FA";

	protected static final String RELATION_TYPE_CUSTODIAN1 = "V"; // custody

	protected static final String RELATION_TYPE_CUSTODIAN2 = "VF"; // custody

	protected static final String RELATION_TYPE_SPOUSE = "M";

	protected static final String RELATION_TYPE_PARTNER = "P";

	protected static final String DEACTIVATION_CODE_EMIGRATED = "UV";

	protected static final String DEACTIVATION_CODE_DEATH = "AV";

	protected static final String DEACTIVATION_CODE_OLD_PIN = "GN";

	protected static final String DEACTIVATION_CODE_OTHER = "AN";
	
	protected static final String DEACTIVATION_CODE_OTHER2 = "OB";

	protected static final String DEACTIVATION_CODE_OTHER3 = "TA";

	protected static final String DEACTIVATION_CODE_OTHER4 = "AS";

	protected static final String ENTRY_START = "#UP ";
}