package me.husamp.smartconvert;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


/**
 * @author hozcan
 * This class, which implements ResponseHandler interface, handles the XML Response from
 * European Central Bank, takes the rates and calculates the entered value in currency 2
 * and returns it in Double format with 2 decimals. 
 *
 */
public class XMLResponseHandler implements ResponseHandler<String> {
	// List of all currencies by ECB. They exist in an array based on the order in the ArrayAdapter used in MainActivity.
	private static String[] currencies = {"EUR","JPY","USD","BGN","CZK","DKK","GBP","HUF","LTL","PLN",
	                                      "RON","SEK","CHF","NOK","HRK","RUB","TRY","AUD","BRL","CAD","CNY",
	                                      "HKD","IDR","ILS","INR","KRW","MXN","MYR","NZD","PHP","SGD","THB",
	                                      "ZAR"};
	
	private final Integer currency1_key;
	private final Integer currency2_key;
	private final Double value;
	
	// Currency choices and value entered by the user are initialized in the constructor.
	public XMLResponseHandler(int currencyIndex1, int currencyIndex2, double value) {
		// TODO Auto-generated constructor stub
		this.currency1_key = currencyIndex1;
		this.currency2_key = currencyIndex2;
		this.value = value;
	}

	@Override
	public String handleResponse(HttpResponse response)
			throws ClientProtocolException, IOException 
	{
		// Take the currency keys based on the order number 
		String currency1 = currencies[currency1_key];
		String currency2 = currencies[currency2_key];
		Double currency1_val = null;
		Double currency2_val = null;
		String finalResult = "";
		
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(new InputStreamReader(response.getEntity().getContent()));
			int eventType = xpp.getEventType();
			
			// Iterate over the XML values until the rates with the given currencies are found. 
			// If nothing is found, it means that EUR is chosen, whose rate is 1.0
			while (eventType != XmlPullParser.END_DOCUMENT && (currency1_val == null || currency2_val == null)) 
			{
				if (eventType == XmlPullParser.START_TAG) {
					if(xpp.getAttributeCount() == 2)
					{
						String curr_type = xpp.getAttributeValue(null, "currency");
						if(curr_type != null && curr_type.compareTo(currency1) == 0) {
							currency1_val = Double.parseDouble(xpp.getAttributeValue(null, "rate"));
						}
						if(curr_type != null && curr_type.compareTo(currency2) == 0) {
							currency2_val = Double.parseDouble(xpp.getAttributeValue(null, "rate"));
						}	
					}
				}
				eventType = xpp.next();
			}
			if(currency1_val == null ){
				currency1_val = 1.0;
			}
			if(currency2_val == null ){
				currency2_val = 1.0;
			}
		}
		catch (XmlPullParserException e) {
			
		}
		if(currency1_val != null && currency2_val != null){
			// Find the cross rate exchange between 2 different currencies 
			// and return the result in Currency 2 in double format with 2 decimals. 
			Double rate = (double)currency2_val / (double)currency1_val;
			Double new_value = (double)value * (double)rate;
			DecimalFormat df = new DecimalFormat("#.##");
			// TODO Auto-generated method stub
			finalResult = df.format(new_value).toString();
		}
		return finalResult;
	}

}
