package phonebuzz;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.twilio.sdk.verbs.Say;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

@Controller 
public class PhoneBuzzController {
	
	@RequestMapping("/simple")
	public ResponseEntity<?> simplePB() {
		ResponseEntity<String> resp = new ResponseEntity<String>(HttpStatus.NO_CONTENT);
		HttpHeaders headers = new HttpHeaders();
		
		try {
			TwiMLResponse twiml = new TwiMLResponse();
			twiml.append(new Say("Hello Leland"));
			resp = new ResponseEntity<String>(twiml.toXML(), headers, HttpStatus.ACCEPTED);
			return resp;
		}
		catch (TwiMLException e){
			e.printStackTrace();
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}
	}

}
