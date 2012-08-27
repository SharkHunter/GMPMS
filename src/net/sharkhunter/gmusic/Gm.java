package net.sharkhunter.gmusic;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.*;
import java.net.*;
import java.io.*;

import javax.net.ssl.HttpsURLConnection;

import net.pms.PMS;

import java.math.*;
import java.util.HashMap;

public class Gm {
	// Public fields sort of configuration
	public String savePath;
	public static final int DefaultDisplayLimit=32;
	public static final int DefaultDownloadDelay=3000;
	private static final String agentString="Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8 (.NET CLR 3.5.30729)";
	private static final String MMagentString="Music Manager (1, 0, 24, 7712 - Windows)";
	
	public int delay;
	public static int DisplayLimit;
	public static boolean zero_fill;
	
	// Fields
	private String token;
	private boolean save;
	// Generated fields
	
	public String initError;
	
	// Google music
	// Authentication URLs
	private static final String AUTH_URL = "https://www.google.com/accounts/ClientLogin";
	private static final String PLAY_URL = "https://play.google.com/music/listen?u=0&hl=en";
	private static final String ISSUE_URL = "https://www.google.com/accounts/IssueAuthToken";
	private static final String TOKEN_URL = "https://www.google.com/accounts/TokenAuth";
	// Main url
	private static final String BASE_URL = "https://play.google.com/music/";
	
	
	private String auth;
	private String sid;
	private String lsid;
	
	
	// Constructors
	
	public Gm(String name,String pwd) {
		try {
			initError=null;
			save=false;
			zero_fill=false;
			delay=DefaultDownloadDelay;
			DisplayLimit=DefaultDisplayLimit;
			CookieManager manager = new CookieManager();
	        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
	        CookieHandler.setDefault(manager);
			URL url=new URL(AUTH_URL);
			HttpsURLConnection conn =(HttpsURLConnection)url.openConnection();
			HttpsURLConnection.setFollowRedirects(true);
			conn.setInstanceFollowRedirects(true);
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);	
			conn.setRequestProperty("User-Agent",agentString);
			// Construct data
			String data="Email="+name+"&Passwd="+pwd+"&accountType=GOOGLE&service=sj";
			String page=doPost(conn,data);
			String[] lines=page.split("\r");
			for(int i=0;i<lines.length;i++) {
				String[] t=lines[i].split("=");
				if(t.length<2)
					continue;
				if(t[0].equals("SID"))
					sid=t[1];
				if(t[0].equals("LSID"))
					lsid=t[1];
				if(t[0].equals("Auth"))
					auth=t[1];
			}
			// Second login step
			url=new URL(ISSUE_URL);
			conn =(HttpsURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);	
			conn.setRequestProperty("User-Agent",MMagentString);
			data="SID="+escape(sid)+"&LSID="+escape(lsid)+"&service=gaia";
			page=doPost(conn,data);
			//getCookies(conn);
			token=page.trim();
			data="auth="+escape(token)+"&service=sj&source=jumper&continue="+escape(PLAY_URL);
			url=new URL(TOKEN_URL+"?"+data);
			conn =(HttpsURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.setDoOutput(true);	
			conn.setRequestProperty("User-Agent",MMagentString);
			page=fetchPage(conn);
			// finally get some more cookies
			url=new URL(PLAY_URL);
			conn =(HttpsURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.setDoOutput(true);	
			conn.setRequestProperty("User-Agent",MMagentString);
			}
		catch (Exception e) {
			initError="exception during init "+e;
			return ;
		}
	}
	
	private String escape(String str) {
		try {
			return URLEncoder.encode(str,"UTF-8");
		} catch (Exception e) {
			
		}
		return str;
	} 

	// JSON functions
	
	public String jsonString(String key,String val) {
		return "\""+key+"\":\""+val+"\"";
	}
	
	public String jsonBlock(String name,String data) {
		return "\""+name+"\":{"+data+"}";
	}
	
	private String deJsonify(String str) {
		return str.replaceAll("\"","");
	}
	
	public String jsonHeader(String param) {
		return "json={"+param+"}";
	}
	
	private String doPost(URLConnection connection,String q) {
		try {
		//Send request
			
				DataOutputStream wr = new DataOutputStream (
						connection.getOutputStream());
				wr.writeBytes(q);
				wr.flush ();
				wr.close ();
			

      //Get Response
		InputStream is = connection.getInputStream();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuffer response = new StringBuffer();
		while((line = rd.readLine()) != null) {
			response.append(line);
			response.append('\r');
		}
		rd.close();
		return decode(response.toString());
	}
	catch (Exception e) {
		error("exceptiuon "+e);
	    return "";
	}
	}
	

	// Used to get inital gws page to retrive session id
	private String fetchPage(URLConnection connection) {
		try {
			connection.setRequestProperty("User-Agent",agentString);
			connection.setDoInput(true);
			connection.setDoOutput(true);	
			
			//Send request
			/*DataOutputStream wr = new DataOutputStream (
					connection.getOutputStream ());
			wr.writeBytes ("");
			wr.flush ();
			wr.close ();*/

	      //Get Response	
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
		    return response.toString();
		}
		catch (Exception e) {
			error("fetch page internal "+e);
		    return "";
		}
	}
	
	// Convert the hash to a hex string
	private static String toHex(byte[] bytes) {
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "x", bi);
	}

	
	public String request(String param,String method) {	
		return request(param,method,"services/");
	}
	
