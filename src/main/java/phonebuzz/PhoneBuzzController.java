package phonebuzz;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Call;
import com.twilio.sdk.verbs.Gather;
import com.twilio.sdk.verbs.Play;
import com.twilio.sdk.verbs.Say;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

/**
 * Controller class for a PhoneBuzz web application implemented with Spring MVC
 * Completed as a coding challenge for LendUp
 * @author LelandTran
 *
 */
@Controller 
public class PhoneBuzzController {
	public static final String FIZZ = "fizz";
	public static final String BUZZ = "buzz";
	public static final String HASH = "#";
	public static final String GET  = "GET";
	public static final String STAT_ATTR = "status";
	public static final String EMPTY_STR = "";
	public static final String DIGITS_DEFAULT = "5";
	public static final String DELAY_DEFAULT = "0";
	public static final String TARGET_DEFAULT = "7606217851";
	public static final String XML_TYPE = "application/xml";
	public static final int    MAX_DIGITS = 2;
	public static final String X_TWI_SIG = "X-Twilio-Signature";
	public static final String TWILIO_NUM = "(760) 621-7851";
	public static final String ACCOUNT_SID = "AC6e71fb7422800a2cc39230311da08588";
	public static final String AUTH_TOKEN = "8054f0218fc92eaed7f4d9e22e3cea01";
	public static final String SIMPLE_URL = "http://lelandbuzz.herokuapp.com/simple";
	public static final String PB_URL = "http://lelandbuzz.herokuapp.com/phonebuzz";
	public static final String BEEP_MP3 = "http://soundbible.com/mp3/Censored_Beep-Mastercard-569981218.mp3";
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	/**
	 * The home page for the PhoneBuzz application
	 * Asks the user for a number to send a PhoneBuzz call to and a delay 
	 * @return the view name "index" for the home page
	 */
	@RequestMapping("/")
	public String home() {
		
		return "index";
	}
	
	/**
	 * The page which sends a POST request to Twilio to make a PhoneBuzz call to the number
	 * included as the target parameter in the request to this page.
	 * @param target the number that PhoneBuzz will call and prompt to play
	 * @param model  the model that will be sent to the view
	 * @return       the view for "outbound" page
	 */
	@RequestMapping("/outbound")
	public ResponseEntity<?> outbound(
			@RequestParam(value="target", required=false, defaultValue=TARGET_DEFAULT) String target,
			@RequestParam(value="delay", required=false, defaultValue=DELAY_DEFAULT) String delay,
			Model model){
		ResponseEntity<String> resp = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
		StringBuffer statBuf = new StringBuffer();
		// Use REST API to make call
		// Does not need validation because this is not a TwiML document
		try {
			TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
			Account mainAccount = client.getAccount();
			CallFactory callFactory = mainAccount.getCallFactory();
			Map<String, String> callParams = new HashMap<String, String>();
			callParams.put("To", target);
			callParams.put("From", TWILIO_NUM);
			// NOTE: Here we append a parameter to pass the delay input to /simple
			callParams.put("Url", SIMPLE_URL+"?delay="+delay);
			callParams.put("Method", GET);
			//callParams.put("delay", delay);
			delayInSeconds(Integer.parseInt(delay));
			Call call = callFactory.create(callParams);
			statBuf = statBuf.append("A PhoneBuzz call was made to ").append(target);
			statBuf = statBuf.append("  ");
			statBuf = statBuf.append("Call SID: ").append(call.getSid());
			
		}
		catch (NumberFormatException e) {
			statBuf.append("The delay specified is not a number");
		}
		catch (TwilioRestException e) {
			statBuf.append("The PhoneBuzz call failed due to: ");
			statBuf.append(e.getMessage());
		}
		
		model.addAttribute(STAT_ATTR, statBuf.toString());
		
		//TODO: Do this programmatically with a JSON library
		JsonGenerator generator = Json.createGenerator(new ByteArrayOutputStream());
		generator.toString(); // TODO: remove - written just to suppress an eclipse warning
		resp = new ResponseEntity<String>("{\"status\": \"" + statBuf.toString() + "\"}", HttpStatus.OK);
		return resp;
	}
	
