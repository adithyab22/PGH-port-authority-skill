package com.maya.portAuthority.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.Session;

/**
 * Contains the methods to interact with the persistence layer for Port Authority in DynamoDB.
 */
public class PaDao {
	private static Logger log = LoggerFactory.getLogger(PaDao.class);
    private final PaDynamoDbClient dynamoDbClient;

    public PaDao(PaDynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void deletePaInput(Session session){
    	PaUserDataItem item = new PaUserDataItem();
        item.setCustomerId(session.getUser().getUserId());
        dynamoDbClient.deleteItem(item);
    }
    
    /**
     * Reads and returns the user information from the session.
     * <p>
     * Returns null if the item could not be found in the database.
     * 
     * @param session
     * @return
     */
    public PaInput getPaInput(Session session) {
        PaUserDataItem item = new PaUserDataItem();
        item.setCustomerId(session.getUser().getUserId());

        item = dynamoDbClient.loadItem(item);

        if (item == null) {
            return null;
        }

        return PaInput.newInstance(session, item.getInputData());
    }

    /**
     * Saves the {@link PaInput} into the database.
     * 
     * @param input
     */
    public void savePaInput(PaInput input) {
        PaUserDataItem item = new PaUserDataItem();
        item.setCustomerId(input.getSession().getUser().getUserId());
        //item.setGameData(game.getGameData());
        item.setInputData(input.getData());
        dynamoDbClient.saveItem(item);
    }
}
