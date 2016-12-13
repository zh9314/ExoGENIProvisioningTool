package Configuration;

import java.io.FileWriter;
import java.io.IOException;

import provisioningTool.ARP;

public class SoftwareConf {
	private String scriptPath = "";
	private String installDir = "";
	private String pubAddress = "";
	private String userName = "";
	private String sshPriKey = "";
	
	private String sshOption = "-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null";
	
	public SoftwareConf(String sp, String id, String pa, String un, String SshPriKey){
		scriptPath = sp;
		installDir = id;
		pubAddress = pa;
		userName = un;
		sshPriKey = SshPriKey;
	}
	
	public void installSofware(String OStype){
		if(scriptPath.equals("null")){
			System.out.println("Nothing needs to be installed!");
			return;
		}
		try{
			int lastIndex = scriptPath.lastIndexOf("/");
			String scriptName = scriptPath.substring(lastIndex+1);
			String dirPath = installDir;
			if(installDir.charAt(installDir.length()-1) == '/')
				dirPath = installDir.substring(0, installDir.length()-1);
			int lastIndexDir = dirPath.lastIndexOf('/');
			String dirName = dirPath.substring(lastIndexDir+1);
			if(OStype.toLowerCase().contains("ubuntu")){
				Process ps = Runtime.getRuntime().exec("chmod +x "+scriptPath);  
				ps.waitFor();
			    
				java.util.Calendar cal = java.util.Calendar.getInstance();
				long currentMili = cal.getTimeInMillis();
				String runFilePath = ARP.currentDir+"geni_run_"+currentMili+".sh";
			    FileWriter fw = new FileWriter(runFilePath, false);
			    fw.write("scp -i "+sshPriKey+" "+sshOption+" "+scriptPath+" root@"+pubAddress+":~/\n");
			    fw.write("ssh -i "+sshPriKey+" "+sshOption+"  root@"+pubAddress+" \"chmod +x "+scriptName+"\" \n");
			    if(!userName.equals("null"))
			    	fw.write("ssh -i "+sshPriKey+" "+sshOption+" root@"+pubAddress+" \"mv /root/"+scriptName+" /home/"+userName+"/ \" \n");
			    if(!installDir.equals("null")){
			    	fw.write("scp -i "+sshPriKey+" "+sshOption+" -r "+installDir+" root@"+pubAddress+":~/\n");
			    	if(!userName.equals("null"))
			    		fw.write("ssh -i "+sshPriKey+" "+sshOption+" root@"+pubAddress+" \"mv /root/"+dirName+"/ /home/"+userName+"/\" \n");
			    }
			    if(!userName.equals("null"))
			    	fw.write("ssh -i "+sshPriKey+" "+sshOption+" root@"+pubAddress+" \"sh /home/"+userName+"/"+scriptName+" 0</dev/null 1>/dev/null 2>/dev/null\"\n");
			    else
			    	fw.write("ssh -i "+sshPriKey+" "+sshOption+" root@"+pubAddress+" \"sudo ./"+scriptName+" 0</dev/null 1>/dev/null 2>/dev/null\"\n");
			    fw.close();
			        
			    ps = Runtime.getRuntime().exec("chmod +x "+runFilePath);  
				ps.waitFor();
				
				System.out.println("start run.sh");
				ps = Runtime.getRuntime().exec("sh "+runFilePath);  
				ps.waitFor();
				
				System.out.println("end run.sh");
				
				Thread.sleep(20000);
				
				ps = Runtime.getRuntime().exec("rm "+runFilePath);  
		        ps.waitFor();
			}
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	

}