	/**
	 * The TwiML document which prompts the other end of the call to enter a number
	 * to begin PhoneBuzz. The TwiML gathers the number and sends it to the 
	 * phonebuzz() method mapped to /phonebuzz
	 * NOTE for Phase 1: For some reason, the first phone call I make to the phone 
	 * number after a few hours triggers an application error because of a network 
	 * failure. However, calling the number again results in a successful connection.
	 * @param req binding to the HttpRequest
	 * @return ResponseEntity with TwiML prompt for PhoneBuzz 
	 */
	@RequestMapping("/simple")
	public ResponseEntity<?> simple(HttpServletRequest req,
			@RequestParam(value="delay", required=false, defaultValue="66") String delay) {
		
		
		ResponseEntity<String> resp = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
		HttpHeaders headers = new HttpHeaders();

	    		
		try {
			
			/* NOTE: Below code throws an exception if there is a signature mismatch
			 * This is a circumstance I've not really encountered so I'm not sure what 
			 * typical industry protocol is, whether it's best to have the validation
			 * method throw an exception or instead return a boolean which I would check
			 * in this method (simple()) as the caller and throw an exception on mismatch.
			 */
			validateRequest(req);
			
			TwiMLResponse twiml = new TwiMLResponse();
			twiml.append(new Say("Welcome to Leland's PhoneBuzz."));
			twiml.append(new Say("After the beep, enter a number up to " + MAX_DIGITS + " digits followed by the pound key to hear FizzBuzz up to that number."));
			twiml.append(new Play(BEEP_MP3));
			/* NOTE: Could not figure out how to nest a Say within a 
			 * Gather using Java helper library.
			 * Another approach would be to hardcode the TwiML myself 
			 * to allow for a nested Say in the Gather so the user 
			 * could input digits as soon as he/she wanted to. */
			Gather gather = new Gather();
			gather.setNumDigits(MAX_DIGITS);
			gather.setFinishOnKey(HASH);
			// NOTE: Here we append a parameter to pass the delay input to /simple
			gather.setAction(PB_URL+"?delay="+delay);
			gather.setMethod(GET);
			twiml.append(gather);
			headers.add(HttpHeaders.CONTENT_TYPE, XML_TYPE);
			resp = new ResponseEntity<String>(twiml.toXML(), headers, HttpStatus.OK);
			return resp;
		}
		catch (TwiMLException e){
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	/**
	 * The TwiML that recites FizzBuzz up to the number parsed from the String
	 * included as the Digits parameter in the request to this page
	 * @param digits the Digits parameter included in the request to this page
	 * @param req    binding to the HttpRequest to this page
	 * @return       ResponseEntity with TwiML for reciting FizzBuzz
	 */
	@RequestMapping("/phonebuzz")
	public ResponseEntity<?> phonebuzz(
			@RequestParam(value="Digits", required=false, defaultValue=DIGITS_DEFAULT) String digits,
			@RequestParam(value="To", required=true) String target,
			@RequestParam(value="delay", required=false, defaultValue="55") String delay,
			HttpServletRequest req){
		ResponseEntity<String> resp = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
		TwiMLResponse twiml = new TwiMLResponse();
		HttpHeaders headers = new HttpHeaders();
		try {
			
			validateRequest(req);
			
			// Retrieve the auto-generated key that was created from row insertion

			Timestamp time = new Timestamp((new Date()).getTime());
			
			String sql = "INSERT INTO replays(time, phoneNum, delay, digits) "+
					"VALUES(?, ?, ?, ?)";
			jdbcTemplate.update(sql, time, target, delay, digits);
			
			System.err.println("added row");
			/*
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(
				    new PreparedStatementCreator() {
				    	@Override
				        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				            PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"});
				            ps.setTimestamp(1, time);
				            ps.setString(2, target);
				            ps.setInt(3, Integer.parseInt(delay));
				            
				            return ps;
				        }
				    },
				    keyHolder);
			int key = keyHolder.getKey().intValue();
			statBuf.append("  ");
			statBuf.append("Call inserted into database. Key: " + key);
			*/
						
			if (digits != null) {
				int top = Integer.parseInt(digits);
				for (int i = 1; i <= top; i++){
					Say sayDigit = new Say(intToFB(i));
					twiml.append(sayDigit);
				}
				headers.add(HttpHeaders.CONTENT_TYPE, XML_TYPE);
				resp = new ResponseEntity<String>(twiml.toXML(), headers, HttpStatus.OK);
			}
			else {
				throw new NullPointerException("Digits param not included");
			}
		}
		/*
		 * TODO: Include more informative exceptions i.e. TwiMLException, exception from
		 * attempting to parse integers
		 */
		catch (TwiMLException | NullPointerException e){
			resp = new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return resp;
	}
	
	/**
	 * Retrieve the URL that the HttpRequest was sent to
	 * @param req the HttpRequest
	 * @return    the URL the HttpRequest was sent to
	 */
	private String getReqURL(HttpServletRequest req) {
		StringBuffer urlBuf = req.getRequestURL();
		String query = req.getQueryString();
		if (query!=null) {
			urlBuf = urlBuf.append("?").append(query);
		}
		return urlBuf.toString();
	}
	
	/**
	 * Hashes the request's URL to match against the X-Twilio-Signature header of the 
	 * Twilio page request
	 * @param req
	 * @return the hashed URL
	 */
	private String getExpectedXTwiSig(HttpServletRequest req) {
		return hmacSha1Base64(AUTH_TOKEN, getReqURL(req));
	}
	
	/**
	 * Validate that the page request is indeed coming from Twilio
	 * @param req
	 * @throws TwiMLException if the page request is not coming from Twilio
	 */
	private void validateRequest(HttpServletRequest req) throws TwiMLException {
		// TODO: Get this to work with POST request
		String hmacsha1base64 = getExpectedXTwiSig(req);
		String xTwiSig = req.getHeader(X_TWI_SIG);
		if  (!hmacsha1base64.equals(xTwiSig)){
			throw new TwiMLException(X_TWI_SIG + " mismatch");
		}
	}
	
	/**
	 * Hash the valueToDigest with the key in hMAC-SHA1 encryption algorithm and encode
	 * the String in Base64
	 * @param key the key used for hashing
	 * @param valueToDigest the value that will be hashed
	 * @return the hashed value in Base64 encoding
	 */
	private String hmacSha1Base64(String key, String valueToDigest) {
		byte[] hmacsha1 = HmacUtils.hmacSha1(key, valueToDigest);
		String hmacsha1base64 = Base64.getEncoder().encodeToString(hmacsha1);
		return hmacsha1base64;
	}
	
	/**
	 * Returns the String that will be recited by FizzBuzz for int i
	 * @param i the number
	 * @return the String which is said as i in FizzBuzz
	 */
	private String intToFB(int i) {
		String fb = EMPTY_STR;
		if (i % 3 != 0 && i % 5 != 0) {
			fb = ((Integer)i).toString();
		}
		else {
			if (i % 3 == 0) {
				fb += FIZZ;
			}
			if (i % 5 == 0) {
				fb += BUZZ;
			}
		}
		return fb;
	}
	
	/**
	 * Delay the execution of this thread by a specified parameter length of time in milliseconds
	 * @param delay
	 */
	private void delayInSeconds(int delay) {
		try {
			delay = delay * 1000;
			Thread.sleep(delay);
		}
		catch (InterruptedException e) {
			System.out.println("Thread awakened prematurely");
		}
	}
	
	

}
