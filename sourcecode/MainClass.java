import java.util.ArrayList;
import java.util.Scanner;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MainClass {
	
	float precision; //precision in dollars..combinations having a difference within the precision value from the minimum difference (i.e diff of closest combination) will also be  displayed.
	float budget; //input from user
	int itemsRequired; //input from user
	int productsArrEndIndex; 
	ArrayList <Product> productsArr; // stores the products.
	float absMinDifference; //absolute minimum difference of budget and total sum of a chosen product combination
	ArrayList<int[]> finalCombinations; // list of indices final product combinations closest to the budget.
	int[] currentCombination; //temporary array
	
	public MainClass(float a, int b) {
		budget=a;
		itemsRequired=b;
		precision=0.25f; 
		absMinDifference=Float.POSITIVE_INFINITY;
		productsArr=new ArrayList<Product>();
		finalCombinations=new ArrayList<int[]>();
		currentCombination=new int[itemsRequired];
				
		
	}
	
	public void getProducts()
	{
		try
		{
			Client client = ClientBuilder.newClient();
			int pageNo=1;
			boolean fetchAll=false; //flag to fetch all available products on Zappos
			int noOfPagesToFetch=2; //limit the no. of products fetched else it will return many product combinations that are closest to the budget(since algorithm is exponential).
			String key="52ddafbe3ee659bad97fcce7c53592916a6bfd73";
			String searchTerm="";
			String excludeStr="%5B%22colorId%22,%22brandName%22,%22thumbnailImageUrl%22,%22originalPrice%22,%22percentOff%22%5D";
			String includeStr="%5B%22pageCount%22%5D";
			String sortStr="%7B%22price%22%3A%22asc%22%7D"; //sort the results by price
			int limit=10;
			JSONParser parser = new JSONParser();
			WebTarget target;
			Invocation.Builder invocationBuilder;
			Response response;
			JSONObject jsonObj=new JSONObject();
			JSONArray jsonArr=new JSONArray();								
						
			
			//fetching products from Zappos api and parsing json objects and converting into "Product" POJO's
			do {
		
				target = client.target("http://api.zappos.com/Search?term="+searchTerm+"&excludes="+excludeStr+"&includes="+includeStr+"&limit="+limit+"&sort="+sortStr+"&page="+pageNo+"&key="+key);
				invocationBuilder = target.request(MediaType.APPLICATION_JSON);	
				response = invocationBuilder.get();
				jsonObj=(JSONObject)parser.parse(response.readEntity(String.class));
				if(!((String)jsonObj.get("statusCode")).equals("200"))
					{
					System.out.println("Error: "+(String)jsonObj.get("message"));
					break;
					}
				jsonArr.addAll((JSONArray)jsonObj.get("results"));
				pageNo++;
							
				}	while((fetchAll && pageNo <= (Integer)jsonObj.get("pageCount")) || (!fetchAll && pageNo<=noOfPagesToFetch));
			
			System.out.println("Objects:");
			for(int i=0;i<jsonArr.size();i++)
				{
				float price=Float.parseFloat(((String)((JSONObject)jsonArr.get(i)).get("price")).substring(1));
				Product p=new Product((String)((JSONObject)jsonArr.get(i)).get("productId"),(String)((JSONObject)jsonArr.get(i)).get("productName"),(String)((JSONObject)jsonArr.get(i)).get("productUrl"),price,(String)((JSONObject)jsonArr.get(i)).get("styleId"));
				productsArr.add(p);
				System.out.println(p.toString());
				}
			System.out.println("Total objects loaded:" + productsArr.size());
			System.out.println();
			System.out.println();
			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
		}
		
	}
	
	//function that initiates calculation and display of product combinations
	public void displayCombinations() throws ParseException
	{		
		getProducts();
		productsArrEndIndex=productsArr.size()-1;
		int startindex=0;
		
		//if the sum of the n maximum priced products is less than the budget, then modify the start index for the recursion. (since array is sorted in ascending order of price)
		if(itemsRequired*productsArr.get(productsArrEndIndex).price-budget<0.009 )
		{			
			for(int i=productsArrEndIndex-itemsRequired;i>=0;i--)
				if(productsArr.get(productsArrEndIndex-itemsRequired-1).price-productsArr.get(i).price >precision)
					startindex=i;
			getCombination(itemsRequired,startindex,0,currentCombination);
		}
		else getCombination(itemsRequired,startindex,0,currentCombination);
		
		printCombinations();
			
	}
	
	
	//function to print the closest product combinations for the inputs
	public void printCombinations()
	{
		float total;
		System.out.println("Product Combinations for your Input:\n");
		for(int i=0;i<finalCombinations.size();i++)
		{
			total=0;
			System.out.println((i+1)+" ->");
			for(int j=0;j<itemsRequired;j++)
			{
				Product p=productsArr.get(finalCombinations.get(i)[j]);
				total+=p.price;
				System.out.println(p.toString());
			}
			System.out.println("Total: $"+total+" Offset from Budget: $"+(total-budget));
			System.out.println();
		}
	}
	
	
	/*
	 * Recursive function to find all the product combinations..
	 * It does a depth first search... It has few conditions which limit the branching/recursion..
	 * It finds product combinations that are closest to the budget (above or below)..
	 * It wont display duplicate products in a combination..
	 * e.g. if user requires 3 items and total available items are also 3 but there is one duplicate (i.e same productid,same price but different styleid),
	 * the algorithm will display 0 combinations instead of giving 1 combination of all items..!
	 */
	
	public void getCombination(int itemsLeft,int startIndex,float currSum,int[] currentCombination)
	{
		
	try
		{
		/*
		 if items remaining to be selected is 0, check if the new offset from budget is lesser than (previous offset-precision). 
		 if yes then update the minimum offset and also clear the previous final product combinations list.
		 else just add the new combination to the list of final combinations.
		 
		 */
		if(itemsLeft==0) 
		{	
			float temp=Math.abs(currSum-budget);
			if( temp-absMinDifference + precision  < 0.0f)
				{
				finalCombinations.clear();
				absMinDifference=temp;
				}
			finalCombinations.add(currentCombination.clone());
			return;
		}
		
		
		else
		{
			for(int i=startIndex;i<=productsArrEndIndex;i++)
			{
				Product pCurr=productsArr.get(i);				
				boolean ignoreProdId=false;
				
				/*
				 * To eliminate repeat of same product with different styles in a combination. Also to limit braching/recursion due to same product with different style.  
				 */
				if(i!=0 )
					{
						Product pPrev=productsArr.get(i-1);
						if(pPrev.productId.equals(pCurr.productId) && Math.abs(pCurr.price-pPrev.price)<0.009)					
							ignoreProdId=true;
						else
						{
							for(int j=currentCombination.length-1;j>=itemsLeft;j--)
								if(productsArr.get(currentCombination[j]).productId.equals(pCurr.productId))
									ignoreProdId=true;
							
						}
					}
				
				//check that puts bounds on the recursion. (since the products are sorted in ascending order of price)
				float temp=itemsLeft*pCurr.price + currSum - budget;
				if(!ignoreProdId && temp-absMinDifference < precision)
				{	
					if(itemsLeft==1)
						{
						if(Math.abs(temp)-absMinDifference > precision)
							return;
						}							
					else if(i==productsArrEndIndex)
							return;
					
					currentCombination[itemsLeft-1]=i;
					getCombination(itemsLeft-1,i+1,currSum+pCurr.price,currentCombination);					
				}	
				
			}			
		}
		}
	catch(Exception e)
	
		{
		e.printStackTrace();
		}
		
	}
	

	//main function
	public static void main(String args[])
	{
		try
		{
			
		Scanner sc=new Scanner(System.in);
		System.out.println("Please enter your budget in dollars upto 2 decimal places");
		float budget=Math.round(sc.nextFloat()*100)/100.0f;
		
		/*We could increase the maxItems but increasing it too much might cause a problem
		with the stack since I am using recursion. Also, practically speaking very few people
		would be buying more than 20 different items as gifts in one go. 
		 */
		int maxItems=20;
		System.out.println("Please enter the number of items you wish to buy (less than "+maxItems+")");
		int itemsRequired=sc.nextInt();
		sc.close();
		if(itemsRequired<maxItems)
			{
			MainClass obj=new MainClass(budget,itemsRequired);
			obj.displayCombinations();			
			}
		else System.out.println("Enter less than 20 items to buy");
		
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		
	}
}
