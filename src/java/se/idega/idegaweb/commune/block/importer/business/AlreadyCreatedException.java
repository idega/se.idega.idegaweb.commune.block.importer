/*
 * Created on 20.6.2003
 */
package se.idega.idegaweb.commune.block.importer.business;

/**
 * @author laddi
 */
public class AlreadyCreatedException extends Exception {

		public AlreadyCreatedException(){
			super();
		}
		
		public AlreadyCreatedException(String s){
			super(s);
		}

}
