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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;


public class MultiServer{
	Connection conn;
	PreparedStatement ps;
	ResultSet rs;
	String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
	String ORACLE_URL = "jdbc:oracle:thin://@localhost:1521:orcl";
	Scanner scan = new Scanner(System.in);
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
		
		String query = "select block_name from block_user where name = ?";
		
		ps = conn.prepareStatement(query);
		
		ps.setString(1, name);
		
		rs = ps.executeQuery();
		
		while(rs.next()) {
			String block = rs.getString(1);
			System.out.println(block);
			blockArr.add(block);
		}
		
		return blockArr;
	}
	
	public boolean checkBlock(String name1, String name2) throws SQLException {
		/*
		만약, name1(블록한 사람)이 말하는 경우에
		name2(블록대상)이 청자라면 name1의 리스트에 현재 iterating되고잇는 듣는사람 중 블록대상을
		찾아 메세지 송출 No
		
		만약, name1(블록한 사람)이 듣고 name2가 말하는 경우에
		name1이 iterating 대상으로  각 듣는사람마다 block list를 찾아
		list에 name2가 있으면 송출 X
		 */
		/*
		 name1은 블록리스트 추출대상, name2는 블록리스트 검사대상
		 */
		ArrayList<String> blockArr = callBlock(name1);
		Iterator<String> itr = blockArr.iterator(); //name1의 이터레이터
		
		boolean flag=false;
		
		while(itr.hasNext()) {
			if(itr.next().equals(name2)) {
				flag = true;
				return flag;
			}
		}
		
		return flag;
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
				
				PrintWriter it_out = 
						(PrintWriter)clientMap.get(name1);
				
				//현재 말하는 사람이 듣는사람의 리스트에 있나 확인, 있으면 들리지 않는다.
				boolean flag1 = checkBlock(name1, name);
				
				//현재 말하는 사람의 블록리스트에 있는 사람에게 송출X
				boolean flag2 = checkBlock(name, name1);
				
				//flag가 둘 중에 하나에라도 해당되면 송출 X
				
				if(flag1==false && flag2==false) {
					it_out.println(URLEncoder.encode("[" + name + "]:" + msg, "UTF-8"));
				}
				
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
	
	public void showArr(String[] arr) {
		for(int i=0; i<arr.length; i++) {
			System.out.printf("[%d] : %s\n", i, arr[i]);
			
		}
	}
	
	
	public void serverFunc(String cmd) {
		
		String[] cmdArr = cmd.split(" ");
		
		showArr(cmdArr);
		
		switch (cmdArr[0].toLowerCase()) {
		case "/help" :
			System.out.println("/bword : 금칙어 설정");
			break;
		case "/bword" :
			
			while(true) {
				
				System.out.println("금칙어 설정 메뉴");
				System.out.println("/setbword : 금칙어 추가");
				System.out.println("/delbword : 금칙어 삭제");
				System.out.println("/bwlist : 금칙어 보기");
				System.out.println("/u : 상위메뉴");

				String bwChoice = scan.nextLine();
				
				String[] bwCArr = bwChoice.split(" ");
				
				showArr(bwCArr);
				
				if(bwCArr[0].equals("/u")) break;
				
				switch(bwCArr[0]) {
				case "/setbword":
					if(bwCArr.length>2) {
						System.out.println("하나의 단어만 입력할 수 있습니다.");
					}
					else {
						setBadWord("admin", bwCArr[1]);
					}
					break;
				case "/delword" :
					if(bwCArr.length>2) {
						System.out.println("하나의 단어만 입력할 수 있습니다.");
					}
					else {
						delBadWord("admin", bwCArr[1]);
					}
					break;
				case "/bwlist":
					listBadWord("admin");
					break;
				default:
					break;
				}
				
			}
				
		default:
			break;
		}
	}
	
	//ServerFunction은 만들었는데 어떻게 적용해야하지?
	public void serverMenu() {
		
		System.out.println("명령어를 입력하세요. 명령어를 검색하려면 '/help' 를 입력하세요");
		System.out.print(">>");
		String cmd = scan.nextLine();
		serverFunc(cmd);
	}
	
	
	public void setBadWord(String name, String word) {
		
		String sql = "INSERT INTO badword VALUES(seq_bw_num.nextval, ?, ?)";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, name);
			ps.setString(2, word);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			System.out.println("badword 입력 중 에러발생!!");
		}
	}
	
	public void delBadWord(String name, String word) {
		
		String sql = "DELETE badword WHERE name = ? and word = ?";
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, word);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			System.out.println("badword");
		}
	}
	
	public void listBadWord(String name) {
		
		String sql = "";
		
		if(name.equals("admin")) {
			sql = "SELECT * FROM badword";
		}
		else {
			sql = "SELECT * FROM badword WHERE name = ?";
		}
		
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				String sequence = rs.getString(1);
				String user = rs.getString(2);
				String word = rs.getString(3);
				System.out.printf("%-3s | %-10s | %-10s", sequence, user, word);
			}
			
			
		} catch (SQLException e) {
			System.out.println("badword");
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
		
		public String checkBw(String sentence) {
			
			String sql = "SELECT word FROM badword WHERE name = ?";
			try {
				ArrayList<String> adminBw = new ArrayList<String>();
				ArrayList<String> userBw = new ArrayList<String>();
				
				//admin금칙어 체크
				ps = conn.prepareStatement(sql);
				ps.setString(1, "admin");
				rs = ps.executeQuery();
				while(rs.next()) {
					String word = rs.getString(1);
					if(sentence.contains(word)) {
						String rep = "";
						for(int i=0; i<word.length(); i++) {
							rep +="*";
						}
						sentence.replace(word, rep);
					}
				}
				
				//user금칙어 체크
				ps = conn.prepareStatement(sql);
				ps.setString(1, name);
				rs = ps.executeQuery();
				while(rs.next()) {
					String word = rs.getString(1);
					if(sentence.contains(word)) {
						String rep = "";
						for(int i=0; i<word.length(); i++) {
							rep +="*";
						}
						sentence.replace(word, rep);
					}
				}
				
				return sentence;
				
			} catch (SQLException e) {
				System.out.println("금칙어 체크 실패(sql오류)");
			}
			return sentence;
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

				String sql2 = "insert into block_user values(seq_block_num.nextval, ?, ?)";

				ps = conn.prepareStatement(sql2);
				ps.setString(1, name);
				ps.setString(2, block_name);
				ps.executeUpdate();
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

					s = checkBw(s);
					
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
						case "/help":
							out.println("/list : 현재 대화 참석자 리스트");
							out.println("/to 대화명 메세지 : 귓속말");
							out.println("/to 대화명 : 귓속말 고정");
							out.println("/bw 단어 : 금칙어 입력(단어)");
						
						case "/list":
							list();
							break;
						case "/to":
							to_name = str.nextToken();
							if(s.equals("/to")) {
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
