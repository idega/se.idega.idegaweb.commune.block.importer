/*
 * Created on 30.6.2004
 *
 * Copyright (C) 2004 Idega hf. All Rights Reserved.
 *
 *  This software is the proprietary information of Idega hf.
 *  Use is subject to license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

/**
 * @author aron
 *
 * ImportFileFieldConstants TODO Describe this type
 */
public class ImportFileFieldConstants {

	/* URL to the properties
	 * http://skatteverket.se/folkbokforing/navet/tekniskbeskrivning.html#73
	 */
	
	protected static final String ACTION_CONCERNS_CURRENT_PERSON =				"H";
	protected static final String ACTION_CONCERNS_RELATIVE = 							"R";
	protected static final String ACTION_PREFIX_CANCEL = 									"3";
	protected static final String ACTION_TYPE_BIRTH =											"6";
	protected static final String ACTION_TYPE_CHANGE_ADDRESS =						"75";
	protected static final String ACTION_TYPE_CITIZENSHIP =								"36";
	protected static final String ACTION_TYPE_COLUMN = 										"01004";
	protected static final String ACTION_TYPE_CUSTODY_RELATIONS =					"12";
	protected static final String ACTION_TYPE_DEATH =											"66";
	protected static final String ACTION_TYPE_DIVORCE =										"59";
	protected static final String ACTION_TYPE_LAST_NAME =									"21";
	protected static final String ACTION_TYPE_MARRIAGE =									"56";
	protected static final String ACTION_TYPE_MIDDLE_NAME=								"25";
	protected static final String ACTION_TYPE_MOVED =											"41";
	protected static final String ACTION_TYPE_MOVED_TO_ANOTHER_COUNTRY =	"43";
	protected static final String ACTION_TYPE_NEW_PERSONAL_ID =						"1";
	protected static final String ACTION_TYPE_SECRET = 										"3";
	protected static final String ACTION_TYPE_SPECIAL_CO_ADDRESS = 				"45";
	protected static final String ACTION_TYPE_SPECIAL_FIRST_NAME =				"29";

	protected static final String ADDRESS_KEY_CODE = 											 "01025";
	protected static final String ADDRESS_COLUMN = 											"01033";
	protected static final String CITIZEN_INFO_SECTION_ENDS = 						"03999";
	protected static final String CITIZEN_INFO_SECTION_STARTS = 					"03000";
	//protected static final String CO_ADDRESS_NAME_COLUMN = 								"01051";
	protected static final String CO_ADDRESS_COLUMN = 										"01052";
	protected static final String CO_POSTAL_CODE_COLUMN = 								"01053";
	protected static final String CO_POSTAL_NAME_COLUMN = 								"01054";
	protected static final String COMMUNE_CODE_COLUMN = 									"01023";
	protected static final String COUNTY_CODE_COLUMN = 										"01022";
	//protected static final String DEACTIVATE_REASON_COLUMN = "01006";
	protected static final String DEACTIVATION_DATE_COLUMN = 							"01007";
	protected static final String EMPTY_FIELD_CHARACTER ="$";
	protected static final String FIRST_NAME_COLUMN = 										"01012";
	protected static final String FIRST_PART_OF_LAST_NAME_COLUMN = 				"01013";
	//protected static final String TEST_GROUP_ID_PARAMETER_NAME =
	// "citizen_test_group_id";
	
	protected static final String FOREIGN_ADDRESS_1_COLUMN = 							"01071";
	protected static final String FOREIGN_ADDRESS_2_COLUMN = 							"01072";
	protected static final String FOREIGN_ADDRESS_3_COLUMN = 							"01073";
	protected static final String FOREIGN_ADDRESS_COUNTRY_COLUMN = 				"01077";
	protected static final String HISTORIC_SECTION_ENDS = 								"04999";
	protected static final String HISTORIC_SECTION_STARTS = 							"04000";
	protected static final String LAST_NAME_COLUMN = 											"01014";
	protected static final String PIN_COLUMN =														"01001";
	protected static final String POSTAL_CODE_COLUMN = 										"01034";
	protected static final String POSTAL_NAME_COLUMN = 										"01035";
	protected static final String PREFERRED_FIRST_NAME_INDEX_COLUMN = 		"01011";
	protected static final String REFERENCE_PIN_COLUMN = 									"01005";
	protected static final String REGISTRATION_DATE_COLUMN = 							"01021";
	//protected static final String RELATION_STATUS_NEW = "NY";
	protected static final String RELATION_STATUS_CANCELLED = "AS";
	//protected static final String ACTION_PREFIX_BIRTH_CANCEL = "5";

	protected static final String RELATION_TYPE_CHILD = "B";
	protected static final String RELATION_TYPE_CUSTODY = "VF"; //custody
	protected static final String RELATION_TYPE_CUSTODY2 = "V"; //custody
	protected static final String RELATION_TYPE_FATHER = "FA";
	protected static final String RELATION_TYPE_MOTHER = "MO";
	protected static final String RELATION_TYPE_SPOUSE = "M";
	protected static final String RELATIONAL_SECTION_ENDS = 							"02999";
	protected static final String RELATIONAL_SECTION_STARTS = 						"02000";
	protected static final String RELATIVE_PIN_COLUMN = 									"02001";
	protected static final String RELATIVE_STATUS_COLUMN = 								"02008";
	protected static final String RELATIVE_TYPE_COLUMN = 									"02003";
	protected static final String SECRECY_MARKING_COLUMN = 								"01003";
	protected static final String SPECIALCASE_RELATIONAL_SECTION_ENDS = 	"06999";
	protected static final String SPECIALCASE_RELATIONAL_SECTION_STARTS =	"06000";

}
