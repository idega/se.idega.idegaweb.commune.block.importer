package se.idega.idegaweb.commune.block.importer.business;

import java.util.StringTokenizer;
import com.idega.block.importer.business.ImportBusinessBean;
import com.idega.data.IDOStoreException;
import com.idega.user.data.User;
import com.idega.util.text.TextSoap;


/**
 * @author gimmi
 */
public class NackaImportBusinessBean extends ImportBusinessBean implements NackaImportBusiness {
	/**
	 * @param updateName
	 * @param user
	 */
	public User handleNames(User user, String firstName, String middleName, String lastName, String preferredNameIndex, boolean store) {
		boolean updateName = false;
		
		if (firstName == null || firstName.trim().equals("")) {
			if (user.getFirstName() != null) {
				firstName = user.getFirstName();
			} else {
				firstName = "";
			}
		}
		// Setting middleName as "", required for the rest of the code
		if (middleName != null && !middleName.equals("")) {
			firstName = firstName + " " + middleName;
			middleName = "";
		} else {
			middleName = "";
		}
		
		if (lastName == null || lastName.trim().equals("")) {
			if (user.getLastName() != null) {
				lastName = user.getLastName();
			} else {
				lastName = "";
			}
		}
		
		if (preferredNameIndex == null) {
			preferredNameIndex = "10";
		}
		
		StringBuffer fullname = new StringBuffer();
		//preferred name handling.
		if (preferredNameIndex != null) {

			fullname.append(firstName).append(" ").append(middleName).append(" ").append(lastName);
			//log("Name : "+fullname.toString());

			//if (!"10".equals(preferredNameIndex) && !"12".equals(preferredNameIndex) && !"13".equals(preferredNameIndex)) {
			int index = Integer.parseInt(preferredNameIndex);
			int refName1 = index / 10;
			int refName2 = index % 10;
			
				if (refName2 > 0) {
					//StringBuffer full = new StringBuffer();
					//full.append(firstName).append(" ").append(middleName).append(" ").append(lastName);
					String fullName = fullname.toString();
					
					String preferredName1 = getValueAtIndexFromNameString(refName1, fullName);
					String preferredName2 = getValueAtIndexFromNameString(refName2, fullName);
					
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
				} else if (refName1 > 0){
					String fullName = fullname.toString();
					
					String preferredName = getValueAtIndexFromNameString(refName1, fullName);
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
					if (refName1 > 1 && !lastName.equals(preferredName)) { // !lastName.equals(preferredName) added so that last_name is not null, if preferred name = last_name
						lastName = TextSoap.findAndCut(lastName, preferredName);
						lastName = TextSoap.findAndReplace(lastName, "  ", " ");
					}
	
					updateName = true;
				}
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

		if (store) {
			try {
				user.store();
			} catch (IDOStoreException e) {
				throw e;
			}
		}
		return user;
	}

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

}
