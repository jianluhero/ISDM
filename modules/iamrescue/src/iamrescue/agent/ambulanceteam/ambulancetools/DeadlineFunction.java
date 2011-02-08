package iamrescue.agent.ambulanceteam.ambulancetools;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class DeadlineFunction {
	double[] function = new double[300];
	int index = 1;
	/*
	 * init code for reading in the file
	 */
	public DeadlineFunction(){
		String curDir = System.getProperty("user.dir");

		//String dirStr = curDir + "/../modules/iamrescue/src/iamrescue/agent/ambulanceteam/ambulancetools/deadline.csv";
		String dirStr = curDir + "/deadline.csv";
		File fFile = new File(dirStr);
		function[0]=10000;
		try {
			processLineByLine(fFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	 protected void processLine(String aLine){
		 function[index] = Double.valueOf(aLine);
		 index++;
	 }
	
	public final void processLineByLine(File fFile) throws FileNotFoundException {
	    Scanner scanner = new Scanner(fFile);
	    try {
	      //first use a Scanner to get each line
	      while ( scanner.hasNextLine() ){
	        processLine( scanner.nextLine() );
	      }
	    }
	    finally {
	      //ensure the underlying stream is always closed
	      scanner.close();
	    }
	 }
	/*
	 * returns the number steps this civilian has to live
	 */
	public double getDeadline(double hp){
		//find index with nearest value to hp
		
		int closest = 0;
		double dist=Double.MAX_VALUE;
		for(int i=0; i<300; i++){
			double d = Math.abs(function[i]-hp);
			if(d<dist){
				dist=d;
				closest=i;
			}
		}
		//assume dead by end
		return 300-closest;
	}
}
