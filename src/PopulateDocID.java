
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.Tables;

/**
 * 
 */

/**
 * @author dichenli
 * populate WordCount table on DynamoDB
 */
public class PopulateDocID {
	
	static AmazonDynamoDBClient dynamoDB; //DB, collection of tables
	static String tableName = "DocURL"; //self defined table name. new table will be created if not exists
	static String keyName = "docID"; //self defined name for the table key
	static ScalarAttributeType keyType = ScalarAttributeType.B; //http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DataModel.html
	static long writeCapacity = 1L; //http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ProvisionedThroughputIntro.html
	static long readCapacity = 1L;
	
	
	
	
	/**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.ProfilesConfigFile
     * @see com.amazonaws.ClientConfiguration
     */
	private static void init() throws Exception {
        /*
         * The ProfileCredentialsProvider will return your [aws150415]
         * credential profile by reading from the credentials file located at
         * (/Users/dichenli/.aws/credentials).
         */
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("aws150415").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/dichenli/.aws/credentials), and is in valid format.",
                    e);
        }
        dynamoDB = new AmazonDynamoDBClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        dynamoDB.setRegion(usWest2);
    }
	
	public static void createTable() throws Exception {
		init();

        try {
            String tableName = PopulateDocID.tableName;

            // Create table if it does not exist yet
            if (Tables.doesTableExist(dynamoDB, tableName)) {
                System.out.println("Table " + tableName + " is already ACTIVE");
            } else {
                // Create a table with a primary hash key named 'name', which holds a string
                CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                    .withKeySchema(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName(keyName).withAttributeType(keyType))
                    .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(readCapacity).withWriteCapacityUnits(writeCapacity));
                    TableDescription createdTableDescription = dynamoDB.createTable(createTableRequest).getTableDescription();
                System.out.println("Created Table: " + createdTableDescription);

                // Wait for it to become active
                System.out.println("Waiting for " + tableName + " to become ACTIVE...");
                Tables.awaitTableToBecomeActive(dynamoDB, tableName);
            }

            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);

            // Add an item
            String[] words1 = {"Bill", "Ted", "Excellent", "year"};
            Map<String, AttributeValue> item = newItem(words1);
            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
            System.out.println("Result: " + putItemResult);

            // Add another item
            item = newItem("Airplane", "Bill", "James", "Billy");
            putItemRequest = new PutItemRequest(tableName, item);
            putItemResult = dynamoDB.putItem(putItemRequest);
            System.out.println("Result: " + putItemResult);

//            // Scan items for movies with a year attribute greater than 1985
//            HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
//            Condition condition = new Condition()
//                .withComparisonOperator(ComparisonOperator.GT.toString())
//                .withAttributeValueList(new AttributeValue().withN("1985"));
//            scanFilter.put("year", condition);
//            ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
//            ScanResult scanResult = dynamoDB.scan(scanRequest);
//            System.out.println("Result: " + scanResult);

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
	}
	
	
	
	
	public static void main(String[] args) throws Exception {
        
    }

	static Integer index = 0;
    private static Map<String, AttributeValue> newItem(String... words) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(keyName, new AttributeValue().withN((index++).toString())); //insert the item with its key (incrementing number)
        item.put("words", new AttributeValue().withSS(words)); //SS: string set, which means a collection of strings (words is String[] type)
        return item;
    }
}
