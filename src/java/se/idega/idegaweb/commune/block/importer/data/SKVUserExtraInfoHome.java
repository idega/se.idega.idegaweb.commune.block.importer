package se.idega.idegaweb.commune.block.importer.data;


import javax.ejb.CreateException;
import com.idega.data.IDOHome;
import javax.ejb.FinderException;
import com.idega.user.data.User;

public interface SKVUserExtraInfoHome extends IDOHome {
	public SKVUserExtraInfo create() throws CreateException;

	public SKVUserExtraInfo findByPrimaryKey(Object pk) throws FinderException;

	public SKVUserExtraInfo findByUser(User user) throws FinderException;

	public SKVUserExtraInfo findByUserID(int userID) throws FinderException;
}