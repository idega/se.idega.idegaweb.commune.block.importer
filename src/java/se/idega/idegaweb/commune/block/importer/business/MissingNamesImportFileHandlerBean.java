/*
 * Created on 30.6.2004
 *
 * Copyright (C) 2004 Idega hf. All Rights Reserved.
 *
 *  This software is the proprietary information of Idega hf.
 *  Use is subject to license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.FinderException;

import com.idega.block.importer.data.ImportFile;
import com.idega.business.IBOServiceBean;
import com.idega.data.IDOStoreException;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.user.data.UserHome;
import com.idega.util.database.ConnectionBroker;
import com.idega.util.text.TextSoap;

/**
 * @author aron
 *
 * MissingNamesImportFileHandlerBean TODO Describe this type
 */
public class MissingNamesImportFileHandlerBean extends IBOServiceBean implements MissingNamesImportFileHandler{
	
	private ImportFile file;

	/* (non-Javadoc)
	 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
	 */
	public List getFailedRecords() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see com.idega.block.importer.business.ImportFileHandler#handleRecords()
	 */
	public boolean handleRecords() throws RemoteException {
		
		UserHome citizenHome = (UserHome)getIDOHome(User.class);
		Map userMap = getMissingNameUsersByPersonalID();
		if(userMap==null || userMap.isEmpty()){
			log("No nameless users to handle");
			return false;
		
		}
		String item;
		int count = 0,fixcount = 0;
		Map fieldMap;
		while (!(item = (String) file.getNextRecord()).equals("")) {
			item = TextSoap.findAndCut(item, "#UP ");
			fieldMap = stripOutFields(item);
			if(fieldMap.containsKey(ImportFileFieldConstants.PIN_COLUMN)){
				String pid = (String) fieldMap.get(ImportFileFieldConstants.PIN_COLUMN);
				
				String firstName = fieldMap.containsKey(ImportFileFieldConstants.FIRST_NAME_COLUMN)?(String)fieldMap.get(ImportFileFieldConstants.FIRST_NAME_COLUMN):null;
				String lastName = fieldMap.containsKey(ImportFileFieldConstants.LAST_NAME_COLUMN)?(String)fieldMap.get(ImportFileFieldConstants.LAST_NAME_COLUMN):null;
				if(firstName!=null || lastName!=null){
					if(userMap.containsKey(pid)){
						Integer userID = (Integer) userMap.get(pid);
						log("update user "+userID+" with names "+firstName+" and "+lastName);
						try {
							User user = citizenHome.findByPrimaryKey(userID);
							user.setFullName(firstName+" "+lastName);
							user.store();
							fixcount++;
						} catch (IDOStoreException e) {
							e.printStackTrace();
						} catch (FinderException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
			count++;
			
		}
		log("Recovered "+fixcount+" citizen names out of "+count);
		return true;
	}
	
	private Map stripOutFields(String record){
		Map map = new HashMap();
		String line = null,property = null,value = null;
		String seperator = " ";
		int index = -1;
		try {
			LineNumberReader reader = new LineNumberReader(new StringReader(record));
			while ((line = reader.readLine()) != null) {
				if ((index = line.indexOf(seperator)) != -1) {
					property = line.substring(0, index);
					value = line.substring(index + 1, line.length());
					map.put(property, value);
					//log("Param:"+property+" Value:"+value);
				}
			}

			reader.close();
			reader = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
	
	private Map getMissingNameUsersByPersonalID(){
		Map map = new HashMap();
		Connection conn = null;
		Statement stmt = null;
		try {
			
			String sql = "select personal_id,ic_user_id from ic_user where first_name is null and last_name is null and personal_id is not null";
			conn = ConnectionBroker.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql.toString());
			while(rs.next()){
				String personalID = rs.getString(1);
				Integer userID = new Integer(rs.getInt(2));
				map.put(personalID,userID);
				
			}
			rs.close();
	
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally{
			if (stmt != null) {
                try {
					stmt.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }	
			if (conn != null) {
                ConnectionBroker.freeConnection(conn);
            }
		
		}
		return map;
	}
	
	public void log(String msg){
		super.log("[Missing Names Import Handler] "+msg);
	}
	
	/* (non-Javadoc)
	 * @see com.idega.block.importer.business.ImportFileHandler#setImportFile(com.idega.block.importer.data.ImportFile)
	 */
	public void setImportFile(ImportFile file) throws RemoteException {
		this.file = file;

	}
	/* (non-Javadoc)
	 * @see com.idega.block.importer.business.ImportFileHandler#setRootGroup(com.idega.user.data.Group)
	 */
	public void setRootGroup(Group rootGroup) throws RemoteException {
		
	}
}
