package phonebuzz;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.twilio.sdk.verbs.*;

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
	public static final String PB_URL = "http://lelandbuzz.herokuapp.com/phonebuzz";
	public static final String BEEP_MP3 = "http://soundbible.com/mp3/Censored_Beep-Mastercard-569981218.mp3";
	
	@RequestMapping("/")
	public String home() {
		
		return "index";
	}
	
	@RequestMapping("/simple")
	public ResponseEntity<?> simple() {
		ResponseEntity<String> resp = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
		HttpHeaders headers = new HttpHeaders();
		
		try {
			/* Obstacle: Could not figure out how to nest a Say within a Gather using Java helper library */
			TwiMLResponse twiml = new TwiMLResponse();
			twiml.append(new Say("Welcome to Leland's PhoneBuzz."));
			twiml.append(new Say("After the beep, enter a number up to " + MAX_DIGITS + " digits followed by the pound key to hear FizzBuzz up to that number."));
			twiml.append(new Play(BEEP_MP3));
			Gather gather = new Gather();
			gather.setNumDigits(MAX_DIGITS);
			gather.setFinishOnKey(HASH);
			gather.setAction(PB_URL);
			gather.setAction(GET);
			twiml.append(gather);
			headers.add(HttpHeaders.CONTENT_TYPE, XML_TYPE);
			resp = new ResponseEntity<String>(twiml.toXML(), headers, HttpStatus.OK);
			return resp;
		}
		catch (TwiMLException e){
			e.printStackTrace();
			return new ResponseEntity<String>(HttpStatus.UNPROCESSABLE_ENTITY);
		}
	}
	
	@RequestMapping("/phonebuzz")
	public ResponseEntity<?> phonebuzz(@RequestParam(value="Digits", required=false, defaultValue=DIGITS_DEFAULT) String digits){
		ResponseEntity<String> resp = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
		TwiMLResponse twiml = new TwiMLResponse();
		HttpHeaders headers = new HttpHeaders();
		try {
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
				throw new NullPointerException();
			}
		}
		catch (Exception e){
			resp = new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
		return resp;
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
