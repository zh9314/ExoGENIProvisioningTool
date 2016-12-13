package provisioningTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ConfLoader {
	private String confFilePath;
	public String UserKeyPath = "";
	public String KeyAlias = "";
	public String KeyPassword = "";   
	public String SshPubKeyPath = "";   
	public String SshPriKeyPath = ""; 
	public String ApiUrl = "";
	//private String SupportDomainString = "";
	//public String [] SupportDomains;
	public String DatabaseDir = "";
	public String LogsDir = "";
	
	public ConfLoader(String confFilePath){
		this.confFilePath = confFilePath;
	}
	
	public boolean loadConfiguration(){
		File conf = new File(confFilePath);
		try {
			BufferedReader in = new BufferedReader(new FileReader(conf));
			String line = null;
			while((line = in.readLine()) != null){
				String[] cmd = line.split("=");
				if(cmd[0].trim().toLowerCase().equals("userkeypath"))
					UserKeyPath = cmd[1];
				if(cmd[0].trim().toLowerCase().equals("keyalias"))
					KeyAlias = cmd[1];
				if(cmd[0].trim().toLowerCase().equals("keypassword"))
					KeyPassword = cmd[1];
				if(cmd[0].trim().toLowerCase().equals("sshpubkeypath"))
					SshPubKeyPath = cmd[1];
				if(cmd[0].trim().toLowerCase().equals("sshprikeypath"))
					SshPriKeyPath = cmd[1];
				if(cmd[0].trim().toLowerCase().equals("databasedir"))
					DatabaseDir = cmd[1];
				if(cmd[0].trim().toLowerCase().equals("logsdir"))
					LogsDir = cmd[1];
				if(cmd[0].trim().toLowerCase().equals("apiurl"))
					ApiUrl = cmd[1];
			}
			if(UserKeyPath.equals("") || KeyAlias.equals("") ||
					KeyPassword.equals("") || SshPubKeyPath.equals("") || SshPriKeyPath.equals("") ||
					DatabaseDir.equals("") || LogsDir.equals("") || ApiUrl.equals(""))
				return false;
			DatabaseDir = rephaseTheDir(DatabaseDir);
			LogsDir = rephaseTheDir(LogsDir);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	///make the dir path always end up with character '/'
	private String rephaseTheDir(String inputDir){
		String outputDir = inputDir;
		if(inputDir.lastIndexOf('/') != inputDir.length()-1)
			outputDir += "/";
		return outputDir;
	}

}
