/*
 * $Id: PlacementImportDateBMPBean.java,v 1.4 2004/04/15 11:48:56 anders Exp $
 *
 * Copyright (C) 2003 Agura IT. All Rights Reserved.
 *
 * This software is the proprietary information of Agura IT AB.
 * Use is subject to license terms.
 *
 */
package se.idega.idegaweb.commune.block.importer.data;

import java.sql.Date;

import com.idega.block.school.data.SchoolClassMember;
import com.idega.data.GenericEntity;

/**
 * Entity bean holding import date values for student placements.
 * <p>
 * Last modified: $Date: 2004/04/15 11:48:56 $ by $Author: anders $
 *
 * @author Anders Lindman
 * @version $Revision: 1.4 $
 */
public class PlacementImportDateBMPBean extends GenericEntity implements PlacementImportDate {

	private static final String ENTITY_NAME = "comm_placement_imp_date";

	private static final String COLUMN_SCH_CLASS_MEMBER_ID = "sch_class_member_id";
	private static final String COLUMN_IMPORT_DATE = "import_date";
		
	/**
	 * @see com.idega.data.GenericEntity#getEntityName()
	 */
	public String getEntityName() {
		return ENTITY_NAME;
	}
	
	/**
	 * @see com.idega.data.GenericEntity#getIdColumnName()
	 */
	public String getIDColumnName() {
		return COLUMN_SCH_CLASS_MEMBER_ID;
	}

	/**
	 * @see com.idega.data.GenericEntity#initializeAttributes()
	 */
	public void initializeAttributes() {
		addOneToOneRelationship(getIDColumnName(), SchoolClassMember.class);
		setAsPrimaryKey (getIDColumnName(), true);

		addAttribute(COLUMN_IMPORT_DATE, "Import date", true, true, Date.class);
	}
	
	public SchoolClassMember getSchoolClassMember() {
		return (SchoolClassMember) getColumnValue(COLUMN_SCH_CLASS_MEMBER_ID);	
	}

	public int getSchoolClassMemberId() {
		return getIntColumnValue(COLUMN_SCH_CLASS_MEMBER_ID);	
	}
	
	public Date getImportDate() {
		return (Date) getColumnValue(COLUMN_IMPORT_DATE);	
	}

	public void setSchoolClassMemberId(int id) { 
		setColumn(COLUMN_SCH_CLASS_MEMBER_ID, id); 
	}

	public void setImportDate(Date date) { 
		setColumn(COLUMN_IMPORT_DATE, date); 
	}
}
