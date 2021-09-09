package org.example;/*
SPDX-License-Identifier: Apache-2.0
*/

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Vector;
import java.io.Console;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.*;
public class ClientApp {

	public static String path = "/home/adminuser/gopath/src/github.com/hyperledger/fabric/scripts/fabric-samples/cbfm/client/";

	public static void clientApp(Network network,String MspId, String CurrentUsername) throws Exception {

		Scanner scanner = new Scanner(System.in);
		String opt = "";

		/*
		* System Function Start
		*
		* */
		while (!opt.equals("0")) {

			System.out.println();
			System.out.println("[当前用户: "+CurrentUsername+" ]");
			System.out.println("===================");
			System.out.println(" ** 全局查询 --- 1");
			System.out.println(" ** 单项查询 --- 2");
			System.out.println(" ** 上传数据 --- 3");
			System.out.println(" ** 共享数据 --- 4");
			System.out.println(" ** 审计数据 --- 5");
			System.out.println(" ** 更新数据 --- 6");
			System.out.println(" ** 验证数据 --- 7");
			System.out.println(" ** 退出用户 --- 0");
			System.out.println("===================");
			System.out.println("请输入 : ");
			opt = scanner.nextLine();
			switch (opt){
				case "1"://全局查询
					while(!opt.equals("0")){
						System.out.println();
						System.out.println("[当前用户: "+CurrentUsername+" ]");
						System.out.println("===============================");
						System.out.println(" *** 查询全部用户 ----------- 1");
						System.out.println(" *** 查询全部云服务器 ------- 2");
						System.out.println(" *** 查询全部数据 ----------- 3");
						System.out.println(" *** 查询全部操作记录 ------- 4");
						System.out.println(" *** 查询全部共享记录 ------- 5");
						System.out.println(" *** 查询全部审计记录 ------- 6");
						//System.out.println(" *** 查询全部未删除数据 ----- 5");
						System.out.println(" *** 返回上级目录 ----------- 0");
						System.out.println("===============================");
						System.out.println("请输入 : ");
						opt = scanner.nextLine();
						switch (opt){
							case "1"://查询全部用户
								System.out.println(" *** 查询全部用户 ***");
								try{
									User.queryAllUser(network);
									System.out.println("查询成功!");
								}catch (Exception e){
									System.out.println("查询失败!");
									e.printStackTrace();
								}
								break;
							case "2"://查询全部云服务器
								System.out.println(" *** 查询全部云平台 ***");
								try{
									Cloud.queryAllCloud(network);
									System.out.println("查询成功!");
								}catch (Exception e){
									System.out.println("查询失败!");
									e.printStackTrace();
								}
								break;
							case "3"://查询全部数据
								System.out.println(" *** 查询全部数据 ***");
								try{
									Data.queryAllData(network);
									System.out.println("查询成功!");
								}catch (Exception e){
									System.out.println("查询失败!");
									e.printStackTrace();
								}
								break;
							case "4"://查询全部操作记录
								System.out.println(" *** 查询全部操作记录 ***");
								try{
									Operation.queryAllOperation(network);
									System.out.println("查询成功!");
								}catch (Exception e){
									System.out.println("查询失败!");
									e.printStackTrace();
								}
								break;
							case "5"://查询全部共享记录
								System.out.println(" *** 查询全部共享记录 ***");
								try{
									Share.queryAllShare(network);
									System.out.println("查询成功!");
								}catch (Exception e){
									System.out.println("查询失败!");
									e.printStackTrace();
								}
								break;
							case "6"://查询全部审计记录
								System.out.println(" *** 查询全部审计记录 ***");
								try{
									Audit.queryAllAudit(network);
									System.out.println("查询成功!");
								}catch (Exception e){
									System.out.println("查询失败!");
									e.printStackTrace();
								}
								break;
							case "0"://返回上级目录
								System.out.println(" *** 返回上级目录 ***");
								break;
							default:
								System.out.println("输入错误！请重新输入.");
								break;
						}
					}
					opt = "";
					break;
				case "2"://单项查询
					while(!opt.equals("0")){
						System.out.println();
						System.out.println("[当前用户: "+CurrentUsername+" ]");
						System.out.println("===============================");
						System.out.println(" *** 用户相关查询 --- 1");
						System.out.println(" *** 云相关查询 ----- 2");
						System.out.println(" *** 数据相关查询 --- 3");
						System.out.println(" *** 操作相关查询 --- 4");
						System.out.println(" *** 共享相关查询 --- 5");
						System.out.println(" *** 审计相关查询 --- 6");
						//System.out.println(" *** 操作记录查询 --- 4");
						System.out.println(" *** 返回上级目录 --- 0");
						System.out.println("===============================");
						System.out.println("请输入 : ");
						opt = scanner.nextLine();
						switch (opt){
							case "1"://用户相关查询
								while(!opt.equals("0")){
									System.out.println();
									System.out.println("[当前用户: "+CurrentUsername+" ]");
									System.out.println("===============================");
									System.out.println(" **** 查询用户信息 --- 1");
									System.out.println(" **** 查询ID信息 ----- 2");
									System.out.println(" **** 返回上级目录 --- 0");
									System.out.println("===============================");
									System.out.println("请输入 : ");
									opt = scanner.nextLine();
									switch (opt){
										case "1"://查询用户信息
											System.out.println(" *** 查询用户信息 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("用户MSP : ");
											String QueryUserMsp = scanner.nextLine();
											System.out.println("用户名 : ");
											String QueryUsername = scanner.nextLine();
											System.out.println("===============================");
											try{
												User.queryUser(network,QueryUserMsp,QueryUsername);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "2"://查询ID信息
											System.out.println(" *** 查询ID信息 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("用户ID : ");
											String QueryUserID = scanner.nextLine();
											System.out.println("===============================");
											try{
												User.queryID(network,QueryUserID);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "0"://返回上级目录
											System.out.println(" *** 返回上级目录 ***");
											break;
										default:
											System.out.println("输入错误！请重新输入.");
											break;
									}
								}
								opt="";
								break;
							case "2"://云相关查询
								while(!opt.equals("0")){
									System.out.println();
									System.out.println("[当前用户: "+CurrentUsername+" ]");
									System.out.println("===============================");
									System.out.println(" **** 查询云信息 ----- 1");
									System.out.println(" **** 返回上级目录 --- 0");
									System.out.println("===============================");
									System.out.println("请输入 : ");
									opt = scanner.nextLine();
									switch (opt){
										case "1":
											System.out.println(" *** 查询云服务器信息 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("云MSP : ");
											String QueryMSP = scanner.nextLine();
											System.out.println("云服务器 : ");
											String QueryCloud= scanner.nextLine();
											System.out.println("===============================");
											try{
												Cloud.queryCloud(network,QueryMSP,QueryCloud);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "0"://返回上级目录
											System.out.println(" *** 返回上级目录 ***");
											break;
										default:
											System.out.println("输入错误！请重新输入.");
											break;
									}
								}
								opt="";
								break;

							case "3"://数据相关查询
								while(!opt.equals("0")) {
									System.out.println();
									System.out.println("[当前用户: " + CurrentUsername + " ]");
									System.out.println("===============================");
									System.out.println(" **** 查询数据记录 --------- 1");
									System.out.println(" **** 查询某用户全部数据 --- 2");
									System.out.println(" **** 查询数据历史 --------- 3");
									System.out.println(" **** 返回上级目录 --------- 0");
									System.out.println("===============================");
									System.out.println("请输入 : ");
									opt = scanner.nextLine();
									switch (opt) {
										case "1"://查询数据长记录
											System.out.println(" *** 查询数据记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											String shortRecord = scanner.nextLine();
											System.out.println("===============================");
											try{
												String dataString = Data.queryData(network,shortRecord);
												TextOutput.jsonOutput("数据长记录",dataString);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											opt = "";
											break;
										case "2"://查询某用户全部数据
											System.out.println(" *** 查询某用户全部数据 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据拥有者ID : ");
											String OwnerId = scanner.nextLine();
											System.out.println("===============================");
											try{
												Data.querySbData(network,OwnerId);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "3"://查询数据历史
											System.out.println(" *** 查询数据历史 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											shortRecord = scanner.nextLine();
											System.out.println("===============================");
											try{
												Data.queryDataHistory(network,shortRecord);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											opt = "";
											break;
										case "0"://返回上级目录
											System.out.println(" *** 返回上级目录 ***");
											break;
										default:
											System.out.println("输入错误！请重新输入.");
											break;
									}
								}
								opt="";
								break;
							case "4" ://操作相关查询
								while(!opt.equals("0")) {
									System.out.println();
									System.out.println("[当前用户: " + CurrentUsername + " ]");
									System.out.println("===============================");
									System.out.println(" **** 查询操作记录 --------- 1");
									System.out.println(" **** 查询某数据操作记录 --- 2");
									System.out.println(" **** 返回上级目录 --------- 0");
									System.out.println("===============================");
									System.out.println("请输入 : ");
									opt = scanner.nextLine();
									switch (opt) {
										case "1"://查询操作记录
											System.out.println(" *** 查询数据记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											String shortRecord = scanner.nextLine();
											System.out.println("时间戳(e.g., 2020-01-01 01:01:01) : ");
											String timeStamp = scanner.nextLine();
											System.out.println("===============================");
											try{
												Operation.queryOperation(network,shortRecord,timeStamp);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "2"://查询某数据操作记录
											System.out.println(" *** 查询某数据操作记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											shortRecord = scanner.nextLine();
											System.out.println("===============================");
											try{
												Operation.queryDataOperation(network,shortRecord);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "0"://返回上级目录
											System.out.println(" *** 返回上级目录 ***");
											break;
										default:
											System.out.println("输入错误！请重新输入.");
											break;
									}
								}
								opt="";
								break;
							case "5" ://共享相关查询
								while(!opt.equals("0")) {
									System.out.println();
									System.out.println("[当前用户: " + CurrentUsername + " ]");
									System.out.println("===============================");
									System.out.println(" **** 查询共享记录 --------- 1");
									System.out.println(" **** 查询某数据共享记录 --- 2");
									System.out.println(" **** 返回上级目录 --------- 0");
									System.out.println("===============================");
									System.out.println("请输入 : ");
									opt = scanner.nextLine();
									switch (opt) {
										case "1"://查询操作记录
											System.out.println(" *** 查询数据记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											String shortRecord = scanner.nextLine();
											System.out.println("时间戳(e.g., 2020-01-01 01:01:01) : ");
											String timeStamp = scanner.nextLine();
											System.out.println("===============================");
											try{
												Share.queryShare(network,shortRecord,timeStamp);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "2"://查询某数据操作记录
											System.out.println(" *** 查询某数据操作记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											shortRecord = scanner.nextLine();
											System.out.println("===============================");
											try{
												Share.queryDataShare(network,shortRecord);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "0"://返回上级目录
											System.out.println(" *** 返回上级目录 ***");
											break;
										default:
											System.out.println("输入错误！请重新输入.");
											break;
									}
								}
								opt="";
								break;
							case "6"://审计相关查询
								while(!opt.equals("0")){
									System.out.println();
									System.out.println("[当前用户: "+CurrentUsername+" ]");
									System.out.println("====================================");
									System.out.println(" **** 查询审计记录 ------------- 1");
									System.out.println(" **** 查询某数据全部审计记录 --- 2");
									System.out.println(" **** 查询某数据最新审计记录 --- 3");
									System.out.println(" **** 返回上级目录 ------------- 0");
									System.out.println("====================================");
									System.out.println("请输入 : ");
									opt = scanner.nextLine();
									switch (opt){
										case "1"://查询审计记录
											System.out.println(" *** 查询审计记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											String shortRecord = scanner.nextLine();
											System.out.println("审计时间 (e.g., 2020-01-01 01:01:01) : ");
											String time = scanner.nextLine();
											System.out.println("===============================");
											try{
												Audit.queryAudit(network,shortRecord,time);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "2"://查询某数据全部审计记录
											System.out.println(" *** 查询某数据全部审计记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录  : ");
											String ShortDataRecord = scanner.nextLine();
											System.out.println("===============================");
											try{
												Audit.queryDataAudit(network,ShortDataRecord);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "3"://查询某数据最新审计记录
											System.out.println(" *** 查询某数据最新审计记录 ***");
											System.out.println(" -- 请输入 --");
											System.out.println("数据短记录 : ");
											ShortDataRecord = scanner.nextLine();
											System.out.println("===============================");
											try{
												Audit.queryDataLatestAudit(network,ShortDataRecord);
												System.out.println("查询成功!");
											}catch (Exception e){
												System.out.println("查询失败!");
												e.printStackTrace();
											}
											break;
										case "0"://返回上级目录
											System.out.println(" *** 返回上级目录 ***");
											break;
										default:
											System.out.println("输入错误！请重新输入.");
											break;
									}
								}
								opt="";
								break;

							case "0"://返回上级目录
								System.out.println(" *** 返回上级目录 ***");
								break;
							default:
								System.out.println("输入错误！请重新输入.");
								break;

						}
					}
					opt = "";
					break;
				case "3"://上传数据
					System.out.println(" *** 上传数据 ***");
					System.out.println(" -- 请输入 --");
					System.out.println("本地文件路径（e.g. /home/simba/data00) : ");
					String filePath = scanner.nextLine();
					System.out.println("云MSP : ");
					String MSP = scanner.nextLine();
					System.out.println("云名称 : ");
					String cloud = scanner.nextLine();
					System.out.println("数据名 : ");
					String DataName = scanner.nextLine();
					System.out.println("加密密钥（8个字符） : ");
					String SymKey = scanner.nextLine();
					System.out.println("文件访问属性（e.g. ((0 AND 1) OR (2 AND 3)) AND 5 ) : ");
					String Policy = scanner.nextLine();
					System.out.println("文件介绍 : ");
					String Introduction = scanner.nextLine();
					System.out.println("===============================");
					try{
						Data.uploadMetaData(network,MspId,DataName,CurrentUsername,MSP,cloud,filePath,SymKey,Policy,Introduction);
						System.out.println("上传数据成功!");
					}catch (Exception e){
						System.out.println("上传数据失败!");
						e.printStackTrace();
					}
					opt = "";
					break;
				case "4"://共享数据
					while (!opt.equals("0")){

						System.out.println();
						System.out.println("[当前用户: "+CurrentUsername+" ]");
						System.out.println("=========================");
						System.out.println(" *** 下载密文数据 --- 1");
						System.out.println(" *** 下载明文数据 --- 2");
						System.out.println(" *** 返回上级目录 --- 0");
						System.out.println("=========================");
						System.out.println("请输入 : ");
						opt = scanner.nextLine();
						switch (opt){
							case "1"://下载密文数据
								System.out.println(" *** 下载密文数据 ***");
								System.out.println(" -- 请输入 --");
								System.out.println("数据短记录 : ");
								String ShortDataRecord = scanner.nextLine();
								System.out.println("===============================");
								try{
									Data.downloadEncryptedData(network,ShortDataRecord);
									System.out.println("下载成功!");
								}catch (Exception e){
									System.out.println("下载失败!");
									e.printStackTrace();
								}
								break;
							case "2"://下载明文数据
								System.out.println(" *** 下载明文数据 ***");
								System.out.println(" -- 请输入 --");
								System.out.println("数据短记录 : ");
								ShortDataRecord = scanner.nextLine();
								System.out.println("===============================");
								try{
									Data.downloadDecryptedData(network,ShortDataRecord,CurrentUsername);
									System.out.println("下载成功!");
								}catch (Exception e){
									System.out.println("下载失败!");
									e.printStackTrace();
								}
								break;
							case "0"://返回上级目录
								break;
							default:
								System.out.println("输入错误！请重新输入.");
								break;
						}

					}
					opt = "";
					break;
				case "5"://审计数据
					System.out.println(" *** 审计数据 ***");
					System.out.println(" -- 请输入 --");
					System.out.println("数据短记录 : ");
					String ShortDataRecord = scanner.nextLine();
					System.out.println("审计数据块数量 : ");
					String C = scanner.nextLine();
					System.out.println("===============================");
					try{
						int c = Integer.valueOf(C);
						System.out.println("\n正在发起审计...");
						String auditString = Audit.genChallenge(network,ShortDataRecord,c);
						System.out.println("\n等待审计结果...");
						// START CHAINCODE EVENT LISTENER HANDLER----------------------
						String expectedEventName = MspId+"_"+CurrentUsername+"_VerifyEvent";
						Channel channel = network.getChannel();
						Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>(); // Test list to capture
						String chaincodeEventListenerHandle = Audit.setChaincodeEventListener(channel, expectedEventName, chaincodeEvents);
						// END CHAINCODE EVENT LISTENER HANDLER------------------------

						// START WAIT FOR THE EVENT-------------------------------------
						Audit.waitForChaincodeEvent(3000, channel, chaincodeEvents, chaincodeEventListenerHandle,MspId,CurrentUsername,network);
						System.out.println("审计成功!");
					}catch (Exception e){
						System.out.println("审计失败!");
						e.printStackTrace();
					}
					opt = "";
					break;
				case "6"://更新数据
					System.out.println(" *** 更新数据 ***");
					System.out.println(" -- 请输入 --");
					System.out.println("数据短记录 : ");
					String ShortRecord = scanner.nextLine();
					try {
						System.out.println("===============================================");
						System.out.println("若更新文件，请填写\"文件名\"。");
						System.out.println("若更新密钥，请填写\"加密密钥\"。");
						System.out.println("若更新访问策略，请填写\"文件访问属性\"。");
						System.out.println("若更新文件简介，请填写\"文件介绍\"。");
						System.out.println("无需更新的内容，请输入\"**\"。");
						System.out.println("===============================================");
						System.out.println("本地文件路径（e.g. /home/simba/data00) : ");
						filePath = scanner.nextLine();
						System.out.println("加密密钥（8个字符） : ");
						SymKey = scanner.nextLine();
						System.out.println("文件访问属性（e.g. ((0 AND 1) OR (2 AND 3)) AND 5 ) : ");
						Policy = scanner.nextLine();
						System.out.println("文件介绍 : ");
						Introduction = scanner.nextLine();
						System.out.println("===============================");
						Data.updateData(network,MspId,CurrentUsername,ShortRecord,filePath,SymKey,Policy,Introduction);
						System.out.println("更新成功!");
					}catch (Exception e){
						System.out.println("更新失败!");
						e.printStackTrace();
					}
					break;
				case "7"://验证数据
					System.out.println(" *** 验证数据签名 ***");
					System.out.println(" -- 请输入 --");
					System.out.println("数据短记录 : ");
					ShortRecord = scanner.nextLine();
					System.out.println("===============================");
					try {
						Cloud.verifySignature(network,ShortRecord);
						System.out.println("验证成功!");
					}catch (Exception e){
						System.out.println("验证失败!");
						e.printStackTrace();
					}
					break;
				case "0"://退出用户
					System.out.println(" *** 退出用户 ***");
					break;
				default:
					System.out.println("输入错误！请重新输入.");
					break;
			}
		}
		/*
		*System Function End
		*
		 */
	}
}
