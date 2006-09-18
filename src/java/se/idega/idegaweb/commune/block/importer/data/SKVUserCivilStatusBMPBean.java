package se.idega.idegaweb.commune.block.importer.data;

import javax.ejb.FinderException;

import com.idega.data.GenericEntity;
import com.idega.data.IDOLookup;
import com.idega.data.IDOQuery;

public class SKVUserCivilStatusBMPBean extends GenericEntity implements
		SKVUserCivilStatus {

	protected final static String ENTITY_NAME = "skv_user_civil_status";

	protected final static String COLUMN_STATUS_CODE = "status_code";

	protected final static String COLUMN_LOCALIZED_KEY = "loc_key";

	public String getEntityName() {
		return ENTITY_NAME;
	}

	public void initializeAttributes() {
		addAttribute(getIDColumnName());
		addAttribute(COLUMN_STATUS_CODE, "Status code", String.class);
		addAttribute(COLUMN_LOCALIZED_KEY, "Localized key", String.class);
	}
	
	public void insertStartData () throws Exception {
		super.insertStartData ();

		SKVUserCivilStatusHome home
				= (SKVUserCivilStatusHome) IDOLookup.getHome(SKVUserCivilStatus.class);
		final String [] data = { "OG", "G", "S", "RP", "SP", "EP" };
		final String [] loc = { "unmarried", "married", "divorced", "registered_partner", "divorced_partner", "surviving_partner" };
		for (int i = 0; i < data.length; i++) {
			SKVUserCivilStatus status = home.create();
			status.setStatusCode(data[i]);
			status.setLocalizedKey(ENTITY_NAME + "." + loc[i]);
			status.store();
		}
	}
	
	//Setters
	public void setStatusCode(String code) {
		setColumn(COLUMN_STATUS_CODE, code);
	}
	
	public void setLocalizedKey(String key) {
		setColumn(COLUMN_LOCALIZED_KEY, key);
	}
	
	//Getters
	public String getStatusCode() {
		return getStringColumnValue(COLUMN_STATUS_CODE);
	}
	
	public String getLocalizedKey() {
		return getStringColumnValue(COLUMN_LOCALIZED_KEY);
	}
	
	//ejb
	public Object ejbFindByStatusCode(String code) throws FinderException {
		IDOQuery query = this.idoQueryGetSelect();
		query.appendWhereEquals(COLUMN_STATUS_CODE, code);

		return idoFindOnePKByQuery(query);
	}
}