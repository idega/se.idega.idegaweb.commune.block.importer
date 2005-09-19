package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.School;
import com.idega.block.school.data.SchoolArea;
import com.idega.block.school.data.SchoolAreaHome;
import com.idega.block.school.data.SchoolDistrict;
import com.idega.block.school.data.SchoolDistrictHome;
import com.idega.block.school.data.SchoolHome;
import com.idega.business.IBOServiceBean;
import com.idega.data.IDOLookup;
import com.idega.user.data.Group;

/**
 * 
 * Description: <br>
 * Copyright: Idega Software 2004 <br>
 * Company: Idega Software <br>
 * @author <a href="mailto:birna@idega.is">Birna Iris Jonsdottir</a>
 */

public class SchoolDistrictImportFileHandlerBean extends IBOServiceBean implements ImportFileHandler , SchoolDistrictImportFileHandler{

	private UserTransaction transaction;
	private SchoolBusiness schoolBiz;
	private SchoolHome sHome;
	private SchoolAreaHome saHome;
	private SchoolDistrictHome sdHome;
	
	private ImportFile file;
	private ArrayList failedRecords = new ArrayList();
	
	public SchoolDistrictImportFileHandlerBean() {}
	
	public boolean handleRecords() throws RemoteException{
		transaction =  this.getSessionContext().getUserTransaction();
		
		try {
			schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
			sHome = schoolBiz.getSchoolHome();
			sdHome = (SchoolDistrictHome) IDOLookup.getHome(SchoolDistrict.class);
			saHome = (SchoolAreaHome) IDOLookup.getHome(SchoolArea.class);
			
			transaction.begin();
			
			String item;
			int count = 1;

			while ( !(item=(String)file.getNextRecord()).equals("") ) {
				
				if(!processRecord(item)) {
					failedRecords.add(item);
				}
				else {
					System.out.println("Processed record number: "+ (count++));
				}
			}
			
			transaction.commit();
			return true;
		}catch (Exception ex) {
			ex.printStackTrace();

			try {
				transaction.rollback();
			}
			catch (SystemException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * The record is:
	 * nr,musicSchoolName,studentSSN,studentTelNr,preSchool,
	 * instrument,singing,level,levelNr,
	 * instrument2,level2Nr,level2,
	 * instument3,level3Nr,level3
	 */
	private boolean processRecord(String record) {
		//data elements from file
		int index = 1;
		String address = file.getValueAtIndexFromRecordString(index++,record);
		String streetNumber = file.getValueAtIndexFromRecordString(index++,record);
		String houseNumber = file.getValueAtIndexFromRecordString(index++,record);
		String school = file.getValueAtIndexFromRecordString(index++,record);
		String district = file.getValueAtIndexFromRecordString(index++,record);

		boolean success = storeInfo(address, streetNumber, houseNumber, school, district);

		return success;
	}
	protected boolean storeInfo(String address, String streetNumber,String houseNumber,String schoolName,String district) {
		
		SchoolArea schoolArea = null;
		
		try {
			schoolArea = saHome.findSchoolAreaByAreaName(district);
		}
		catch(FinderException e) {
			try {
				schoolArea = saHome.create();
				schoolArea.setSchoolAreaName(district);
				schoolArea.store();
			}
			catch(CreateException ce) {
				return false;
			}
		}
		
		School school = null;
		
		try {
			school = sHome.findBySchoolName(schoolName);
		}
		catch(FinderException e) {
			try {
				school = sHome.create();
				school.setSchoolName(schoolName);
				school.setSchoolArea(schoolArea);
				school.store();
			}
			catch(CreateException ce) {
				return false;
			}
		}
		
		SchoolDistrict schoolDistrict = null;
		try {
			schoolDistrict = sdHome.findByStreetAndHouseNumber(streetNumber, houseNumber);
		}
		catch (FinderException fe) {
			try {
				schoolDistrict = sdHome.create();
				schoolDistrict.setAddress(address);
				schoolDistrict.setStreetNumber(streetNumber);
				schoolDistrict.setHouseNumber(houseNumber);
			}
			catch (CreateException ce) {
				return false;
			}
		}
		schoolDistrict.setSchool(school);
		schoolDistrict.setDistrict(district);
		schoolDistrict.store();
		
		return true;
	}
	
	public void setImportFile(ImportFile file) throws RemoteException{
		this.file = file;
	}

	public void setRootGroup(Group rootGroup) throws RemoteException{
	}
	
	public List getFailedRecords() throws RemoteException{
		return failedRecords;
	}
}