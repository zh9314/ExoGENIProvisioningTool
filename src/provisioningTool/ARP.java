package provisioningTool;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Configuration.SoftwareConf;
import Configuration.SshConf;
import toscaTransfer.Node;
import toscaTransfer.toscaSubAnalysis;
import toscal2NDL.tosca2INDL;


public class ARP {
	
	public String NDLString;
	public String sliceId;
	public ArrayList<String> publicAddress;
	
	private static Logger swLog;
	public static String currentDir = ""; ///identify current directory of the jar file.
	private static Map<String, String> addressInfo = new HashMap<String, String>();
	
	public ARP(String NDLString, String slice) throws IOException {
		this.NDLString = NDLString;
		this.sliceId = slice;
		this.publicAddress  = new ArrayList<String>();
	}
	
	public ARP(){
		
	}
	
	////return 0 represents not setup yet. 1 represents setup. -1 represents failed.
	public static int analysisStatus(String status)
	{
		String ss = status;
		int seq = ss.indexOf("Status: ");
		if(seq == -1)
			return 0;
		//boolean tricketed = false;
		boolean allActive = true;
		while(seq != -1)
		{
			String statusw = ss.substring(seq+8,seq+14); 
			//System.out.println("Status: "+statusw);
			if(!statusw.equals("Active"))
				allActive = false;
			//if(statusw.equals("Ticket"))
				//tricketed = true;
			if(statusw.equals("Failed"))
				return -1;
			ss = ss.substring(seq+14);
			seq = ss.indexOf("Status: ");
		}
		if(!allActive){
			//System.out.println("Not all active!");
			return 0;
		}
		//System.out.println("All are actived!");
		return 1;
		
	}
	
	
	private static String getNDLStringFromFile(String NDLfile){
		StringBuilder sb = null;
		try {
			BufferedReader bin = null;
			File f = new File(NDLfile);
			FileInputStream is = new FileInputStream(f);
			bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			sb = new StringBuilder();
			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
			}

			bin.close();
		} catch (Exception e) {
			System.err.println("Error "  + e + " encountered while readling file " + NDLfile);
			System.exit(1);
		} finally {
			;
		}
		return sb.toString();
	}
	
	
	
	public static boolean createSlice(String NDLString, String sliceName, RPCConnector rpc,
			String confPubKeyPath) throws IOException
	{
		
		try {
			swLog.log("INFO","ARP.createSlice","Creating slice " + sliceName);
			long startTime = System.currentTimeMillis();
			swLog.log("INFO","ARP.createSlice","Start time at "+startTime);
			String result = rpc.createSlice(sliceName, NDLString, confPubKeyPath);
			long createdTime = System.currentTimeMillis();
			swLog.log("INFO","ARP.createSlice","Result of create slice: \n" + result);
			swLog.log("INFO","ARP.createSlice","Slice created time at "+createdTime);
			long activatedTime = 0;
			
			int QuerryCount = 0;
			while(QuerryCount < 300)
			{
				Thread.sleep(1000);

				String status = rpc.sliceStatus(sliceName);
				
				int statusNow = analysisStatus(status);
				
				Thread.sleep(7000);
				
				if(statusNow == 1)
				{
					activatedTime = System.currentTimeMillis();
					swLog.log("INFO","ARP.createSlice","Slice is activated successfully! at time of "+activatedTime);
					swLog.log("INFO","ARP.createSlice","Created duration: "+(createdTime-startTime)/1000+"s");
					swLog.log("INFO","ARP.createSlice","Activated duration: "+(activatedTime-startTime)/1000+"s");
					int begin = status.indexOf('<');
					String xmlStatus = status.substring(begin);
					ArrayList<String> publicIPs = tosca2INDL.getPublicIPs(xmlStatus);
					for(int i = 0 ; i<publicIPs.size() ; i++){
						String [] node_ip = publicIPs.get(i).split("::");
						System.out.println(node_ip[0]+" : "+node_ip[1]);
						addressInfo.put(node_ip[0], node_ip[1]);
						swLog.log("INFO","ARP.createSlice","Address info -> "+publicIPs.get(i));
					}
					return true;
				}
				if(statusNow == -1)
				{
					swLog.log("ERROR","ARP.createSlice","Sth wrong during creating slices!");
					break;
				}
				QuerryCount++;
			}

			} catch (Exception e) {
				swLog.log("ERROR","ARP.createSlice","An exception has occurred in creating slice " + e);
				return false;
			}
		return false;
	}
	
	////generate the provisioned TOSCA file and update the information about the public address
	private static void getAddressInfo(String sliceName, String toscaFilePath, ArrayList<Node> nodes){
		/*File sliceLog = new File(sliceName+".log");
		ArrayList<String> nodeInfo = new ArrayList<String>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(sliceLog));
			String line = null;
			while((line = in.readLine()) != null){
				if(line.contains("Address info")){
					int begin = line.indexOf("->");
					int end = line.length();
					String info = line.substring(begin+3, end);
					nodeInfo.add(info);
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		////update the public address information
		for(int j = 0 ; j<nodes.size() ; j++){
			if(addressInfo.containsKey(nodes.get(j).nodeName))
				nodes.get(j).publicAddress = addressInfo.get(nodes.get(j).nodeName);
			else
				swLog.log("WARN", "ARP.getAddressInfo"
						, "There is no public address for node "+nodes.get(j).nodeName);
		}
		
		//if(nodeInfo.size() > 0)
		if(addressInfo.size() > 0)
		{
			int point = toscaFilePath.lastIndexOf(".");
			String newToscaFilePath = toscaFilePath.substring(0, point)+"_provisioned"+toscaFilePath.substring(point);
			File orgToscaFile = new File(toscaFilePath);
			try {
				BufferedReader in = new BufferedReader(new FileReader(orgToscaFile));
				FileWriter fw = new FileWriter(newToscaFilePath, false);
				String line = null;
				while((line = in.readLine()) != null){
					if(line.contains("public_address")){
						int start = line.indexOf("public_address");
						String spaceString = line.substring(0, start);
						int begin = line.indexOf(':');
						String nodeName = line.substring(begin+1).trim();
						if(addressInfo.containsKey(nodeName))
							fw.write(spaceString+"public_address: "+addressInfo.get(nodeName)+"\n");
						else
							swLog.log("WARN", "ARP.getAddressInfo"
										, "The node name of "+nodeName+" in tosca file may be wrong!");
						
						/*for(int i = 0 ; i<nodeInfo.size() ; i++){
							String [] node_ip = nodeInfo.get(i).split("::");
							for(int j = 0 ; j<nodes.size() ; j++){
								if(nodes.get(j).nodeName.equals(node_ip[0]))
								{
									nodes.get(j).publicAddress = node_ip[1];
									break;
								}
							}
							if(node_ip[0].equals(nodeName)){
								fw.write(spaceString+"public_address: "+node_ip[1]+"\n");
								break;
							}
						}*/
					}else
						fw.write(line+"\n");
				}
				
				fw.close();
				in.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static void configuration(ArrayList<Node> nodes, String pubKeyPath, 
			String userName, String sshPriKeyPath){
		for(int i = 0 ; i<nodes.size() ; i++){
			Node tmpNode = nodes.get(i);
			String scriptPath = tmpNode.script;
			String installDir = tmpNode.installation;
			String pubAddress = tmpNode.publicAddress;
			String OStype = tmpNode.OStype;
			
			swLog.log("INFO", "ARP.configuration", "Configuration on node "+tmpNode.nodeName+" OStype "+OStype);
			//System.out.println("OS: "+OStype);
			SshConf sshConf = new SshConf(pubAddress, userName, pubKeyPath, sshPriKeyPath);
			
			if(!pubKeyPath.equals("null"))
				sshConf.confUserSSH(OStype);
			
			SoftwareConf softConf = new SoftwareConf(scriptPath, installDir, 
					pubAddress, userName, sshPriKeyPath);
			softConf.installSofware(OStype);
		}
	}
	
	
	public static void deleteSlice(String sliceName, RPCConnector rpc) throws Exception
	{
		if(rpc.deleteSlice(sliceName))
			swLog.log("INFO", "ARP.deleteSlice", "Slice "+sliceName+" has been deleted successfully!");
			//System.out.println("Slice "+sliceName+" has been deleted successfully!");
		else
			swLog.log("INFO", "ARP.deleteSlice", "Slice "+sliceName+"does not exist!");
			//System.out.println("Slice "+sliceName+"does not exist!");
	}
	
	////absolute path of current directory
	private static String getCurrentDir(){
		String curDir = new ARP().getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		int index = curDir.lastIndexOf('/');
		return curDir.substring(0, index+1);
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		currentDir = getCurrentDir();
		
		///createSlice confPath tosca/indl xx.ndl    ///deleteSlice confPath sliceName
		
		if(args[0].equals("createSlice")){
			
			if(args.length != 4){
				System.out.println("Paramenters number is wrong!");
				return ;
			}
			ConfLoader confLoader = new ConfLoader(args[1]);
			confLoader.loadConfiguration();
			swLog = new Logger(confLoader.LogsDir+"geni.log");
			swLog.log("INFO", "ARP.main", "Creating slice of "+args[3]+" from ExoGENI!");
			String arg1 = args[2].toLowerCase();
			if(!arg1.equals("tosca") && !arg1.equals("indl"))
			{
				System.out.println("Invalid command!");
				swLog.log("ERROR", "ARP.main", "These is no tosca or indl specified in command!");
				return ;
			}
			String indl_s = "";
			
			tosca2INDL t2i = new tosca2INDL();
			
			toscaSubAnalysis tsa = new toscaSubAnalysis(swLog);
			tsa.generateInfrastructure(args[3]);

			if(arg1.equals("tosca")){
				indl_s = t2i.generateINDL(tsa.nodes, tsa.connections, 1);
			}else{
				indl_s = getNDLStringFromFile(args[3]);
			}
			
			
			String sliceNames = "";
			int begin = args[3].lastIndexOf('/')+1;
			int end = args[3].lastIndexOf('.');
			
			sliceNames = args[3].substring(begin, end);
			RPCConnector rpc = new RPCConnector(confLoader, swLog);
			if(createSlice(indl_s, sliceNames, rpc, "null")){
				getAddressInfo(sliceNames, args[3], tsa.nodes);
				configuration(tsa.nodes, tsa.publicKeyPath, tsa.userName, confLoader.SshPriKeyPath);
			}
			swLog.closeLog();
			
			
			
		}else if(args[0].equals("deleteSlice")){
			ConfLoader confLoader = new ConfLoader(args[1]);
			confLoader.loadConfiguration();
			swLog = new Logger(confLoader.LogsDir+"geni.log");
			RPCConnector rpc = new RPCConnector(confLoader, swLog);
			try {
				deleteSlice(args[2], rpc);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else
			System.out.println("Invalid command!");
		
	}

}
