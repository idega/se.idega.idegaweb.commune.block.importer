package se.idega.idegaweb.commune.block.importer.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SKVEntryHolder {
	
	private Map propertiesMap = null;
	
	private List relatives = null;
	
	public boolean isEmpty() {
		if (propertiesMap == null) {
			return true;
		}
		
		return propertiesMap.isEmpty();
	}
	
	public String getAddress1() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_ADDRESS1);
	}

	public String getAddress2() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_ADDRESS2);
	}

	public String getAddressCoordinate() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_ADDRESS_COORDINATE);
	}

	public String getBirthCounty() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_BIRTH_COUNTY);
	}

	public String getBirthParish() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_BIRTH_PARISH);
	}

	public String getCitizenshipCode() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_CITIZEN_INFO_CITIZENSHIP_CODE);
	}

	public String getCitizenshipDate() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_CITIZEN_INFO_CITIZENSHIP_DATE);
	}

	public String getCivilStatusCode() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_CIVIL_STATUS_CODE);
	}

	public String getCivilStatusDate() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_CIVIL_STATUS_DATE);
	}

	public String getCoAddress() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_CO_ADDRESS);
	}

	public String getCommuneCode() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_COMMUNE_CODE);
	}

	public String getCountyCode() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_COUNTY_CODE);
	}

	public String getDeactivationCode() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_DEACTIVATION_CODE);
	}

	public String getDeactivationDate() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_DEACTIVATION_DATE);
	}

	public String getDisplayName() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_DISPLAY_NAME);
	}

	public String getExtraAddress1() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_EXTRA_ADDRESS1);
	}

	public String getExtraAddress2() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_EXTRA_ADDRESS2);
	}

	public String getExtraCoAddress() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_EXTRA_CO_ADDRESS);
	}

	public String getExtraPostalCode() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_EXTRA_POSTAL_CODE);
	}

	public String getExtraPostalName() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_EXTRA_POSTAL_NAME);
	}

	public String getFirstName() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FIRST_NAME);
	}

	public String getFirstPartOfLastName() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FIRST_PART_OF_LAST_NAME);
	}

	public String getForeignAddress1() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FOREIGN_ADDRESS1);
	}

	public String getForeignAddress2() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FOREIGN_ADDRESS2);
	}

	public String getForeignAddress3() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FOREIGN_ADDRESS3);
	}

	public String getForeignAddressCountry() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FOREIGN_ADDRESS_COUNTRY);
	}

	public String getForeignBirthCity() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FOREIGN_BIRTH_CITY);
	}

	public String getForeignBirthCountry() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_FOREIGN_BIRTH_COUNTRY);
	}

	public String getImmigrationDate() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_IMMIGRATION_DATE);
	}

	public String getLastName() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_LAST_NAME);
	}

	public String getPin() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_PIN);
	}

	public String getPostalCode() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_POSTAL_CODE);
	}

	public String getPostalName() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_POSTAL_NAME);
	}

	public String getPreferredFirstNameIndex() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_PREFERRED_FIRST_NAME_INDEX);
	}

	public String getReferencePin() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_REFERENCE_PIN);
	}

	public String getRegistrationDate() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_REGISTRATION_DATE);
	}

	public String getSecrecy() {
		return (String) propertiesMap.get(SKVConstants.COLUMN_SECRECY);
	}
	
	public List getRelatives() {
		return relatives;
	}
	
	public void setAttribute(String key, String value) {
		if (propertiesMap == null) {
			propertiesMap = new HashMap();
		}
		
		propertiesMap.put(key, value);
	}
	
	public SKVRelativeEntryHolder getNewRelativeEntryHolder() {
		return new SKVRelativeEntryHolder();
	}
	
	public void addRelative(SKVRelativeEntryHolder relative) {
		if (relatives == null) {
			relatives = new ArrayList();
		}

		relatives.add(relative);
	}
	
	public class SKVRelativeEntryHolder {
		
		public SKVRelativeEntryHolder() {
			
		}
		
		private String relativePin = null;
		
		private String relativeAlternativePin = null;
		
		private String relativeType = null;
		
		private String relativeFirstName = null;
		
		private String relativeMiddleName = null;
		
		private String relativeLastName = null;
		
		private String relativeDeactivationCode = null;
		
		private String relativeDeactivationDate = null;
		
		public SKVRelativeEntryHolder(String pin, String alternativePin, String type, String firstName, String middleName, String lastName, String deactivationCode, String deactivationDate) {
			relativePin = pin;
			relativeAlternativePin = alternativePin;
			relativeType = type;
			relativeFirstName = firstName;
			relativeMiddleName = middleName;
			relativeLastName = lastName;
			relativeDeactivationCode = deactivationCode;
			relativeDeactivationDate = deactivationDate;
		}

		public String getRelativeAlternativePin() {
			return relativeAlternativePin;
		}

		public String getRelativeFirstName() {
			return relativeFirstName;
		}

		public String getRelativeLastName() {
			return relativeLastName;
		}

		public String getRelativeMiddleName() {
			return relativeMiddleName;
		}

		public String getRelativePin() {
			return relativePin;
		}

		public String getRelativeType() {
			return relativeType;
		}
		
		public String getRelativeDeactivationCode() {
			return relativeDeactivationCode;
		}
		
		public String getRelativeDeactivationDate() {
			return relativeDeactivationDate;
		}

		public void setRelativeAlternativePin(String relativeAlternativePin) {
			this.relativeAlternativePin = relativeAlternativePin;
		}

		public void setRelativeFirstName(String relativeFirstName) {
			this.relativeFirstName = relativeFirstName;
		}

		public void setRelativeLastName(String relativeLastName) {
			this.relativeLastName = relativeLastName;
		}

		public void setRelativeMiddleName(String relativeMiddleName) {
			this.relativeMiddleName = relativeMiddleName;
		}

		public void setRelativePin(String relativePin) {
			this.relativePin = relativePin;
		}

		public void setRelativeType(String relativeType) {
			this.relativeType = relativeType;
		}
		
		public void setRelativeDeactivationCode(String relativeDeactivationCode) {
			this.relativeDeactivationCode = relativeDeactivationCode;
		}
		
		public void setRelativeDeactivationDate(String relativeDeactivationDate) {
			this.relativeDeactivationDate = relativeDeactivationDate;
		}
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("pin = ");
		buffer.append(this.getPin());
		buffer.append("\n");
		buffer.append("secrecy = ");		
		buffer.append(this.getSecrecy());
		buffer.append("\n");
		buffer.append("reference pin = ");
		buffer.append(this.getReferencePin());
		buffer.append("\n");
		buffer.append("deactivation code = ");
		buffer.append(this.getDeactivationCode());
		buffer.append("\n");
		buffer.append("preferred first name index = ");
		buffer.append(this.getPreferredFirstNameIndex());
		buffer.append("\n");
		buffer.append("first name = ");
		buffer.append(this.getFirstName());
		buffer.append("\n");
		buffer.append("middle name = ");
		buffer.append(this.getFirstPartOfLastName());
		buffer.append("\n");
		buffer.append("last name = ");
		buffer.append(this.getLastName());
		buffer.append("\n");
		buffer.append("display name = ");
		buffer.append(this.getDisplayName());
		buffer.append("\n");
		buffer.append("registration date = ");
		buffer.append(this.getRegistrationDate());
		buffer.append("\n");
		buffer.append("county code = ");
		buffer.append(this.getCountyCode());
		buffer.append("\n");
		buffer.append("commune code = ");
		buffer.append(this.getCommuneCode());
		buffer.append("\n");
		buffer.append("address coordinate = ");
		buffer.append(this.getAddressCoordinate());
		buffer.append("\n");
		buffer.append("co address = ");
		buffer.append(this.getCoAddress());
		buffer.append("\n");
		buffer.append("address 1 = ");
		buffer.append(this.getAddress1());
		buffer.append("\n");
		buffer.append("address 2 = ");
		buffer.append(this.getAddress2());
		buffer.append("\n");
		buffer.append("postal code = ");
		buffer.append(this.getPostalCode());
		buffer.append("\n");
		buffer.append("postal name = ");
		buffer.append(this.getPostalName());
		buffer.append("\n");
		buffer.append("extra co address = ");
		buffer.append(this.getExtraCoAddress());
		buffer.append("\n");
		buffer.append("extra address 1 = ");
		buffer.append(this.getExtraAddress1());
		buffer.append("\n");
		buffer.append("extra address 2 = ");
		buffer.append(this.getExtraAddress2());
		buffer.append("\n");
		buffer.append("extra postal code = ");
		buffer.append(this.getExtraPostalCode());
		buffer.append("\n");
		buffer.append("extra postal name = ");
		buffer.append(this.getExtraPostalName());
		buffer.append("\n");
		buffer.append("foreign address 1 = ");
		buffer.append(this.getForeignAddress1());
		buffer.append("\n");
		buffer.append("foreign address 2 = ");
		buffer.append(this.getForeignAddress2());
		buffer.append("\n");
		buffer.append("foreign address 3 = ");
		buffer.append(this.getForeignAddress3());
		buffer.append("\n");
		buffer.append("foreign address country = ");
		buffer.append(this.getForeignAddressCountry());
		buffer.append("\n");
		buffer.append("civil status code = ");
		buffer.append(this.getCivilStatusCode());
		buffer.append("\n");
		buffer.append("civil status date = ");
		buffer.append(this.getCivilStatusDate());
		buffer.append("\n");
		buffer.append("birth county = ");
		buffer.append(this.getBirthCounty());
		buffer.append("\n");
		buffer.append("birth parish = ");
		buffer.append(this.getBirthParish());
		buffer.append("\n");
		buffer.append("foreign birth city = ");
		buffer.append(this.getForeignBirthCity());
		buffer.append("\n");
		buffer.append("foreign birth country = ");
		buffer.append(this.getForeignBirthCountry());
		buffer.append("\n");
		buffer.append("immigration date = ");
		buffer.append(this.getImmigrationDate());
		buffer.append("\n");
		buffer.append("citizenship code = ");
		buffer.append(this.getCitizenshipCode());
		buffer.append("\n");
		buffer.append("citizenship date = ");
		buffer.append(this.getCitizenshipDate());	
		buffer.append("\n");
		
		buffer.append("number of relatives = ");
		if (getRelatives() != null) {
			buffer.append(getRelatives().size());
			buffer.append("\n");
			Iterator it = getRelatives().iterator();
			while (it.hasNext()) {
				SKVRelativeEntryHolder holder = (SKVRelativeEntryHolder) it.next();
				buffer.append("pin = ");
				buffer.append(holder.getRelativePin());					
				buffer.append("\n");
				buffer.append("alternative pin = ");
				buffer.append(holder.getRelativeAlternativePin());					
				buffer.append("\n");
				buffer.append("first name = ");
				buffer.append(holder.getRelativeFirstName());					
				buffer.append("\n");
				buffer.append("middle name = ");
				buffer.append(holder.getRelativeMiddleName());					
				buffer.append("\n");
				buffer.append("last name = ");
				buffer.append(holder.getRelativeLastName());					
				buffer.append("\n");
				buffer.append("type = ");
				buffer.append(holder.getRelativeType());					
				buffer.append("\n");
			}
		} else {
			buffer.append("0");
		}
		
		return buffer.toString();
	}
}