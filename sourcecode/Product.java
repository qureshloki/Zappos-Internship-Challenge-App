
public class Product {

	String productId;
	String productName;
	String productUrl;
	float price;
	String styleId;
	
	Product(String prodid,String name,String url,float price,String style)
	{
		productId=prodid;
		productName=name;
		productUrl=url;
		this.price=price;
		styleId=style;		
	}
	
	public String toString()
	{
		return "|price: $"+price+", productId: "+productId+",styleId: "+styleId+", productName: "+productName+", productUrl: "+productUrl+"|";
	}
}