	public String request(String param,String method,String endpoint) {
		try {
			URL url=new URL(calcUrl(method,endpoint,param));
			HttpsURLConnection conn =(HttpsURLConnection)url.openConnection();
			HttpsURLConnection.setFollowRedirects(true);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			String page;
			if(method.equals("play")) {
				conn.setRequestMethod("GET");
				page=fetchPage(conn);
			}
			else {
				conn.setRequestMethod("POST");
				page=doPost(conn,"");
			}
			return deJsonify(page);
		}
		catch (Exception e) {
			PMS.debug("request page "+e);
			return "";
		}
	}
	
	public String getCookie(String cookie) {	
		CookieManager manager=(CookieManager) CookieHandler.getDefault();
		 CookieStore cookieJar =  manager.getCookieStore();	
        List <HttpCookie> cookies =
            cookieJar.getCookies();
        for(HttpCookie c : cookies)
        	if(c.getName().equals(cookie))
        		return c.getValue();
        return "";
	}
	
	private String calcUrl(String method,String endpoint,String param) {
		String xt;
		if(method.equals("play"))
			xt="pt=e";
		else
			xt="xt="+getCookie("xt");
		return BASE_URL+endpoint+method+"?u=0&"+xt+(param.length()==0?"":"&"+param);
	}
	
	private String ucFirst(String str) {
		char first=str.charAt(0);
		return String.valueOf(first).toUpperCase()+str.substring(1);
	}
	
	public String search(String str) {
		return search(str,"Songs");
	}
	public String search(String str,String type) {
		String param=jsonString("query",str)+","+jsonString("type",ucFirst(type));
		return request(param,"getResultsFromSearch");
	}
	
	public String tinySearch(String str) {
		str=str.replace(' ', '+');
		try {
			URL url=new URL("http://www.tinysong.com/s/"+str+"?format=json&limit="+
					DefaultDisplayLimit);
			String page=fetchPage(url.openConnection());
			return deJsonify(page);
		}
		catch (Exception e) {
			error("tiny serached failed "+e);
			return "";
		}
	}
	
	public static String[] jsonSplit(String str) {
		return str.split(",");
	}
	
	public static String getField(String[] list,String field) {
		for(int i=0;i<list.length;i++) {
			String[] s=list[i].split(":");
			if(s[0].compareToIgnoreCase(field)==0)
				if(s.length>1)
					return s[1];
		}
		return "";
	}
	
	private String decode(String s) {
		//\u00e4
		String x= s.replaceAll("\\u00e4", "ä").replaceAll("\\u00e5", "å").replaceAll("\\u00e6", "ö");
		//debug("s "+s+" x "+x);
		return x;
	}
	
	// Other external functions
	
	public static String getField(Matcher m,String[] order,String field) {
		for(int i=0;i<order.length;i++) {
			if(order[i].compareTo(field)==0) { 
				if(i+1>m.groupCount())
					return "";
				return m.group(i+1);
			}
		}
		return "";
	}
	
	public void setPath(String path) {
		this.savePath=path;
	}
	
	public boolean saveSong() {
		return save;
	}
	
	public void setSave(boolean b) {
		save=b;
	}
	
	// Cover handling
	public String cover(String dataURL) {
		return "http:"+dataURL;
	}
	
	public void error(String msg) {
		PMS.minimal(msg);
	}
	
	public static boolean more(int i) {
		return (Gm.DisplayLimit!=0)&&(i>Gm.DisplayLimit);
	}
	
	public static void main(String args[]) {
		int i;
		Gm g =new Gm("","");
		g.setPath("c:\\gm_tst");
		g.setSave(true);
		String gpage=g.request("", "loadalltracks");
	//	System.out.println("gpage "+gpage);
		GmSong[] songs1=GmSong.parseSongs(gpage, g);
		for(i=0;i<songs1.length;i++)
			System.out.println("song "+songs1[i].toString());
		System.out.println("total songs "+songs1.length);
		/*GmSong s=songs1[0];
		s.fetchStreamData();
		System.out.println("stream url "+s.streamURL());
		s.download();*/
		String ppage=g.request("all", "loadplaylist");
		System.out.println("ppage "+ppage);
		GmPlaylist[] pls=GmPlaylist.parsePlaylist(ppage, g);
		for(i=0;i<pls.length;i++)
			System.out.println("pls "+pls[i].toString());
		GmSong[] s2=pls[3].getSongs();
		for(i=0;i<s2.length;i++)
			System.out.println("song "+s2[i].toString());
		String apage=g.request("", "loadalbums");
		System.out.println("apage "+apage);
		String rpage=g.request("", "loadartists");
		System.out.println("rpage "+rpage);	
		System.out.println("all done");
		
	}
	
}