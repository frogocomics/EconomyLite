package me.Flibio.EconomyLite.Utils;

import me.Flibio.EconomyLite.EconomyLite;
import me.Flibio.EconomyLite.Utils.FileManager.FileType;
import me.Flibio.EconomyLite.Utils.MySQLManager.ChangeAction;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;

public class BusinessManager {
	
	private FileManager fileManager;
	
	/**
	 * EconomyLite's Business API. 
	 * 
	 * Methods will query a MySQL Database if the EconomyLite user has opted to save data to a database. 
	 * 
	 * If possible, you should run these methods in a seperate thread.
	 */
	public BusinessManager() {
		fileManager = new FileManager();
	}
	
	/**
	 * Registers a new business with EconomyLite
	 * @param businessName
	 * 	Name of the business to register
	 * @return
	 * 	Boolean based on if the method was successful or not
	 */
	public boolean createBusiness(String businessName) {
		if(EconomyLite.access.sqlEnabled) {
			//MySQL
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(mySQL.businessExists(businessName)) return false;
			return mySQL.newBusiness(businessName);
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Check if business exists
			if(!businessExists(businessName)) {
				//Set the balance and the owner of the business
				root.getNode(businessName).getNode("balance").setValue(0);
				root.getNode(businessName).getNode("owners").setValue(new ArrayList<String>());
				root.getNode(businessName).getNode("invited").setValue(new ArrayList<String>());
				root.getNode(businessName).getNode("confirmNeeded").setValue(true);
				fileManager.saveFile(FileType.BUSINESS_DATA, root);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Checks if the specified business exists
	 * @param businessName
	 * 	Business to check
	 * @return
	 * 	Boolean based on if the business was found or not
	 */
	public boolean businessExists(String businessName) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			return mySQL.businessExists(businessName);
		} else {
			//Load and get the businesses file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Check if the business name is in the business file
			for(Object raw : root.getChildrenMap().keySet()) {
				if(raw instanceof String) {
					String business = (String) raw;
					
					if(business.trim().equalsIgnoreCase(businessName.trim())) {
						return true;
					}
				}
			}
			return false;
		}
	}
	
	/**
	 * Gets the balance of the specified business
	 * @param businessName
	 * 	Name of the business whose balance to get
	 * @return
	 * 	Integer of the balance of the business(-1 if an error occured)
	 */
	public int getBusinessBalance(String businessName) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return -1;
			return mySQL.getBusinessBalance(businessName);
		} else {
			//Check if the business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return -1;
				//Read the balance
				ConfigurationNode balance = business.getNode("balance");
				String rawBalance = balance.getString();
				int intBalance;
				try {
					intBalance = Integer.parseInt(rawBalance);
				} catch(NumberFormatException e) {
					return -1;
				}
				return intBalance;
			} else {
				return -1;
			}
		}
	}
	
	/**
	 * Sets a business's to an amount
	 * @param businessName
	 * 	The name of the business whose balance will be set
	 * @param amount
	 * 	What to set the businesses balance to
	 * @return
	 * 	Boolean based on if the method was successful or not
	 */
	public boolean setBusinessBalance(String businessName, int amount) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			return mySQL.setBusinessBalance(businessName, amount);
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return false;
				if(amount<0||amount>1000000) return false;
				//Change the balance
				root.getNode(business.getKey()).getNode("balance").setValue(amount);
				fileManager.saveFile(FileType.BUSINESS_DATA, root);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Gets a list of all owners of a business
	 * @param businessName
	 * 	Name of the business to retrieve owners from
	 * @return
	 * 	List of all owners of the specified business
	 */
	public ArrayList<String> getBusinessOwners(String businessName) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			return mySQL.getOwners(businessName);
		} else {
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return null;
				ArrayList<String> owners = getStringListValues(business.getNode("owners"));
				return owners;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Checks if the specified player is an owner of the specified business
	 * @param businessName
	 * 	Name of the business to check
	 * @param uuid
	 * 	UUID of the player to search for
	 * @return
	 * 	Boolean based on the the player is an owner or not
	 */
	public boolean ownerExists(String businessName, String uuid) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			return mySQL.getOwners(businessName).contains(uuid);
		} else {
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return false;
				ArrayList<String> owners = getStringListValues(business.getNode("owners"));
				if(owners.contains(uuid)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Adds an owner to a business
	 * @param businessName
	 * 	Business whom the owner will be added to
	 * @param uuid
	 * 	UUID of the player to add as an owner
	 * @return
	 * 	Boolean based on if the method was successful or not
	 */
	public boolean addOwner(String businessName, String uuid) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			return mySQL.setOwner(ChangeAction.ADD, uuid, businessName);
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return false;
				ArrayList<String> owners = getStringListValues(business.getNode("owners"));
				owners.add(uuid);
				
				root.getNode(business.getKey()).getNode("owners").setValue(owners);
				fileManager.saveFile(FileType.BUSINESS_DATA, root);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Removes an owner from a business
	 * @param businessName
	 * 	Business whom the owner will be removed from
	 * @param uuid
	 * 	UUID of the player to remove as an owner
	 * @return
	 * 	Boolean based on if the method was successful or not
	 */
	public boolean removeOwner(String businessName, String uuid) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			return mySQL.setOwner(ChangeAction.REMOVE, uuid, businessName);
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return false;
				ArrayList<String> owners = getStringListValues(business.getNode("owners"));
				owners.remove(uuid);
				root.getNode(business.getKey()).getNode("owners").setValue(owners);
				fileManager.saveFile(FileType.BUSINESS_DATA, root);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Deletes a business from EconomyLite
	 * @param businessName
	 * 	The business to delete
	 * @return
	 * 	Boolean based on if the method was successful or not
	 */
	public boolean deleteBusiness(String businessName) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			return mySQL.deleteBusiness(businessName);
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return false;
				root.getNode(business.getKey()).setValue(null);
				fileManager.saveFile(FileType.BUSINESS_DATA, root);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Sets if a business needs confirmation or not
	 * @param businessName
	 * 	Business to change confirmation of
	 * @param needed
	 * 	What to set the confirmation needed to 
	 * @return
	 * 	Boolean based on if the method was successful or not
	 */
	public boolean setConfirmationNeeded(String businessName, boolean needed) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			return mySQL.setConfirm(businessName, needed);
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return false;
				root.getNode(business.getKey()).getNode("confirmNeeded").setValue(needed);
				fileManager.saveFile(FileType.BUSINESS_DATA, root);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Checks if a business needs confirmation to delete it
	 * @param businessName
	 * 	The business to check
	 * @return
	 * 	Boolean based on if the business needs confirmation or not
	 */
	public boolean confirmationNeeded(String businessName) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return true;
			return mySQL.needsConfirm(businessName);
		} else {
			ConfigurationNode business = getBusiness(businessName);
			if(business==null) return true;
			return business.getNode("confirmNeeded").getBoolean();
		}
	}
	
	/**
	 * Sets if a player is invited or not to a business
	 * @param businessName
	 * 	Business whose invited list will be edited
	 * @param uuid
	 * 	UUID of the player to change the invited status of
	 * @param inviteStatus
	 * 	Boolean if the player is invited or not
	 * @return
	 * 	Boolean based on if the method was successful or not
	 */
	public boolean setInvited(String businessName, String uuid, boolean inviteStatus) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			if(inviteStatus){
				return mySQL.setInvite(ChangeAction.ADD, uuid, businessName);
			} else {
				return mySQL.setInvite(ChangeAction.REMOVE, uuid, businessName);
			}
		}
		//Load and get the business file
		fileManager.loadFile(FileType.BUSINESS_DATA);
		ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
		//Check if business exists
		if(businessExists(businessName)) {
			ConfigurationNode business = getBusiness(businessName);
			if(business==null) return false;
			ArrayList<String> invitedList = getStringListValues(business.getNode("invited"));
			ArrayList<String> invited = new ArrayList<String>();
			for(String invitee : invitedList) {
				invited.add(invitee);
			}
			if(inviteStatus) {
				invited.add(uuid);
			} else {
				invited.remove(uuid);
			}
			root.getNode(business.getKey()).getNode("invited").setValue(invited);
			fileManager.saveFile(FileType.BUSINESS_DATA, root);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Checks if a player was invited to join a business
	 * @param businessName
	 * 	The business to check for invite
	 * @param uuid
	 * 	UUID of the player to check for
	 * @return
	 * 	Boolean based on if the player was invited or not
	 */
	public boolean isInvited(String businessName, String uuid) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return false;
			return mySQL.getInvited(businessName).contains(uuid);
		} else {
			//Check if business exists
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return false;
				ArrayList<String> invited = getStringListValues(business.getNode("invited"));
				if(invited.contains(uuid)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Gets a business from the EconomyLite data file
	 * @param businessName
	 * 	Business which to search for
	 * @return
	 * 	ConfigurationNode of the business, null if not found or an error occured
	 */
	private ConfigurationNode getBusiness(String businessName) {
		if(businessExists(businessName)) {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			//Loop through the children and find the business
			for(ConfigurationNode business : root.getChildrenMap().values()) {
				Object raw = business.getKey();
				if(raw instanceof String) {
					if(((String) raw).equalsIgnoreCase(businessName)) {
						return business;
					}
				}
			}
			return null;
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the correct capitilaztion of a business name
	 * @param businessName
	 * 	The business name to capitlize
	 * @return
	 * 	Correctly capitalized business name, empty string if the business is not found
	 */
	public String getCorrectBusinessName(String businessName) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			if(!mySQL.businessExists(businessName)) return "";
			return mySQL.getCapitalizedBusinessName(businessName);
		} else {
			if(businessExists(businessName)) {
				ConfigurationNode business = getBusiness(businessName);
				if(business==null) return "";
				Object raw = business.getKey();
				if(raw instanceof String) {
					return (String) raw;
				} else {
					return "";
				}
			} else {
				return "";
			}
		}
	}
	
	/**
	 * Gets a list of all the businesses for one owner
	 * @param owner
	 * 	The owner to check for
	 * @return
	 * 	An String ArrayList which contains all of the business names the owner is a part of
	 */
	public ArrayList<String> getBusinesses(String owner) {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			return mySQL.getBusinesses(owner);
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			ArrayList<String> businesses = new ArrayList<String>();
			//Loop through the children and find the business
			for(ConfigurationNode business : root.getChildrenMap().values()) {
				Object raw = business.getKey();
				if(raw instanceof String) {
					String businessName = (String) raw;
					if(ownerExists(businessName,owner)) {
						businesses.add(businessName);
					}
				}
			}
			return businesses;
		}
	}
	
	/**
	 * Adds the specified amount of currency to the specified business
	 * @param uuid
	 * 	Name of the business who will receive the currency
	 * @param amount
	 * 	Amount of currency the business will receive
	 * @return
	 * 	If the method failed or was successful
	 */
	public boolean addCurrency(String id, int amount) {
		return setBusinessBalance(id, getBusinessBalance(id) + amount);
	}
	
	/**
	 * Removes the specified amount of currency from the specified business
	 * @param uuid
	 * 	Name of the business whom the currency will be taken from
	 * @param amount
	 * 	Amount of currency the business will lose
	 * @return
	 * 	If the method failed or was successful
	 */
	public boolean removeCurrency(String id, int amount) {
		return setBusinessBalance(id, getBusinessBalance(id) - amount);
	}
	
	/**
	 * Gets a list of all the businesses
	 * @return
	 * 	An String ArrayList which contains all of the business names
	 */
	public ArrayList<String> getAllBusinesses() {
		if(EconomyLite.access.sqlEnabled) {
			MySQLManager mySQL = EconomyLite.getMySQL();
			return mySQL.getAllBusinesses();
		} else {
			//Load and get the business file
			fileManager.loadFile(FileType.BUSINESS_DATA);
			ConfigurationNode root = fileManager.getFile(FileType.BUSINESS_DATA);
			ArrayList<String> businesses = new ArrayList<String>();
			//Loop through the children and find the business
			for(ConfigurationNode business : root.getChildrenMap().values()) {
				Object raw = business.getKey();
				if(raw instanceof String) {
					String businessName = (String) raw;
					businesses.add(businessName);
				}
			}
			return businesses;
		}
	}
	
	private ArrayList<String> getStringListValues(ConfigurationNode node) {
		Object rawList = node.getValue();
		ArrayList<String> toReturn = new ArrayList<String>();
		
		if(rawList instanceof List<?>) {
			List<?> list = (List<?>) rawList;
			for(Object rawObject : list) {
				if(rawObject instanceof String) {
					toReturn.add((String) rawObject);
				}
			}
		}
		return toReturn;
	}
}