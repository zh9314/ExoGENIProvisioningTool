package Configuration;

import java.io.FileWriter;
import java.io.IOException;

import provisioningTool.ARP;

public class SshConf {
	private String pubAddress = "";
	private String userName = "";
	private String pubKeyPath = "";
	private String sshPriKey = "";
	
	private String sshOption = "-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null";
	
	public SshConf(String pa, String un, String pk, String SshPriKey){
		pubAddress = pa;
		userName = un;
		pubKeyPath = pk;
		sshPriKey = SshPriKey;
	}
	
	/*public void firstConnect(){
		try {
			FileWriter fw = new FileWriter("expect.sh");
			fw.write("#!/usr/bin/expect -f\n");
			fw.write("set timeout 15\n");
		    fw.write("spawn ssh -i ExoGENIcerts/id_dsa -p 22 root@"+pubAddress+" \"echo 'login Success!'\"\n");
		    fw.write("expect \"connecting\"\n");
			fw.write("send \"yes\\n\"\n");
			fw.write("interact\n");
			fw.close();
			
			Process ps = Runtime.getRuntime().exec("chmod +x expect.sh");  
			ps.waitFor();
			
			System.out.println("exec expect.sh");
			ps = Runtime.getRuntime().exec("expect expect.sh");  
			
			BufferedReader in = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) 
				System.out.println(line);
			
			ps.waitFor();
			
			
			
			Thread.sleep(1000);
			
			ps = Runtime.getRuntime().exec("rm expect.sh");  
	        ps.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}*/
	
	public void confUserSSH(String OStype){
		int lastIndex = pubKeyPath.lastIndexOf("/");
		String pubKeyName = pubKeyPath.substring(lastIndex+1); 
	    try {
	    	if(OStype.toLowerCase().contains("ubuntu")){
	    		java.util.Calendar cal = java.util.Calendar.getInstance();
	    		long currentMili = cal.getTimeInMillis();
	    		String sshFilePath = ARP.currentDir+"geni_ssh_"+currentMili+".sh";
		    	FileWriter fw = new FileWriter(sshFilePath, false);
				fw.write("useradd -d \"/home/"+userName+"\" -m -s \"/bin/bash\" "+userName+"\n");
				fw.write("mkdir /home/"+userName+"/.ssh \n");
				fw.write("mv "+pubKeyName+" /home/"+userName+"/.ssh/authorized_keys \n");
			    fw.write("chmod 740 /etc/sudoers \n");
			    fw.write("echo \""+userName+" ALL=(ALL)NOPASSWD: ALL\" >> /etc/sudoers \n");
			    fw.write("chmod 440 /etc/sudoers \n");
			    fw.write("chown -R "+userName+":"+userName+" /home/"+userName+"/.ssh/\n");
			    fw.close();  
			    
			    String runFilePath = ARP.currentDir+"geni_runSSH_"+currentMili+".sh";
			    fw = new FileWriter(runFilePath, false);
			    fw.write("chmod +x "+sshFilePath+"\n");
			    fw.write("scp -i "+sshPriKey+" "+sshOption+" "+sshFilePath+" root@"+pubAddress+":~/ssh.sh\n");
			    fw.write("scp -i "+sshPriKey+" "+sshOption+" "+pubKeyPath+" root@"+pubAddress+":~/"+pubKeyName+"\n");
			    fw.write("ssh -i "+sshPriKey+" "+sshOption+" root@"+pubAddress+" \"sudo ./ssh.sh\"\n");
			    fw.write("sleep 2s\n");
			    fw.write("ssh -i "+sshPriKey+" "+sshOption+" root@"+pubAddress+" \"rm ssh.sh\"\n");
			    fw.close();
			    
			    Process ps = Runtime.getRuntime().exec("chmod +x "+runFilePath);  
				ps.waitFor();
				
				ps = Runtime.getRuntime().exec("sh "+runFilePath);  
				ps.waitFor();
				
				//Thread.sleep(2000);
			    //ps = Runtime.getRuntime().exec("rm "+runFilePath+" "+sshFilePath);  
				//ps.waitFor();
	    	}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
