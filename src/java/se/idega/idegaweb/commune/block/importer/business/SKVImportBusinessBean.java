package se.idega.idegaweb.commune.block.importer.business;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.idega.block.importer.business.ImportBusinessBean;
import com.idega.data.IDOStoreException;
import com.idega.user.data.User;

/**
 * @author palli
 */
public class SKVImportBusinessBean extends ImportBusinessBean implements
		SKVImportBusiness {

	private ArrayList getNameArray(String name, String delimeter) {
		ArrayList nameArray = new ArrayList();

		StringTokenizer tokens = new StringTokenizer(name, delimeter, true);
		String value = null;
		while (tokens.hasMoreTokens()) {
			value = tokens.nextToken();
			if (" ".equals(value) || "-".equals(value)) {
				String nameAtIndex = (String) nameArray
						.get(nameArray.size() - 1);
				StringBuffer buffer = new StringBuffer(nameAtIndex);
				buffer.append(value);
				nameArray.set(nameArray.size() - 1, buffer.toString());
			} else {
				nameArray.add(value);
			}
		}

		return nameArray;
	}

	public User handleNames(User user, String firstName, String middleName,
			String lastName, String preferredNameIndex, boolean store) {
		boolean updateName = false;
		if (firstName == null || firstName.trim().equals("")) {
			if (user.getFirstName() != null) {
				firstName = user.getFirstName();
			} else {
				firstName = "";
			}
		}
		/*
		 * Setting middleName as "", required for the rest of the code
		 */
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

		String delimeter = " -";
		
		if (preferredNameIndex == null) {
			preferredNameIndex = "10";
			delimeter = " ";
		}

		StringBuffer fullname = new StringBuffer();

		fullname.append(firstName).append(" ").append(middleName);

		int index = Integer.parseInt(preferredNameIndex);
		int refName1 = index / 10;
		int refName2 = index % 10;

		ArrayList nameList = getNameArray(fullname.toString(), delimeter);

		firstName = "";
		middleName = "";
		if (refName1 > 0) {
			firstName = (String) nameList.get(refName1 - 1);

			updateName = true;
		}

		if (refName2 > 0) {
			firstName = firstName + (String) nameList.get(refName2 - 1);

			updateName = true;
		}

		for (int i = 0; i < nameList.size(); i++) {
			if ((i + 1) != refName1 && (i + 1) != refName2) {
				middleName = middleName + (String) nameList.get(i);
			}
		}

		/*
		 * Needed because createUser uses the method setFullName that splits the
		 * name with it's own rules
		 */
		if (updateName) {
			if (firstName != null) {
				firstName.trim();
			}

			if (middleName != null) {
				middleName.trim();
			}

			if (lastName != null) {
				lastName.trim();
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
}