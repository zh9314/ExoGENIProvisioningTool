package provisioningTool;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import orca.ndl.NdlAbstractDelegationParser;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.util.ssl.ContextualSSLProtocolSocketFactory;
import orca.util.ssl.MultiKeyManager;
import orca.util.ssl.MultiKeySSLContextFactory;

public class RPCConnector {
	
	public  static String apiURL;
	public String userKeyPath, keyAlias, keyPass, sshPubKeyPath, databaseDir;
	private Logger swLog;
	private static final String CREATE_SLICE = "orca.createSlice";
	private static final String SLICE_STATUS = "orca.sliceStatus";
	private static final String DELETE_SLICE = "orca.deleteSlice";
	private static final String LIST_RESOURCES = "orca.listResources";
	private static final String ERR_RET_FIELD = "err";
	private static final String RET_RET_FIELD = "ret";
	private static final String MSG_RET_FIELD = "msg";
	
	private static final String RDF_START = "<rdf:RDF";
	private static final String RDF_END = "</rdf:RDF>";
	
	
	private static final int HTTPS_PORT = 443;
	private static MultiKeyManager mkm = null;
	private static ContextualSSLProtocolSocketFactory regSslFact = null;
	boolean sslIdentitySet = false;
	// alternative names set on the cert that is in use. Only valid when identity is set
	Collection<List<?>> altNames = null;
	
	// VM domains known to this controller
	private List<String> knownDomains = null;
	
	// Resource availability of the current SM
	private Map<String, Map<String, Integer>> resourceSlots = null;
	
	public RPCConnector(ConfLoader confLoader, Logger log){
		this.apiURL = confLoader.ApiUrl;
		this.userKeyPath = confLoader.UserKeyPath;
		this.keyAlias = confLoader.KeyAlias;
		this.keyPass = confLoader.KeyPassword;
		this.sshPubKeyPath = confLoader.SshPubKeyPath;
		this.swLog = log;
	}
	
	public static final Map<String, String> domainMap;
	static {
		Map<String, String> dm = new HashMap<String, String>();
		dm.put("RENCI (Chapel Hill, NC USA) XO Rack", "rcivmsite.rdf#rcivmsite");
		dm.put("BBN/GPO (Boston, MA USA) XO Rack", "bbnvmsite.rdf#bbnvmsite");
		dm.put("Duke CS (Durham, NC USA) XO Rack", "dukevmsite.rdf#dukevmsite");
		dm.put("UNC BEN (Chapel Hill, NC USA)", "uncvmsite.rdf#uncvmsite");
		dm.put("RENCI BEN (Chapel Hill, NC USA)", "rencivmsite.rdf#rencivmsite");
		dm.put("NICTA (Sydney, Australia) XO Rack", "nictavmsite.rdf#nictavmsite");
		dm.put("FIU (Miami, FL USA) XO Rack", "fiuvmsite.rdf#fiuvmsite");
		dm.put("UH (Houston, TX USA) XO Rack", "uhvmsite.rdf#uhvmsite");
		dm.put("UvA (Amsterdam, The Netherlands) XO Rack", "uvanlvmsite.rdf#uvanlvmsite");
		dm.put("UFL (Gainesville, FL USA) XO Rack", "uflvmsite.rdf#uflvmsite");
		dm.put("UCD (Davis, CA USA) XO Rack", "ucdvmsite.rdf#ucdvmsite");
		dm.put("OSF (Oakland, CA USA) XO Rack", "osfvmsite.rdf#osfvmsite");
		dm.put("SL (Chicago, IL USA) XO Rack", "slvmsite.rdf#slvmsite");
		dm.put("WVN (UCS-B series rack in Morgantown, WV, USA)", "wvnvmsite.rdf#wvnvmsite");
		dm.put("NCSU (UCS-B series rack at NCSU)", "ncsuvmsite.rdf#ncsuvmsite");
		dm.put("NCSU2 (UCS-C series rack at NCSU)", "ncsu2vmsite.rdf#ncsu2vmsite");
		dm.put("TAMU (College Station, TX, USA) XO Rack", "tamuvmsite.rdf#tamuvmsite");
		dm.put("UMass (UMass Amherst, MA, USA) XO Rack", "umassvmsite.rdf#umassvmsite");
		dm.put("WSU (Detroit, MI, USA) XO Rack", "wsuvmsite.rdf#wsuvmsite");
		dm.put("UAF (Fairbanks, AK, USA) XO Rack", "uafvmsite.rdf#uafvmsite");
		dm.put("PSC (Pittsburgh, PA, USA) XO Rack", "pscvmsite.rdf#pscvmsite");
		dm.put("GWU (Washington DC,  USA) XO Rack", "gwuvmsite.rdf#gwuvmsite");
		dm.put("CIENA (Ottawa,  CA) XO Rack", "cienavmsite.rdf#cienavmsite");

		domainMap = Collections.unmodifiableMap(dm);
	}
	
