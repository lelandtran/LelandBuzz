package phonebuzz;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class PhoneCall implements Serializable {
	private Timestamp time;
	private String phoneNum;
	private int delay;
	private int digits;
	
	public PhoneCall() {
		time = new Timestamp(0);
		phoneNum = "null";
		delay = 0;
		digits = 0;
	}
	
	/* NOTE: Default constructor written so BeanPropertyRowMapper could 
	 * instantiate the Replay class
	 */
	public PhoneCall(Timestamp time, String phoneNum, int delay, int digits) {
		this.time = time;
		this.phoneNum = phoneNum;
		this.delay = delay;
		this.digits = digits;
	}
	
	public String toString(){
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
		String timeFormatted = dateFormat.format(this.time);
		return String.format("Replay[time=%s, phoneNum=%s, delay=%d, digits=%d",
				timeFormatted, this.phoneNum, this.delay, this.digits);
	}
	
	public Timestamp getTime() {
		return time;
	}
	public void setTime(Timestamp time) {
		this.time = time;
	}
	public String getPhoneNum() {
		return phoneNum;
	}
	public void setPhoneNum(String phoneNum) {
		this.phoneNum = phoneNum;
	}
	public int getDelay() {
		return delay;
	}
	public void setDelay(int delay) {
		this.delay = delay;
	}
	public int getDigits() {
		return digits;
	}
	public void setDigits(int digits) {
		this.digits = digits;
	}


	
	
	
}
