package toscal2NDL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ho.yaml.Yaml;
import org.ho.yaml.YamlStream;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.InputSource;

import toscaTransfer.Connection;
import toscaTransfer.Eth;
import toscaTransfer.Node;
import toscaTransfer.toscaSubAnalysis;


public class tosca2INDL {
	
	     private static Namespace ns_ec2 = Namespace.getNamespace("ec2", "http://geni-orca.renci.org/owl/ec2.owl#");
	     private static Namespace ns_request;
	     private static Namespace ns_kansei = Namespace.getNamespace("kansei", "http://geni-orca.renci.org/owl/kansei.owl#");
	     private static Namespace ns_appcolor = Namespace.getNamespace("app-color", "http://geni-orca.renci.org/owl/app-color.owl#");
	     private static Namespace ns_geni = Namespace.getNamespace("geni", "http://geni-orca.renci.org/owl/geni.owl#");
	     private static Namespace ns_domain = Namespace.getNamespace("domain", "http://geni-orca.renci.org/owl/domain.owl#");
	     private static Namespace ns_eucalyptus = Namespace.getNamespace("eucalyptus", "http://geni-orca.renci.org/owl/eucalyptus.owl#");
	     private static Namespace ns_collections = Namespace.getNamespace("collections", "http://geni-orca.renci.org/owl/collections.owl#");
	     private static Namespace ns_openflow = Namespace.getNamespace("openflow", "http://geni-orca.renci.org/owl/openflow.owl#");
	     private static Namespace ns_xsd = Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
	     private static Namespace ns_rdf = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	     private static Namespace ns_exogeni = Namespace.getNamespace("exogeni", "http://geni-orca.renci.org/owl/exogeni.owl#");
	     private static Namespace ns_layer = Namespace.getNamespace("layer", "http://geni-orca.renci.org/owl/layer.owl#");
	     private static Namespace ns_rdfs = Namespace.getNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
	     private static Namespace ns_request_schema = Namespace.getNamespace("request-schema", "http://geni-orca.renci.org/owl/request.owl#");
	     private static Namespace ns_ip4 = Namespace.getNamespace("ip4", "http://geni-orca.renci.org/owl/ip4.owl#");
	     private static Namespace ns_planetlab = Namespace.getNamespace("planetlab", "http://geni-orca.renci.org/owl/planetlab.owl#");
	     private static Namespace ns_ethernet = Namespace.getNamespace("ethernet", "http://geni-orca.renci.org/owl/ethernet.owl#");
	     private static Namespace ns_dtn = Namespace.getNamespace("dtn", "http://geni-orca.renci.org/owl/dtn.owl#");
	     private static Namespace ns_time = Namespace.getNamespace("time", "http://www.w3.org/2006/time#");
	     private static Namespace ns_owl = Namespace.getNamespace("owl", "http://www.w3.org/2002/07/owl#");
	     private static Namespace ns_modify_schema = Namespace.getNamespace("modify-schema", "http://geni-orca.renci.org/owl/modify.owl#");
	     private static Namespace ns_compute = Namespace.getNamespace("compute", "http://geni-orca.renci.org/owl/compute.owl#");
	     private static Namespace ns_topology = Namespace.getNamespace("topology", "http://geni-orca.renci.org/owl/topology.owl#");
	     private static Namespace ns_orca = Namespace.getNamespace("orca", "http://geni-orca.renci.org/owl/orca.rdf#");
	     private static Namespace ns_j16 = Namespace.getNamespace("j.16", "http://geni-orca.renci.org/owl/topology.owl#");
	     
	// converting to netmask
		private static final String[] netmaskConverter = {
			"128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
			"255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
			"255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
			"255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"
		};
			
		// helper
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
		