	public static final Map<String, String> netDomainMap;
	static {
		Map<String, String> ndm = new HashMap<String, String>();

		ndm.put("RENCI XO Rack Net", "rciNet.rdf#rciNet");
		ndm.put("BBN/GPO XO Rack Net", "bbnNet.rdf#bbnNet");
		ndm.put("Duke CS Rack Net", "dukeNet.rdf#dukeNet");
		ndm.put("UNC BEN XO Rack Net", "uncNet.rdf#UNCNet");
		ndm.put("NICTA XO Rack Net", "nictaNet.rdf#nictaNet");
		ndm.put("FIU XO Rack Net", "fiuNet.rdf#fiuNet");
		ndm.put("UH XO Rack Net", "uhNet.rdf#uhNet");
		ndm.put("NCSU XO Rack Net", "ncsuNet.rdf#ncsuNet");
		ndm.put("UvA XO Rack Net", "uvanlNet.rdf#uvanlNet");
		ndm.put("UFL XO Rack Net", "uflNet.rdf#uflNet");
		ndm.put("UCD XO Rack Net", "ucdNet.rdf#ucdNet");
		ndm.put("OSF XO Rack Net", "osfNet.rdf#osfNet");
		ndm.put("SL XO Rack Net", "slNet.rdf#slNet");
		ndm.put("WVN XO Rack Net", "wvnNet.rdf#wvnNet");
		ndm.put("NCSU XO Rack Net", "ncsuNet.rdf#ncsuNet");
		ndm.put("NCSU2 XO Rack Net", "ncs2Net.rdf#ncsuNet");
		ndm.put("TAMU XO Rack Net",  "tamuNet.rdf#tamuNet");
		ndm.put("UMass XO Rack Net",  "umassNet.rdf#umassNet");
		ndm.put("WSU XO Rack Net",  "wsuNet.rdf#wsuNet");
		ndm.put("UAF XO Rack Net",  "uafNet.rdf#uafNet");
		ndm.put("PSC XO Rack Net",  "pscNet.rdf#pscNet");
		ndm.put("GWU XO Rack Net",  "gwuNet.rdf#gwuNet");
		ndm.put("CIENA XO Rack Net",  "cienaNet.rdf#cienaNet");

		ndm.put("I2 ION/AL2S", "ion.rdf#ion");
		ndm.put("NLR Net", "nlr.rdf#nlr");
		ndm.put("BEN Net", "ben.rdf#ben");
	
		netDomainMap = Collections.unmodifiableMap(ndm);
	}
	
	static {
		mkm = new MultiKeyManager();
		regSslFact = new ContextualSSLProtocolSocketFactory();
		
		// register the protocol (Note: All xmlrpc clients must use XmlRpcCommonsTransportFactory
		// for this to work). See ContextualSSLProtocolSocketFactory.
		
		Protocol reghhttps = new Protocol("https", (ProtocolSocketFactory)regSslFact, HTTPS_PORT); 
		Protocol.registerProtocol("https", reghhttps);
	}
	
	TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					// return 0 size array, not null, per spec
					return new X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateExpiredException, CertificateNotYetValidException {
					// Trust always, unless expired
					// FIXME: should check the cert of controller we're talking to
					for(X509Certificate c: certs) {
						c.checkValidity();	
					}
				}
			}
	};
	
	private KeyStore loadJKSData(FileInputStream jksIS, String keyAlias, String keyPassword)
			throws Exception {

		KeyStore ks = KeyStore.getInstance("jks");
		ks.load(jksIS, keyPassword.toCharArray());

		return ks;
	}
	
	
	private File loadUserFile(String pathStr) {
		File f;

		if (pathStr.startsWith("~/")) {
			pathStr = pathStr.replaceAll("~/", "/");
			f = new File(System.getProperty("user.home"), pathStr);
		}
		else {
			f = new File(pathStr);
		}

		return f;
	}

	/**
	 * Set the identity for the communications to the XMLRPC controller. Eventually
	 * we may talk to several controller with different identities. For now only
	 * one is configured.
	 */
	protected void setSSLIdentity() throws Exception {
		
		if (sslIdentitySet)
			return;

		try {
			URL ctrlrUrl = new URL(apiURL);

			KeyStore ks = null;
			File keyStorePath = loadUserFile(userKeyPath);

			if (keyStorePath.exists()) {
				FileInputStream jksIS = new FileInputStream(keyStorePath);
				ks = loadJKSData(jksIS, keyAlias, keyPass);
				
				jksIS.close();
			}

			if (ks == null)
				throw new Exception("Was unable to find either: " + keyStorePath.getCanonicalPath());

			// check that the spelling of key alias is proper
			Enumeration<String> as = ks.aliases();
			while (as.hasMoreElements()) {
				String a = as.nextElement();
				if (keyAlias.toLowerCase().equals(a.toLowerCase())) {
					keyAlias = a;
					break;
				}
			}

			// alias has to exist and have a key and cert present
			if (!ks.containsAlias(keyAlias)) {
				throw new Exception("Alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");
			}

			if (ks.getKey(keyAlias, keyPass.toCharArray()) == null)
				throw new Exception("Key with alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");

			if (ks.getCertificate(keyAlias) == null) {
				throw new Exception("Certificate with alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");
			}

			if (ks.getCertificate(keyAlias).getType().equals("X.509")) {
				X509Certificate x509Cert = (X509Certificate)ks.getCertificate(keyAlias);
				altNames = x509Cert.getSubjectAlternativeNames();
				try {
					x509Cert.checkValidity();
				} catch (Exception e) {
					throw new Exception("Certificate with alias " + keyAlias + " is not yet valid or has expired.");
				}
			}

			// add the identity into it
			mkm.addPrivateKey(keyAlias, 
					(PrivateKey)ks.getKey(keyAlias, keyPass.toCharArray()), 
					ks.getCertificateChain(keyAlias));

			// before we do SSL to this controller, set our identity
			mkm.setCurrentGuid(keyAlias);

			// add this multikey context factory for the controller host/port
			int port = ctrlrUrl.getPort();
			if (port <= 0)
				port = HTTPS_PORT;
			regSslFact.addHostContextFactory(new MultiKeySSLContextFactory(mkm, trustAllCerts), 
					ctrlrUrl.getHost(), port);

			sslIdentitySet = true;
			
		} catch (Exception e) {
			e.printStackTrace();

			throw new Exception("Unable to load user private key and certificate from the keystore: " + e);
		}
	}
	
	private String getUserKeyFile(File path) {
		try {
			FileInputStream is = new FileInputStream(path);
			BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				// re-add line separator
				sb.append(System.getProperty("line.separator"));
			}

			bin.close();

			return sb.toString();

		} catch (IOException e) {
			return null;
		}
	}
	
	
	/** submit an ndl request to create a slice, using explicitly specified users array
	 * 
	 * @param sliceId
	 * @param resReq
	 * @param users
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String createSlice(String sliceId, String resReq, List<Map<String, ?>> users) throws Exception {
		assert(sliceId != null);
		assert(resReq != null);

		String result = null;
		setSSLIdentity();

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(apiURL));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// create sliver
			rr = (Map<String, Object>)client.execute(CREATE_SLICE, new Object[]{ sliceId, new Object[]{}, resReq, users});
		} catch (MalformedURLException e) {
			swLog.log("ERROR", "RPCConnector.createSlice", "Please check the SM URL " + apiURL);
			throw new Exception("Please check the SM URL " + apiURL);
		} catch (XmlRpcException e) {
			swLog.log("ERROR", "RPCConnector.createSlice", "Unable to contact SM " + apiURL + " due to " + e);
			throw new Exception("Unable to contact SM " + apiURL + " due to " + e);
		} catch (Exception e) {
			swLog.log("ERROR", "RPCConnector.createSlice", "Unable to submit slice to SM:  " + apiURL + " due to " + e);
			return "Unable to submit slice to SM:  " + apiURL + " due to " + e;
		}

		if (rr == null){
			swLog.log("ERROR", "RPCConnector.createSlice", "Unable to contact SM " + apiURL);
			throw new Exception("Unable to contact SM " + apiURL);
		}

		if ((Boolean)rr.get(ERR_RET_FIELD)){
			swLog.log("ERROR", "RPCConnector.createSlice", "Unable to create slice: " + (String)rr.get(MSG_RET_FIELD));
			throw new Exception("Unable to create slice: " + (String)rr.get(MSG_RET_FIELD));
		}

		result = (String)rr.get(RET_RET_FIELD);
		return result;
	}
	
	
	/**
	 * submit an ndl request to create a slice using this user's credentials
	 * @param sliceId
	 * @param resReq
	 * @param users
	 * @return
	 */
	public String createSlice(String sliceId, String resReq, String confPubKeyPath) throws Exception {
		setSSLIdentity();

		// collect user credentials from $HOME/.ssh or load from portal

		// create an array
		List<Map<String, ?>> users = new ArrayList<Map<String, ?>>();
		String keyPathStr = null;
		String userKey = null;
		File keyPath;
		
		if(confPubKeyPath.equals("null"))
			keyPathStr = sshPubKeyPath;
		else
			keyPathStr = confPubKeyPath;
		if (keyPathStr.startsWith("~/")) {
			keyPathStr = keyPathStr.replaceAll("~/", "/");
			keyPath = new File(System.getProperty("user.home"), keyPathStr);
		}
		else {
			keyPath = new File(keyPathStr);
		}

		userKey = getUserKeyFile(keyPath);

		if (userKey == null) {
			swLog.log("ERROR", "ARP.createSlice", "Unable to load user public ssh key " + keyPath);
			throw new Exception("Unable to load user public ssh key " + keyPath);
		}

		Map<String, Object> userEntry = new HashMap<String, Object>();

		userEntry.put("login", "root");
		List<String> keys = new ArrayList<String>();
		keys.add(userKey);
		userEntry.put("keys", keys);
		users.add(userEntry);

		// submit the request
		return createSlice(sliceId, resReq, users);
	}
	
	
	@SuppressWarnings("unchecked")
	public boolean deleteSlice(String sliceId)  throws Exception {
		boolean res = false;
		setSSLIdentity();

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(apiURL));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// delete sliver
			rr = (Map<String, Object>)client.execute(DELETE_SLICE, new Object[]{ sliceId, new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the SM URL " + apiURL);
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact SM " + apiURL + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact SM " + apiURL);
		}

		if (rr == null)
                        throw new Exception("Unable to contact SM " + apiURL);

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to delete slice: " + (String)rr.get(MSG_RET_FIELD));
		else
			res = (Boolean)rr.get(RET_RET_FIELD);

		return res;
	}

	
	@SuppressWarnings("unchecked")
	public String sliceStatus(String sliceId)  throws Exception {
		assert(sliceId != null);

		String result = null;
		setSSLIdentity();

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(apiURL));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// sliver status
			rr = (Map<String, Object>)client.execute(SLICE_STATUS, new Object[]{ sliceId, new Object[]{}});

		} catch (MalformedURLException e) {
			throw new Exception("Please check the SM URL " + apiURL);
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact SM " + apiURL + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact SM " + apiURL);
		}

		if (rr == null)
			throw new Exception("Unable to contact SM " + apiURL);

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to get slice status: " + rr.get(MSG_RET_FIELD));

		result = (String)rr.get(RET_RET_FIELD);

		return result;
	}
	
	/*
	 * Get resources from Service Provider
	 */
	@SuppressWarnings("unchecked")
	public String listResources() throws Exception {

		String result = null;
		setSSLIdentity();

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(apiURL));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// modify slice
			rr = (Map<String, Object>)client.execute(LIST_RESOURCES, new Object[]{ new Object[]{}, new HashMap<String, String>()});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the SM URL " + apiURL);
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact SM " + apiURL + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact SM " + apiURL);
		}

		if (rr == null)
			throw new Exception("Unable to contact SM " + apiURL);

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to list resources: " + (String)rr.get(MSG_RET_FIELD));

		result = (String)rr.get(RET_RET_FIELD);
		return result;
	}
	
	// sets the knownDomains instance variable based
	// on a query to the selected SM
	public void listSMResources() throws Exception {
		// query the selected controller for resources
			// re-initialize known domains and resource slots
			knownDomains = new ArrayList<String>();
			resourceSlots = new TreeMap<String, Map<String, Integer>>();
			
			String ads = listResources();
			List<String> domains = new ArrayList<String>();
		
			try {
				
				boolean done = false;
				int testc = 0;
				//while (!done) {
					// find <rdf:RDF> and </rdf:RDF>
					int start = ads.indexOf(RDF_START);
					int end = ads.indexOf(RDF_END);
					if ((start == -1) || (end == -1)) {
						done = true;
						//continue;
					}
					String ad = ads.substring(start, end + RDF_END.length());
					System.out.println("num "+testc+": "+ad+"mark");
					swLog.log("INFO", "RPCConnector.listSMResources", "Num "+testc+": "+ad+"mark");
					testc++;

					AdLoader adl = new AdLoader();
					// parse out
					NdlAbstractDelegationParser nadp = new NdlAbstractDelegationParser(ad, adl);
					
					// this will call the callbacks
					nadp.processDelegationModel();
					Map<String, Integer> hardwareType = new HashMap<String, Integer>();
					hardwareType = adl.getSlots();
					Iterator iterator = hardwareType.keySet().iterator();                
		            while (iterator.hasNext()) {    
		             Object key = iterator.next();    
		             System.out.println("Key is: "+key+"\nmap.get(key) is :"+hardwareType.get(key));    
		            }
					System.out.println("GetDomain: "+adl.getDomain()+"\n\n\n\n");
					domains.add(adl.getDomain());
						
					String domShortName = reverseLookupDomain(adl.getDomain());
					System.out.println("short name: "+domShortName);
					if (domShortName == null)
						domShortName = adl.getDomain();
						
					if (!resourceSlots.containsKey(domShortName))
						resourceSlots.put(domShortName, new TreeMap<String, Integer>());
					   	resourceSlots.get(domShortName).putAll(adl.getSlots());
						
						nadp.freeModel();
						
						// advance pointer
						ads = ads.substring(end + RDF_END.length());
						//System.out.println("ads: "+ads);
					//}
				} catch (NdlException e) {
					return;
				}
			System.out.println("domains:");
				for(String d: domains) {
					if (d.endsWith("Domain/vm")) {
						String domName = reverseLookupDomain(d);
						System.out.println(domName);
						if (domName != null)
							knownDomains.add(domName);
					}
				}
				
		}
	
	
	
	public static String reverseLookupDomain(String dom) {
		if (dom == null)
			return null;
		// strip off name space and "/Domain"
		String domainName = StringUtils.removeStart(dom, NdlCommons.ORCA_NS);
		//System.out.println("remove: "+domainName);
		if (domainName == null)
			return null;
		
		// try vm domain, then net domain
		String mapping = reverseLookupDomain_(dom, domainMap, "/Domain");
		if (mapping == null)
			mapping = reverseLookupDomain_(dom, domainMap, "/Domain/vm");
		if (mapping == null)
			mapping = reverseLookupDomain_(dom, domainMap, "/Domain/lun");
		if (mapping == null) 
			mapping = reverseLookupDomain_(dom, domainMap, "/Domain/vlan");
		if (mapping == null)
			mapping = reverseLookupDomain_(dom, domainMap, "/Domain/baremetalce");
		if (mapping == null) 
			mapping = reverseLookupDomain_(dom, netDomainMap, "/Domain/vlan");
		
		return mapping;
	}
	
	
	// use different maps to try to do a reverse lookup
		private static String reverseLookupDomain_(String dom, Map<String, String> m, String suffix) {
			String domainName = StringUtils.removeStart(dom, NdlCommons.ORCA_NS);
			if (domainName == null)
				return null;
			
			// remove one or the other
			domainName = StringUtils.removeEnd(domainName, suffix);
			for (Iterator<Map.Entry<String, String>> domName = m.entrySet().iterator(); domName.hasNext();) {
				Map.Entry<String, String> e = domName.next();
				if (domainName.equals(e.getValue()))
					return e.getKey();
			}
			return null;
		}

}
