package phonebuzz;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.twilio.sdk.verbs.Gather;
import com.twilio.sdk.verbs.Play;
import com.twilio.sdk.verbs.Say;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

@Controller 
public class PhoneBuzzController {
	public static final String FIZZ = "fizz";
	public static final String BUZZ = "buzz";
	public static final String HASH = "#";
	public static final String GET  = "GET";
	public static final String EMPTY_STR = "";
	public static final String DIGITS_DEFAULT = "5";
	public static final String XML_TYPE = "application/xml";
	public static final int    MAX_DIGITS = 2;
	public static final String X_TWI_SIG = "X-Twilio-Signature";
	public static final String AUTH_TOKEN = "8054f0218fc92eaed7f4d9e22e3cea01";
	public static final String PB_URL = "http://lelandbuzz.herokuapp.com/phonebuzz";
	public static final String BEEP_MP3 = "http://soundbible.com/mp3/Censored_Beep-Mastercard-569981218.mp3";
	
	@RequestMapping("/")
	public String home() {
		
		return "index";
	}
	
	@RequestMapping("/simple")
	public ResponseEntity<?> simple(HttpServletRequest req) {
		
		
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
			/* NOTE: Could not figure out how to nest a Say within a Gather using Java helper library */
			Gather gather = new Gather();
			gather.setNumDigits(MAX_DIGITS);
			gather.setFinishOnKey(HASH);
			gather.setAction(PB_URL);
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
	
	@RequestMapping("/phonebuzz")
	public ResponseEntity<?> phonebuzz(
			@RequestParam(value="Digits", required=false, defaultValue=DIGITS_DEFAULT) String digits,
			HttpServletRequest req){
		ResponseEntity<String> resp = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
		TwiMLResponse twiml = new TwiMLResponse();
		HttpHeaders headers = new HttpHeaders();
		try {
			
			validateRequest(req);
			
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
		catch (Exception e){
			resp = new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return resp;
	}
	
	private String getReqURL(HttpServletRequest req) {
		String url = req.getRequestURL().toString()+ "?" + req.getQueryString();
		return url;
	}
	
	private String getExpectedXTwiSig(HttpServletRequest req) {
		return hmacSha1Base64(AUTH_TOKEN, getReqURL(req));
	}
	
	private void validateRequest(HttpServletRequest req) throws TwiMLException {
		String hmacsha1base64 = getExpectedXTwiSig(req);
		String xTwiSig = req.getHeader(X_TWI_SIG);
		if  (!hmacsha1base64.equals(xTwiSig)){
			throw new TwiMLException(X_TWI_SIG + " mismatch");
		}
	}
	
	private String hmacSha1Base64(String key, String valueToDigest) {
		byte[] hmacsha1 = HmacUtils.hmacSha1(key, valueToDigest);
		String hmacsha1base64 = Base64.getEncoder().encodeToString(hmacsha1);
		return hmacsha1base64;
	}
	
	
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
	
	

}
