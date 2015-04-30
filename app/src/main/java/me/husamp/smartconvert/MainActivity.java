package me.husamp.smartconvert;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author hozcan This application is a currency converter between 33 top
 *         different currencies in the World, by taking the daily conversion
 *         rates from European Central Bank in XML format and making a cross
 *         exchange rate from Euro. If you have any questions, please e-mail:
 *         android.husamp@gmail.com
 */

public class MainActivity extends Activity {
	
	private static final String INITIAL_VALUE = "1.0";
	private Spinner spinner1; // Variable for spinner for the first currency value
	private Spinner spinner2; // Variable for spinner for the second currency value
	private EditText userValue; // Variable for currency value entered by the user 
	private TextView result; // Variable for result value after currency conversion
	private AsyncTask<ConversionParam, Void, String> conversionTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Check if device is connected to Internet. If not, kill the application.
		if(!isConnected()){
			Toast.makeText(getApplicationContext(), R.string.no_connection_warning, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		// Initializing the spinner for first currency choice
		spinner1 = (Spinner) findViewById(R.id.currency1_spinner);
		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
		        R.array.currencies_array, android.R.layout.simple_spinner_item);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(adapter1);
		
		// Initializing the spinner for second currency choice
		spinner2 = (Spinner) findViewById(R.id.currency2_spinner);
		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
		        R.array.currencies_array, android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(adapter2);
		spinner2.setSelection(2); //Setting the initial choice of second currency to USD.
		
		// Initializing EditText variable for the value to be entered by the user and setting the initial value to 1.0
		userValue = (EditText) findViewById(R.id.value);
		userValue.setText(INITIAL_VALUE);
		
		// Initializing TextView variable for the result after conversion
		result = (TextView) findViewById(R.id.result);
		
		// Initialize the variable for choosing another value from the spinner array adapter.
		MyOnItemSelectedListener spin1 = new MyOnItemSelectedListener(); 
		spinner1.setOnItemSelectedListener(spin1);
		MyOnItemSelectedListener spin2 = new MyOnItemSelectedListener(); 
		spinner2.setOnItemSelectedListener(spin2);
		
		//Calling conversion execution method
		executeConversion();
		
		// Implementation of the swapper button - It swaps currency 1 with currency 2 and executes conversion.
		ImageButton swapper = (ImageButton) findViewById(R.id.swapcurrency);
		swapper.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				int spin1 = spinner1.getSelectedItemPosition();
				int spin2 = spinner2.getSelectedItemPosition();
				spinner1.setSelection(spin2);
				spinner2.setSelection(spin1);
				executeConversion();
			}
		});
		
		//Implement a text watcher to execute conversion when the user changes the value
		userValue.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) 
			{
				// TODO Auto-generated method stub
				if(s.toString().compareTo("") != 0){ // Check if the value entered by the user is empty. If so, don't execute the conversion.
					executeConversion();
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}

		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (conversionTask != null) {
			conversionTask.cancel(false);
			conversionTask = null;
		}
	}
	
	/**
	 * @author hozcan This class is designed for this application, to pass data
	 *         into AsyncTask. It has 2 integers, Indexes of the chosen
	 *         currencies, and one double, which is the value entered by the
	 *         user.
	 * 
	 */
	private class ConversionParam {
		private final int currencyIndex1;
		private final int currencyIndex2;
		private final double userValue;
		
		public ConversionParam (int index1, int index2, double userValue) {
			this.currencyIndex1 = index1;
			this.currencyIndex2 = index2;
			this.userValue = userValue;
		}
		
		private int getCurrencyIndex1() {
			return currencyIndex1;
		}
		
		private int getCurrencyIndex2() {
			return currencyIndex2;
		}
		
		private double getUserValue() {
			return userValue;
		}
		
	}

	public class MyOnItemSelectedListener extends Activity implements AdapterView.OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			executeConversion();
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
	}
	
	/**
	 * @author hozcan This private class is for running HttpGetTask operation
	 *         using AsyncTask. It executes the background operation using
	 *         AndroidHttpClient and XMLResponseHandleer classes and updates the
	 *         UI at onPostExecute method.
	 */
	private class HttpGetTask extends AsyncTask<ConversionParam, Void, String> {
		
		private final String URL = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
		
		@Override
		protected void onPreExecute() {
			// Make the progress bar visible, to show the user that the background operation is ongoing.
			ProgressBar progressbar = (ProgressBar) findViewById(R.id.progress);
			progressbar.setVisibility(ProgressBar.VISIBLE);
			Log.d("HttpGetTask","Preexecute");
	    }
		
		@Override
		protected String doInBackground(ConversionParam... params) {
			Log.d("HttpGetTask","doInBackground");
			// Do the background operation using XMLResponseHandler and return the result.
			HttpGet request = new HttpGet(URL);
			ConversionParam conversion = params[0];
			XMLResponseHandler responseHandler = new XMLResponseHandler(
					conversion.getCurrencyIndex1(),
					conversion.getCurrencyIndex2(), conversion.getUserValue());
			AndroidHttpClient client = AndroidHttpClient.newInstance("");
			try {
				String result = client.execute(request, responseHandler);
				client.close();
				request.abort();
				return result;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		
		@Override
		protected void onPostExecute(String result2) {
			// Make the progress bar invisible and set the result in UIthread.
			ProgressBar progressbar = (ProgressBar) findViewById(R.id.progress);
			progressbar.setVisibility(ProgressBar.INVISIBLE);
			Log.d("HttpGetTask","Setting text to: "+ result2);
			result.setText(result2);
	    }

		
	}
		
	/**
	 * This method checks if the device is connected to Internet or not.
	 */
	public Boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
		                      activeNetwork.isConnectedOrConnecting();
		return isConnected;
	}
	
	/**
	 * This method takes the conversion choices and value entered by the user, puts them into a list
	 * and starts the conversion operation using HttpGetTask class execute method.
	 */
	public void executeConversion () {
		if(!isConnected()){
			Toast.makeText(getApplicationContext(), R.string.no_connection_warning, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		ConversionParam conversion = new ConversionParam(
				spinner1.getSelectedItemPosition(),
				spinner2.getSelectedItemPosition(),
				Double.parseDouble(userValue.getText().toString()));
		if (conversionTask != null) {
			conversionTask.cancel(false);
		}
		conversionTask = new HttpGetTask();
		conversionTask.execute(conversion);
	}
}
