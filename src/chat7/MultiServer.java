package chat7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;


public class MultiServer{
	Connection conn;
	PreparedStatement ps;
	ResultSet rs;
	String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
	String ORACLE_URL = "jdbc:oracle:thin://@localhost:1521:orcl";
	
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	//클라이언트 정보 저장을 위한 Map컬렉션
	Map<String, PrintWriter> clientMap;
		
	//생성자
	public MultiServer() {
		
		//클라이언트의 이름과 출력스트림을 저장할 HashMap생성
		clientMap = new HashMap<String, PrintWriter>();
		Collections.synchronizedMap(clientMap);		
		
	}
	//서버 초기화
	public void init() {
		
		try {
			Class.forName(ORACLE_DRIVER);	
			conn = DriverManager.getConnection(ORACLE_URL, "kosmo", "1234");
			System.out.println("DB연결 완료");
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
						
			while(true) {
				socket = serverSocket.accept();
				Thread mst = new MultiServerT(socket);
				mst.start();				
			}
		}
		catch (ClassNotFoundException e) {
			System.out.println("클래스가 없습니다.");
		}
		catch (SQLException e) {
			System.out.println("DB접속 실패");
		}
		
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			}
			
			catch(Exception e){
				e.printStackTrace();
			}
			}		
	}
	
	public void close() {
		try {
			if(conn!=null) conn.close();
			if(ps!=null) ps.close();
			if(rs!=null) rs.close();
		}
		
		catch (Exception e) {
			System.out.println("자원반납오류 발생");
			e.printStackTrace();
		}
	}
	
	public String utf8(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return str;
		}
	}
	
	//메인메소드 : Server객체를 생성한 후 초기화한다.
	public static void main(String[] args) {
		
		MultiServer ms = new MultiServer();
		ms.init();	
		
	}
	
	public ArrayList<String> callBlock(String name) throws SQLException{
		
		ArrayList<String> blockArr = new ArrayList<>();
		
		String sql = "select block from chatting_user_tb where name = ?";
		
		ps = conn.prepareStatement(sql);
		
		ps.setString(1, name);
		
		rs = ps.executeQuery();
		
		while(rs.next()) {
			blockArr.add(rs.getString(1));
		}
		
		return blockArr;
	}

	
	//접속된 모든클라이언트에게 메세지를 전달하는 역할의 메소드
	public void sendAllMsg(String name, String msg) 
	{
		//Map에 저장된 객체의 키값을 먼저 얻어온다. 
		Iterator<String> it = clientMap.keySet().iterator();
		
		//저장된 객체(클라이언트)의 갯수만큼 반복한다.
		while(it.hasNext()) {
			try {
				//각 클라이언트의 PrintWriter객체를 얻어온다.
				String name1 = it.next();
				
				ArrayList<String> blockArr = callBlock(name1);
				
				PrintWriter it_out = 
						(PrintWriter)clientMap.get(name1);
				
				int cnt=0;
				
				for(String block : blockArr) {
					
					if(name.equals(block)) {
						cnt++;
					}
				}
				if(cnt==0) {
					it_out.println(URLEncoder.encode("[" + name + "]:" + msg, "UTF-8"));
				}
				else {
					it_out.println(URLEncoder.encode("차단유저", "UTF-8"));
				}
				
				
				
				
				
				//name이 없는 경우는 없도록 처리했음
//				if(name.equals("")) {
//					it_out.println(URLEncoder.encode(msg,"UTF-8"));
//				}
//				else {
//				}
			}			
			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			catch (NullPointerException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				System.out.println("예외1:" + e);
				e.printStackTrace();
			}
		}
	}
	
	
	class MultiServerT extends Thread{
		//클라이언트로부터 전송된 "대화명"을 저장할 변수
		String name = "";
		//메세지 저장용 변수
		String s = "";
		//멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		//생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(),
						true);
				in = new BufferedReader(new
						InputStreamReader(this.socket.getInputStream(), "UTF-8")
						);			
			}
			
			catch (Exception e) {
				System.out.println("예외2:" + e);
			}
		}
		
		public void whisper(StringTokenizer str, String to) {
			Iterator<String> it2 = clientMap.keySet().iterator();
			
			
			while(it2.hasNext()) {
				try {
					//각 클라이언트의 PrintWriter객체를 얻어온다.
					String find_name = it2.next();
					PrintWriter it_out = 
							(PrintWriter)clientMap.get(find_name);
					
					if(to.equals(find_name)) {
						String msg="";
						while(str.hasMoreTokens()) {
							msg += str.nextToken() + " ";
						}
						it_out.println(URLEncoder.encode("[" + name + "님의 귓속말]:" + msg,"UTF-8"));
					}
				}			
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} 			
				catch (Exception e) {
					System.out.println("예외4:" + e);
				}
			}
		}
		
		public void list() {
			try {
				out.println(URLEncoder.encode("========현재접속 중인 이용자=========", "UTF-8"));
				Iterator<String> it1 = clientMap.keySet().iterator();
				int cnt = 0;
				while(it1.hasNext()) {
					cnt++;
					out.println(URLEncoder.encode(
							String.format("이용자%d : ", cnt) 
							+ it1.next(),
							"UTF-8"));
				}							
				out.println(URLEncoder.encode(String.format("현재 접속 중인 이용자는 총 %d명입니다.", cnt), "UTF-8"));
				out.println("=====================================");
				
			} catch (UnsupportedEncodingException e) {
				// TODO: handle exception
			}catch (Exception e) {
				// TODO: handle exception
			}
			
		}
		

		
		public void block(String block_name) throws SQLException{
			
			String sql1 = "select name from chatting_user_tb where name = ?";
			ps = conn.prepareStatement(sql1);
			ps.setString(1, block_name);
			rs = ps.executeQuery();
			int cnt = 0;
			while(rs.next()) {
				cnt++;
			}
			
			if(cnt>0) {
				String sql2 = "update chatting_user_tb \r\n" + 
						"set block = (select block from chatting_user_tb where name = ?)||','||?\r\n" + 
						"where name = ?";

				ps = conn.prepareStatement(sql2);
				ps.setString(1, name);
				ps.setString(2, block_name);
				ps.setString(3, name);
				ps.executeUpdate();
			}
			else {
				out.println("사용자 이름이 잘못되었습니다.");
			}
		}

		
		
		@Override
		public void run() {
			
			try {
				
				while(true) {
					try {
						name = in.readLine();
						name = URLDecoder.decode(name, "UTF-8");
						
						String sql = "insert into chatting_user_tb(serial, name) values(seq_user_serial.nextval, ?)";
						ps = conn.prepareStatement(sql);
						ps.setString(1, name);
						ps.executeUpdate();
						break;
					} catch (SQLException e) {
						System.out.println("중복 대화명 발생");
						out.println(utf8("중복된 대화명이 있습니다."));
					} 
				}

				
				//접속한 클라이언트에게 새로운 사용자의 입장을 알림.
				//접속자를 제외한 나머지 클라이언트만 입장 메세지를 받는다.
				sendAllMsg("", name + "님이 입장하셨습니다.");
				//입장 로그 남김(chatting_tb)
				String sql2 = "insert into chatting_tb values(seq_chat_num.nextval, ?, ?, sysdate)";

				ps = conn.prepareStatement(sql2);

				ps.setString(1, name);
				ps.setString(2, "입장");

				ps.executeUpdate();

				clientMap.put(name, out);

				//HashMap에 저장된 객체의 수로 접속자수를 파악할 수 있다.
				System.out.println(name + "접속");
				System.out.println("현재 접속자 수는"
						+ clientMap.size()+"명 입니다.");

				
				boolean whismode = false;

				String to_name="";
				while(in!=null) {
					
					s = in.readLine();					
					s = URLDecoder.decode(s, "UTF-8");		
					System.out.println(s);
					
					//////////////////////DB저장/////////////////////////
					String sql = "insert into chatting_tb values(seq_chat_num.nextval, ?, ?, sysdate)";													
					ps = conn.prepareStatement(sql);					
					ps.setString(1, name);
					ps.setString(2, s);					
					ps.executeUpdate();
					///////////////////////////////////////////////////
					
					if(s==null)
						break;

					
					if(s.equals("/q")) {
						whismode = false;
						out.println(utf8("귓속말모드가 종료되었습니다."));
						continue;
					}
					
					//s를 Tokenizer 하여 명령어 부분과 받는사람 부분으로 분리한다.
					StringTokenizer str = new StringTokenizer(s);
					
					//whisper method에서 nextToken부터 출력하게 되어있음
					if(whismode == true) {
						whisper(str, to_name);
						continue;
					}
					
					String token = str.nextToken();
					
					if(s.charAt(0)=='/') {
						switch (token) {
						case "/list":
							
							list();
							
							break;
							
						case "/to":
							to_name = str.nextToken();
							if(s=="/to") {
								out.println(utf8("대상의 이름을 입력하세요."));
								continue;
							}
							
							Iterator<String> it2 = clientMap.keySet().iterator();
							boolean flag = false;
							while(it2.hasNext()) {
								if(to_name.equals(it2.next())) {
									flag = true;
									if(str.hasMoreTokens()) {
										whisper(str, to_name);
									}
									else {
										out.println(utf8("귓속말 모드로 대화합니다. 그만하려면 '/q' 를 입력하세요"));
										whismode = true;								
									}
								}
							}
							
							if(flag==false) {
								out.println(utf8("사용자가 없습니다."));
							}
							break;
						
						case "/block":
							//block user user_table에 저장
							String block_name = str.nextToken();
							block(block_name);
							break;
						default:
							break;
						}
						
					}else {
						System.out.println(name + ">>" + s);
						sendAllMsg(name, s);
						
					}
					
				}	
								
			}
			catch (NullPointerException e) {
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				System.out.println("예외3:" + e);
				e.printStackTrace();
			
			}
			finally {
				/*
				 클라이언트가 접속을 종료하면 예외가 발생하게 되어 finally로
				 넘어오게 된다. 이때 "대화명"을 통해 remove()시켜준다.				 
				 */
				clientMap.remove(name);
				sendAllMsg("", name + "님이 퇴장하셨습니다.");
				
				try {
					String sql = "insert into chatting_tb values(seq_chat_num.nextval, ?, ?, sysdate)";
					
					ps = conn.prepareStatement(sql);
					
					ps.setString(1, name);
					ps.setString(2, "퇴장");
					
					ps.executeUpdate();
				} catch (Exception e) {
					// TODO: handle exception
				}
				
				System.out.println(name + "[" + 
				Thread.currentThread().getName() + "] 퇴장");
				//퇴장하는 클라이언트의 쓰레드명을 보여준다.
				System.out.println("현재 접속자 수는"
						+ clientMap.size() + "명 입니다.");
				try {
					in.close();
					out.close();
					socket.close();
					close();
				} 
				
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
