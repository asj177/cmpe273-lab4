package edu.sjsu.cmpe.cache.client;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

public class CRDTClient {

	ConcurrentHashMap<String,CacheServiceInterface> serverList=new ConcurrentHashMap<String,CacheServiceInterface> ();
	public static ArrayList<String>successFullServers;
	private static CountDownLatch countDownLatch;
	public ConcurrentHashMap<String, ArrayList<String>> getResults;
	int maxCount=0;
	String maxValue=null;
	CRDTClient()
	{
		 CacheServiceInterface cache0 = new DistributedCacheService("http://localhost:3000",this);
		 CacheServiceInterface cache1 = new DistributedCacheService("http://localhost:3001",this);
		 CacheServiceInterface cache2 = new DistributedCacheService("http://localhost:3002",this);
		 serverList.put("http://localhost:3000", cache0);
		 serverList.put("http://localhost:3001", cache1);
		 serverList.put("http://localhost:3002", cache2);
	}
	
	public static void putForSuccess(String url){
		successFullServers.add(url);
		countDownLatch.countDown();
	}
	
	public void getFailed(String url){
		System.out.println("Get failed for "+url);
		countDownLatch.countDown();
	}
	
	public void getResultsForServers(HttpResponse<JsonNode> response, String serverUrl){
		
		String value=null;
		
		if(response.getCode()==200){
			value = response.getBody().getObject().getString("value");
			System.out.println("Server "+serverUrl+" has value "+value);
			ArrayList serversWithValue =getResults.get(value);
			 if (serversWithValue == null) {
	                serversWithValue = new ArrayList(3);
	            }
			 serversWithValue.add(serverUrl);
			 int count = serversWithValue.size();
			 
			 if(count>maxCount){
				 maxCount = count;
				 maxValue=value;
			 }
			 
			 getResults.put(value, serversWithValue);
			 countDownLatch.countDown();
		}
		
	}
	
	public void failed(String url){
		System.out.println("Update failed on "+url);
		countDownLatch.countDown();
		
	}

/**
      Put Functionality 
   **/
	public boolean put(long key,String value) throws InterruptedException{
		boolean success=true;
		countDownLatch = new CountDownLatch(serverList.size());
		successFullServers=new ArrayList(serverList.size());
		for(CacheServiceInterface cache:serverList.values()){
			
			cache.put(key, value);
		}
        
        countDownLatch.await();
		//countDownLatch.await();
		if(successFullServers.size()<2){
			success=false;
			
		}
		
		if(!success){
			delete(key,value);
		}
		return success;
		
	}
	
	/**
            Get Functionality along with the Read Repair Logic 
         **/

	public String get(long key) throws InterruptedException{
		countDownLatch = new CountDownLatch(serverList.size());
		getResults=new ConcurrentHashMap<String, ArrayList<String>>();
		for(String url:successFullServers){
			CacheServiceInterface server =serverList.get(url);
			server.get(key);
		}
		countDownLatch.await();//Used for synchronization ,ensures that update is gone on all 3 servers 
		
		for(Entry<String, ArrayList<String>> valueServersPair :getResults.entrySet() ){
			
			
			//Read - Repair Logic 
			 String value = valueServersPair.getKey();
			 
			 if(!maxValue.equals(value)){
				 for (String cacheServerUrl : valueServersPair.getValue()) {
					 CacheServiceInterface server = serverList.get(cacheServerUrl);
	                    server.put(key, maxValue);
				 }
			 }
		}
		return maxValue;
		
		
	}
	
public void delete(long key,String value){
		
		for(String url:successFullServers){
			
			CacheServiceInterface server =serverList.get(url);
			server.delete(key);
		}
	}
}