		public static final Map<String, OSdata> OSMap;
		static {
			Map<String, OSdata> os = new HashMap<String, OSdata>();

			os.put("Ubuntu 14.04", new OSdata("http://geni-images.renci.org/images/standard/ubuntu/ub1404-v1.0.4.xml", 
					"9394ca154aa35eb55e604503ae7943ddaecc6ca5"));
			os.put("Centos 6.7 v1.0.0", new OSdata("http://geni-images.renci.org/images/standard/centos/centos6.7-v1.0.0/centos6.7-v1.0.0.xml", 
					"dceedc1e70bd4d8d95bb4e92197f64217d17eea0"));
			os.put("Apache Storm", new OSdata("http://geni-images.renci.org/images/dcvan/storm/Storm-SiteAware-v0.2/Storm-SiteAware-v0.2.xml", 
					"047baee53ecb3455b8c527f064194cad9a67771a"));
			os.put("Debian 6 (squeeze)", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-neuca-v1.0.8.xml", 
					"d1044d9162bd7851e3fc2c57a8251ad6b3641c0c"));
			os.put("Debian 6 (squeeze) + Hadoop", new OSdata("http://geni-images.renci.org/images/hadoop/deb-sparse-hadoop-10G.v0.2.xml", 
					"e1948a8b67d01b93fd0fb1f78ba5c2b3ce0b41f1"));
			os.put("Debian 6 (squeeze) v1.0.10", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6.v1.0.10.xml", 
					"c120b9d79d3f3882114c0e59cce14f671ef9b0db"));
			os.put("Debian 6 (squeeze) v1.0.9", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-neuca-v1.0.9.xml", 
					"e1972b5a5b30fa1adbd42f2df1effbd40084fb3e"));
			os.put("Debian 6 (squeeze) with OVS", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-ovs-neuca-v1.0.3.xml", 
					"ef7e0b4883e23c218d19b0f22980436020c72b4d"));
			os.put("UDebian-6-Standard-Multi-Size-Image-v.1.0.6", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-neuca-v1.0.7.xml", 
					"ba15fa6f56cc00d354e505259b9cb3804e1bcb73"));
			os.put("Delay.v1", new OSdata("http://geni-images.renci.org/images/tqian/u64_32_delay.xml", 
					"37be17f937c259d2068ad59060a745c033aa3145"));
			os.put("UDocker-v0.1", new OSdata("http://geni-images.renci.org/images/ibaldin/docker/centos6.6-docker-v0.1/centos6.6-docker-v0.1.xml", 
					"b2262a8858c9c200f9f43d767e7727a152a02248"));
			os.put("Fedd-enabled Ubuntu 12.04", new OSdata("http://www.isi.edu/~faber/tmp/fedd.xml", 
					"05cf5d86906c11cdb35ece535d2539fe38481d17"));
			os.put("Fedora 22", new OSdata("http://geni-images.renci.org/images/standard/fedora/fedora22-v1.0.xml", 
					"4fdd820d481f9afe8b9a48ec53dc54d50982d266"));
			os.put("GIMI", new OSdata("http://emmy9.casa.umass.edu/Disk_Images/ExoGENI/exogeni-umass-1.2.xml", 
					"49f0c193cc91d7b2fc1a6f038427935f4c296a8a"));
			os.put("Hadoop 2.7.1 (Centos7)", new OSdata("http://geni-images.renci.org/images/pruth/standard/hadoop/Hadoop-Centos7-v0.1/hadoop-centos7.v0.1.1.xml", 
					"af212901b35c96e1b2abed7a937882fcae81a513"));
			os.put("OpenDaylight", new OSdata("http://geni-images.renci.org/images/cisco-demos/opendaylight-1.0.0.xml", 
					"9b1001c38f203522b1a3cec15b675243b567cc71"));
			os.put("Ubuntu 12.04", new OSdata("http://emmy9.casa.umass.edu/Disk_Images/ExoGENI/Ubuntu12.04-1.0.2/ubuntu12.04-1.0.2.xml", 
					"8ee8735fa6a4f102313f7a29f6fd0918b8ed5fc4"));
			os.put("Ubuntu 13.04 + OVS + OpenDaylight", new OSdata("http://geni-images.renci.org/images/standard/ubuntu/ub1304-ovs-opendaylight-v1.0.0.xml", 
					"608a5757ccb2bbe3b3bb5c85e8fa1f2c3e712258"));
			os.put("perfSonar-v0.3", new OSdata("http://geni-images.renci.org/images/ibaldin/perfSonar/psImage-v0.3/psImage-v0.3.xml", 
					"e45a2c809729c1eb38cf58c4bff235510da7fde5"));
		
			OSMap = Collections.unmodifiableMap(os);
		}
		
		
		/**
		 * Convert netmask string to an integer (24-bit returned if no match)
		 * @param nm
		 * @return
		 */
		public static int netmaskStringToInt(String nm) {
			int i = 1;
			for(String s: netmaskConverter) {
				if (s.equals(nm))
					return i;
				i++;
			}
			return 24;
		}
		
		/**
		 * Convert netmask int to string (255.255.255.0 returned if nm > 32 or nm < 1)
		 * @param nm
		 * @return
		 */
		public static String netmaskIntToString(int nm) {
			if ((nm > 32) || (nm < 1)) 
				return "255.255.255.0";
			else
				return netmaskConverter[nm - 1];
		}
	
	public static Element getTermDuration(int days, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#TermDuration");
		atr_about.setNamespace(ns_rdf);
        e.setAttribute(atr_about);
        Element time = new Element("days");
        time.setNamespace(ns_time);
        Attribute atr_datatype = new Attribute("datatype", "http://www.w3.org/2001/XMLSchema#decimal");
		atr_datatype.setNamespace(ns_rdf);
		time.setAttribute(atr_datatype);
        time.setText(Integer.toString(days));
        Element type = new Element("type");
        type.setNamespace(ns_rdf);
        Attribute atr_type = new Attribute("resource", "http://www.w3.org/2006/time#DurationDescription");
		atr_type.setNamespace(ns_rdf);
        type.setAttribute(atr_type);

        e.addContent(time);
        e.addContent(type);
        
        return e;
	}
	
	public static Element getTerm(String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#Term");
		atr_about.setNamespace(ns_rdf);
        e.setAttribute(atr_about);
        Element has = new Element("hasDurationDescription");
        has.setNamespace(ns_time);
        Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#TermDuration");
		atr_resource.setNamespace(ns_rdf);
        has.setAttribute(atr_resource);
        Element type = new Element("type");
        type.setNamespace(ns_rdf);
        Attribute atr_type = new Attribute("resource", "http://www.w3.org/2006/time#Interval");
		atr_type.setNamespace(ns_rdf);
        type.setAttribute(atr_type);
        
        e.addContent(has);
        e.addContent(type);
        
        return e;
	}
	
	////get the description element. It describe the ip of the node, which interface connects to link
	public static Element getIP(String node, String link, String ip, String netmask, String guid){
		String dashIP = ip.replace('.', '-');
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node+"-ip-"+dashIP);
		atr_about.setNamespace(ns_rdf);
        e.setAttribute(atr_about);
        Element nm = new Element("netmask");
        nm.setNamespace(ns_ip4);
        nm.setText(netmask);
        Element layer = new Element("label_ID");
        layer.setNamespace(ns_layer);
        layer.setText(ip);
        Element type = new Element("type");
        type.setNamespace(ns_rdf);
        Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/ip4.owl#IPAddress");
		atr_resource.setNamespace(ns_rdf);
        type.setAttribute(atr_resource);
        
        e.addContent(nm);
        e.addContent(layer);
        e.addContent(type);
        
        return e;
	}
	
	public static Element getOS(String OS, String guid){
		String dashOS = OS.replace("+", "%2B").replace("(", "%28").replace(")", "%29").replace(" ", "+");
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+dashOS);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
	    
		Element name = new Element("hasName");
		name.setNamespace(ns_topology);
		Attribute atr_datatype = new Attribute("datatype", "http://www.w3.org/2001/XMLSchema#string");
		atr_datatype.setNamespace(ns_rdf);
	    name.setAttribute(atr_datatype);
	    name.setText(OS);
	    OSdata osd = OSMap.get(OS);
	    if(osd == null)
	    	osd = OSMap.get("Ubuntu 14.04");
	    Element imageUrl = new Element("hasURL");
	    imageUrl.setNamespace(ns_topology);
	    imageUrl.setText(osd.OSurl);
	    Element imageGuid = new Element("hasGUID");
	    imageGuid.setNamespace(ns_topology);
	    imageGuid.setText(osd.OSguid);
	    Element type = new Element("type");
	    type.setNamespace(ns_rdf);
	    Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/compute.owl#DiskImage");
	    atr_resource.setNamespace(ns_rdf);
	    type.setAttribute(atr_resource);
	    
	    e.addContent(name);
	    e.addContent(imageUrl);
	    e.addContent(imageGuid);
	    e.addContent(type);
	    
		return e;
	}
	
	public static Element getInterface(String link, String node, String ip, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		Element address = new Element("localIPAddress");
		address.setNamespace(ns_ip4);
		String dashIP = ip.replace('.', '-');
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node+"-ip-"+dashIP);
		atr_resource.setNamespace(ns_rdf);
		address.setAttribute(atr_resource);
		Element type = new Element("type");
		type.setNamespace(ns_rdf);
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/topology.owl#Interface");
		atr_resource2.setNamespace(ns_rdf);
		type.setAttribute(atr_resource2);
		
		e.addContent(address);
		e.addContent(type);
		
		return e;
	}
	
	public static Element getNode(String [] link, String node, String OS, String domain, String vmType, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+node);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		for(int i = 0 ; i<link.length ; i++){
			Element interFace = new Element("hasInterface");
			interFace.setNamespace(ns_topology);
			Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+link[i]+"-"+node);
			atr_resource.setNamespace(ns_rdf);
			interFace.setAttribute(atr_resource);
			e.addContent(interFace);
		}
		Element nodeGuid = new Element("hasGUID");
		nodeGuid.setNamespace(ns_topology);
		UUID uuid = UUID.randomUUID();
		nodeGuid.setText(uuid.toString());
		Element edm = new Element("inDomain");
		edm.setNamespace(ns_request_schema);
		String dm = domainMap.get(domain); 
		if(dm == null)
			dm = domainMap.get("UvA (Amsterdam, The Netherlands) XO Rack");
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+dm+"/Domain");
		atr_resource.setNamespace(ns_rdf);
		edm.setAttribute(atr_resource);
		
		Element diskImage = new Element("diskImage");
		diskImage.setNamespace(ns_compute);
		String dashOS = OS.replace("+", "%2B").replace("(", "%28").replace(")", "%29").replace(" ", "+");
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+dashOS);
		atr_resource2.setNamespace(ns_rdf);
		diskImage.setAttribute(atr_resource2);
		Element vm = new Element("specificCE");
		vm.setNamespace(ns_compute);
		Attribute atr_resource3 = new Attribute("resource", "http://geni-orca.renci.org/owl/exogeni.owl#"+vmType);
		atr_resource3.setNamespace(ns_rdf);
		vm.setAttribute(atr_resource3);
		Element hasType = new Element("hasResourceType");
		hasType.setNamespace(ns_domain);
		Attribute atr_resource4 = new Attribute("resource", "http://geni-orca.renci.org/owl/compute.owl#VM");
		atr_resource4.setNamespace(ns_rdf);
		hasType.setAttribute(atr_resource4);
		Element type = new Element("type");
		type.setNamespace(ns_rdf);
		Attribute atr_resource5 = new Attribute("resource", "http://geni-orca.renci.org/owl/compute.owl#ComputeElement");
		atr_resource5.setNamespace(ns_rdf);
		type.setAttribute(atr_resource5);
		
		e.addContent(nodeGuid);
		e.addContent(edm);
		e.addContent(diskImage);
		e.addContent(vm);
		e.addContent(hasType);
		e.addContent(type);
		
		return e;
	}
	
	public static Element getLink(String link, String []node, int bw, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+link);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		for(int i = 0 ; i<node.length ; i++){
			Element interFace = new Element("hasInterface");
			interFace.setNamespace(ns_topology);
			Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node[i]);
			atr_resource.setNamespace(ns_rdf);
			interFace.setAttribute(atr_resource);
			e.addContent(interFace);
		}
		Element layer = new Element("atLayer");
		layer.setNamespace(ns_layer);
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/ethernet.owl#EthernetNetworkElement");
		atr_resource2.setNamespace(ns_rdf);
		layer.setAttribute(atr_resource2);
		Element bandwidth = new Element("bandwidth");
		bandwidth.setNamespace(ns_layer);
		Attribute atr_datatype = new Attribute("datatype", "http://www.w3.org/2001/XMLSchema#integer");
		atr_datatype.setNamespace(ns_rdf);
		bandwidth.setAttribute(atr_datatype);
		bandwidth.setText(Integer.toString(bw));
		Element linkGuid = new Element("hasGUID");
		linkGuid.setNamespace(ns_topology);
		UUID uuid = UUID.randomUUID();
	    linkGuid.setText(uuid.toString());
	    Element type = new Element("type");
	    type.setNamespace(ns_rdf);
	    Attribute atr_resource3 = new Attribute("resource", "http://geni-orca.renci.org/owl/topology.owl#NetworkConnection");
		atr_resource3.setNamespace(ns_rdf);
	    type.setAttribute(atr_resource3);
		
	    e.addContent(layer);
	    e.addContent(bandwidth);
	    e.addContent(linkGuid);
	    e.addContent(type);
	    
		return e;
	}
	
	public static Element getDomain(String domain, String guid){
		String dm = domainMap.get(domain); 
		if(dm == null)
			dm = domainMap.get("UvA (Amsterdam, The Netherlands) XO Rack");
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+dm+"/Domain");
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		Element type = new Element("type");
		type.setNamespace(ns_rdf);
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/topology.owl#NetworkDomain");
		atr_resource.setNamespace(ns_rdf);
		type.setAttribute(atr_resource);
		
		e.addContent(type);
		
		return e;
	    	
	}
	
	public static Element getTopology(String [] links, String [] nodes, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#");
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		
		for(int i = 0 ; i<links.length ; i++){
			Element interFace = new Element("element");
			interFace.setNamespace(ns_collections);
			Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+links[i]);
			atr_resource.setNamespace(ns_rdf);
			interFace.setAttribute(atr_resource);
			e.addContent(interFace);
		}
		for(int i = 0 ; i<nodes.length ; i++){
			Element interFace = new Element("element");
			interFace.setNamespace(ns_collections);
			Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+nodes[i]);
			atr_resource.setNamespace(ns_rdf);
			interFace.setAttribute(atr_resource);
			e.addContent(interFace);
		}
		
		Element term = new Element("hasTerm");
		term.setNamespace(ns_request_schema);
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#Term");
		atr_resource.setNamespace(ns_rdf);
		term.setAttribute(atr_resource);
		Element reservation = new Element("type");
		reservation.setNamespace(ns_rdf);
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/request.owl#Reservation");
		atr_resource2.setNamespace(ns_rdf);
		reservation.setAttribute(atr_resource2);
		
		e.addContent(term);
		e.addContent(reservation);
		
		return e;
	    	
	}
	
	
	public String generateINDL(ArrayList<Node> nodeSet, 
			ArrayList<Connection> linkSet, int validDay){
		
		
		if(linkSet == null)
			linkSet = new ArrayList<Connection>();
		
		
		Element root = new Element("RDF");
		UUID uuid = UUID.randomUUID();
		String guid = uuid.toString();
		
		ns_request = Namespace.getNamespace("request", "http://geni-orca.renci.org/owl/"+guid+"#");
		root.setNamespace(ns_rdf);
		root.addNamespaceDeclaration(ns_rdf);
		root.addNamespaceDeclaration(ns_appcolor);
		root.addNamespaceDeclaration(ns_collections);
		root.addNamespaceDeclaration(ns_compute);
		root.addNamespaceDeclaration(ns_domain);
		root.addNamespaceDeclaration(ns_dtn);
		root.addNamespaceDeclaration(ns_ec2);
		root.addNamespaceDeclaration(ns_ethernet);
		root.addNamespaceDeclaration(ns_eucalyptus);
		root.addNamespaceDeclaration(ns_exogeni);
		root.addNamespaceDeclaration(ns_geni);
		root.addNamespaceDeclaration(ns_ip4);
		root.addNamespaceDeclaration(ns_kansei);
		root.addNamespaceDeclaration(ns_layer);
		root.addNamespaceDeclaration(ns_modify_schema);
		root.addNamespaceDeclaration(ns_openflow);
		root.addNamespaceDeclaration(ns_orca);
		root.addNamespaceDeclaration(ns_owl);
		root.addNamespaceDeclaration(ns_planetlab);
		root.addNamespaceDeclaration(ns_rdfs);
		root.addNamespaceDeclaration(ns_request);
		root.addNamespaceDeclaration(ns_request_schema);
		root.addNamespaceDeclaration(ns_time);
		root.addNamespaceDeclaration(ns_topology);
		root.addNamespaceDeclaration(ns_xsd);
		
		Element termDuration = getTermDuration(validDay, guid);
		Element term = getTerm(guid);
		root.addContent(term);
		root.addContent(termDuration);
		for(int i = 0 ; i<nodeSet.size() ; i++){
			Node tmp = nodeSet.get(i);
			String tmpLinks [] = new String[tmp.eths.size()];
			for(int j = 0 ; j<tmp.eths.size() ; j++){
				Eth tmpEth = tmp.eths.get(j);
				if(tmpEth.connectionOrSubnet)
				{
					int pointIndex = tmpEth.connectionName.lastIndexOf('.');
					String linkName = tmpEth.connectionName.substring(0, pointIndex);
					Element ip = getIP(tmp.nodeName, linkName, tmpEth.privateAddress, tmpEth.netmask, guid);
					Element interFace = getInterface(linkName, tmp.nodeName, tmpEth.privateAddress, guid);
					root.addContent(ip);
					root.addContent(interFace);
					tmpLinks[j] = linkName;
				}
			}
			Element nodeInfo = getNode(tmpLinks, tmp.nodeName, tmp.OStype, tmp.domain, tmp.nodeType, guid);
			root.addContent(nodeInfo);
		}
		
		ArrayList<String> OSList = new ArrayList<String>();
		ArrayList<String> domainList = new ArrayList<String>();
		for(int i = 0 ; i<nodeSet.size() ; i++){
			String tmpOS = nodeSet.get(i).OStype;
			String tmpDomain = nodeSet.get(i).domain;
			boolean findOS = false;
			boolean findDomain = false;
			for(int j = 0; j<OSList.size() ; j++){
				if(tmpOS.equals(OSList.get(j))){
					findOS = true;
					break;
				}
			}
			if(!findOS)
				OSList.add(tmpOS);
			for(int j = 0 ; j<domainList.size() ; j++){
				if(tmpDomain.equals(domainList.get(j))){
					findDomain = true;
					break;
				}
			}
			if(!findDomain)
				domainList.add(tmpDomain);
		}
		for(int i = 0 ; i<OSList.size() ; i++){
			Element os = getOS(OSList.get(i), guid);
			root.addContent(os);
		}
		for(int i = 0 ; i<domainList.size() ; i++){
			Element domain = getDomain(domainList.get(i), guid);
			root.addContent(domain);
		}
		
		////get Links
		for(int i = 0 ; i<linkSet.size() ; i++){
			String nodes [] = new String[2];
			Connection tmpLink =linkSet.get(i);
			nodes[0] = tmpLink.source.nodeName;
			nodes[1] = tmpLink.target.nodeName;
			Element link = getLink(tmpLink.connectionName, nodes, tmpLink.bandwidth, guid);
			root.addContent(link);
		}
		
		String allLink [] = new String[linkSet.size()];
		String allNode [] = new String[nodeSet.size()];
 		for(int i = 0 ; i<nodeSet.size() ; i++)
 			allNode[i] = nodeSet.get(i).nodeName;
 		for(int i = 0 ; i<linkSet.size() ; i++)
 			allLink[i] = linkSet.get(i).connectionName;
		Element topology = getTopology(allLink, allNode, guid);
		root.addContent(topology);
		
        Document Doc = new Document(root);
        
        XMLOutputter XMLOut = new XMLOutputter();
        return XMLOut.outputString(Doc);
	}
	
	
	///return a set of public address pair. nodeName::PublicIP
	public static ArrayList<String> getPublicIPs(String status){
		ArrayList<String> publicIPs = new ArrayList<String>();
		SAXBuilder saxBuilder = new SAXBuilder();  
		try {
			StringReader read = new StringReader(status);
			InputSource source = new InputSource(read);
			Document doc = saxBuilder.build(source);
			Element root = doc.getRootElement();
			List<Element> children = root.getChildren("Description", ns_rdf);
			for(int i = 0 ; i<children.size() ; i++){
				Element cur = children.get(i);
				Attribute test = cur.getAttribute("about", ns_rdf);
				if(test != null){
					String atrS = test.toString();
					if(atrS.contains("/Service")){
						int begin = atrS.indexOf("#");
						int end = atrS.lastIndexOf("/S");
						String nodeName = atrS.substring(begin+1, end);
						Element son = cur.getChild("managementIP", ns_j16);
						String publicIP = son.getText();
						publicIPs.add(nodeName+"::"+publicIP);
					}
				}
			}
		} catch (JDOMException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 return publicIPs;
	}

}
