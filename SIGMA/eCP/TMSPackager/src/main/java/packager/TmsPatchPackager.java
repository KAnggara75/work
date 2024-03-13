package packager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * 
 * @author Alpinthor
 *
 */
public class TmsPatchPackager {

	private static SVNClientManager ourClientManager;

	public static void main(String[] args) {

		String environment;

//		String environment = "trunk";
//		String environment = "sit";
//		String environment = "uat";
//		String environment = "production-staging1";
//		String environment = "production";

//		String pathSvnTrunk = "C:\\efts\\atm\\";
//		String pathSvnSit = "C:\\SVN\\telkomsigma\\jalin\\ecp_source_code\\atm-link\\branches\\SIT\\efts\\atm\\";
//		String pathSvnUat = "C:\\SVN\\telkomsigma\\jalin\\ecp_source_code\\atm-link\\branches\\UAT\\efts\\atm\\";
//		String pathSvnProdStaging = "C:\\SVN\\telkomsigma\\jalin\\ecp_source_code\\atm-link\\branches\\production-staging1\\efts\\atm\\";
//		String pathSvnProd = "C:\\SVN\\telkomsigma\\jalin\\ecp_source_code\\atm-link\\branches\\production\\efts\\atm\\";

		try {

			System.out.println("-= TASK START! =-");
			System.out.println();

			long start = System.nanoTime();

			environment = args[0];

			if (environment == null) {

				throw new Exception("environment args is null!!!");

			} else if (environment != null && environment.equalsIgnoreCase("manual")) {

				createPatchFromFilesFolder();

			} else {

				System.out.println("Environment : " + environment);

				clearTargetDirectory(environment);

				String revisionNumber = "0";
				Map<String, String> mapOfConfig = new HashMap<String, String>();

				mapOfConfig = setConfigurationFromConfigFile();

				revisionNumber = updateSvnTarget(environment, mapOfConfig);

				createPatchExcludeBlacklist(environment, revisionNumber, mapOfConfig);

//				createPatchWhitelist(environment, mapOfConfig);

			}

			long elapsedTime = (System.nanoTime() - start) / 1000000;

			String stringElapsedTime = new SimpleDateFormat("mm:ss:SSS").format(new Date(elapsedTime)).toString();

//			String stringElapsedTime = String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(elapsedTime),
//					TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
//							- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime)));

			System.out.println("-= TASK COMPLETED! =-");
			System.out.println("-= elapsed time : " + stringElapsedTime + " (mm:ss:SSS) =-");

		} catch (Exception e) {
			System.out.println("CREATE PACKAGE FAILED!");
		}

	}

	public static Map<String, String> setConfigurationFromConfigFile() throws Exception {

		System.out.println("--STEP 2-- setConfigurationFromConfigFile");

		Map<String, String> mapOfConfig = new HashMap<String, String>();

		List<String> listOfWhitelist = getRowAsListOfRows("C:\\tms_patch_packager\\config.txt");

		System.out.println("config file : " + "C:\\tms_patch_packager\\config.txt");
		for (Iterator iterator = listOfWhitelist.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			String[] config = string.split("=");

			String key = config[0];
			String value = config[1];

			System.out.println("key = " + key + ", value = " + value);

			if (key.equalsIgnoreCase("pathSvnTrunk")) {
				mapOfConfig.put("pathSvnTrunk", value);
			} else if (key.equalsIgnoreCase("pathSvnSit")) {
				mapOfConfig.put("pathSvnSit", value);
			} else if (key.equalsIgnoreCase("pathSvnUat")) {
				mapOfConfig.put("pathSvnUat", value);
			} else if (key.equalsIgnoreCase("pathSvnProdStaging")) {
				mapOfConfig.put("pathSvnProdStaging", value);
			} else if (key.equalsIgnoreCase("pathSvnProd")) {
				mapOfConfig.put("pathSvnProd", value);
			}

		}

		System.out.println("GET CONFIG FILE DONE!");
		System.out.println();

		return mapOfConfig;

	}

	public static String updateSvnTarget(String environment, Map<String, String> mapOfConfig) throws SVNException {

		System.out.println("--STEP 3-- updateSvnTarget");
		System.out.println();

		String revisionNumber = "";

		FSRepositoryFactory.setup();

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);

		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();

//		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("alpinthor.muthahari",
//				"Cherry1990!");

		ourClientManager = SVNClientManager.newInstance(options, authManager);

		File directory = null;

		if (environment.equalsIgnoreCase("trunk")) {

			directory = new File(mapOfConfig.get("pathSvnTrunk"));

		} else if (environment.equalsIgnoreCase("sit")) {

			directory = new File(mapOfConfig.get("pathSvnSit"));

		} else if (environment.equalsIgnoreCase("uat")) {

			directory = new File(mapOfConfig.get("pathSvnUat"));

		} else if (environment.equalsIgnoreCase("production-staging1")) {

			directory = new File(mapOfConfig.get("pathSvnProdStaging"));

		} else if (environment.equalsIgnoreCase("production")) {

			directory = new File(mapOfConfig.get("pathSvnProd"));

		}

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();

		updateClient.setIgnoreExternals(false);

		try {

			updateClient.doUpdate(directory, SVNRevision.HEAD, true);

			revisionNumber = String.valueOf(
					ourClientManager.getStatusClient().doStatus(directory, false).getCommittedRevision().getNumber());

		} catch (SVNException e) {
			e.printStackTrace();
			System.out.println("ERROR UPDATE SVN!");
			System.out.println();
			throw e;
		}

		System.out.println("UPDATE SVN DONE! rev " + revisionNumber);
		System.out.println();

		return revisionNumber;

	}

	public static void clearTargetDirectory(String environment) throws IOException {

		System.out.println("--STEP 1-- clearTargetDirectory");
		System.out.println();

		File directory = new File("C:\\tms_patch_packager\\tmp_packer_files\\");

		System.out.println("target directory : " + "C:\\tms_patch_packager\\tmp_packer_files\\");
		try {
			FileUtils.cleanDirectory(directory);
			System.out.println("DELETE TARGET DIRECTORY DONE!");
			System.out.println();
		} catch (IOException e) {
			System.out.println("DELETE TARGET DIRECTORY FAILED!");
			System.out.println();
			e.printStackTrace();
			throw e;
		}

	}

	public static void createPatchFromFilesFolder() throws Exception {

		try {
			// FOR PATCHING manual

			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			Date date = new Date();
			String zipName = "TMS_SCC_" + dateFormat.format(date) + "_.zip";

			writeTextLine("C:\\tms_patch_packager\\files\\script.csv",
					getListOfFilesFromFolder("C:\\tms_patch_packager\\files"));

			System.out.println("FILE script.csv CREATED!");
			packToZip("C:\\tms_patch_packager\\files\\", "C:\\tms_patch_packager\\patch\\" + zipName);
			System.out.println("FILE " + zipName + " CREATED!");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	public static void createPatchExcludeBlacklist(String environment, String revisionNumber,
			Map<String, String> mapOfConfig) throws Exception {

		System.out.println("--STEP 4-- createPatchExcludeBlacklist");
		System.out.println();

		try {
//			FOR PATCHING bundle all exclude blacklist

			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			Date date = new Date();

			String zipName = "";

			if (environment.equalsIgnoreCase("trunk")) {
				zipName = "TMS_VIT_" + dateFormat.format(date) + "_VIT_ALL_svn" + revisionNumber + "_v.zip";
			} else if (environment.equalsIgnoreCase("sit")) {
				zipName = "TMS_SCC_" + dateFormat.format(date) + "_SIT_ALL_svn" + revisionNumber + "_v.zip";
			} else if (environment.equalsIgnoreCase("uat")) {
				zipName = "TMS_U_" + dateFormat.format(date) + "_UAT_ALL_svn" + revisionNumber + "_v.zip";
			} else if (environment.equalsIgnoreCase("production-staging1")) {
				zipName = "TMS_PRODUCTION_" + dateFormat.format(date) + "_v.zip";
			} else if (environment.equalsIgnoreCase("production")) {
				zipName = "TMS_PRODUCTION_" + dateFormat.format(date) + "_v.zip";
			}

//			writeTextLine("C:\\tms_patch_packager\\files\\script.csv",
//					getRowAsListOfRows("C:\\tms_patch_packager\\files\\list.txt"));
			List<String> listOfBlacklist = getRowAsListOfRows("C:\\tms_patch_packager\\blacklist.txt");

//			List<String> newList = getListOfFilesFromAtmFolder(listOfBlacklist);
			writeTextLine("C:\\tms_patch_packager\\tmp_packer_files\\script.csv",
					getListOfFilesFromAtmFolderBlacklist(listOfBlacklist, environment, mapOfConfig));

			System.out.println("FILE script.csv CREATED!");
			packToZip("C:\\tms_patch_packager\\tmp_packer_files\\", "C:\\tms_patch_packager\\patch\\" + zipName);
			System.out.println("FILE " + zipName + " CREATED!");
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	public static void createPatchWhitelist(String environment, Map<String, String> mapOfConfig) throws Exception {

		try {
//			FOR PATCHING bundle white list

			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			Date date = new Date();
			String zipName = "TMS_SCC_" + dateFormat.format(date) + "_.zip";

//			writeTextLine("C:\\tms_patch_packager\\files\\script.csv",
//					getRowAsListOfRows("C:\\tms_patch_packager\\files\\list.txt"));
			List<String> listOfWhitelist = getRowAsListOfRows("C:\\tms_patch_packager\\whitelist_sprint1.txt");

//			List<String> newList = getListOfFilesFromAtmFolder(listOfBlacklist);
			writeTextLine("C:\\tms_patch_packager\\tmp_packer_files\\script.csv",
					getListOfFilesFromAtmFolderWhitelist(listOfWhitelist, environment, mapOfConfig));

			System.out.println("FILE script.csv CREATED!");
			packToZip("C:\\tms_patch_packager\\tmp_packer_files\\", "C:\\tms_patch_packager\\patch\\" + zipName);
			System.out.println("FILE " + zipName + " CREATED!");
			System.out.println();

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	public static ArrayList<String> getRowAsListOfRows(String filePath) throws Exception {
		ArrayList<String> listData = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			String line;
			while ((line = br.readLine()) != null) {
				listData.add(line);
			}
		} catch (Exception e) {
			br.close();
			throw e;
		}
		br.close();
		return listData;
	}

	public static void writeTextLine(String filePath, List<String> listString) throws Exception {
		try {
			FileWriter fw = new FileWriter(filePath);
			String path = "C:\\efts\\atm\\";
			String pathMasks = "C:\\efts\\atm\\masks\\";
			String pathPics = "C:\\efts\\atm\\pics\\";

			boolean isIncludeAtmExe = false;

			for (int i = 0; i < listString.size(); i++) {
				String filename = listString.get(i);

				if (filename.contains("script.csv") || filename.contains("list.txt")) {
//					SKIP
				} else if (filename.contains("PRT_")) {
					fw.write("copy;" + filename + ";" + pathMasks + filename);
					fw.write(System.lineSeparator());
				} else if (filename.contains(".jpg") || filename.contains(".png") || filename.contains(".wmv")) {
					if (filename.contains("TAKE_CARD.png") || filename.contains("TAKE_CASH.png")) {

						fw.write("copy;" + filename + ";" + pathPics + "New Flow\\" + filename);
						fw.write(System.lineSeparator());

					} else {

						fw.write("copy;" + filename + ";" + pathPics + filename);
						fw.write(System.lineSeparator());

					}

				} else if (filename.contains("atm.exe")) {
					fw.write("copy;" + filename + ";" + path + filename);
					fw.write(System.lineSeparator());
					isIncludeAtmExe = true;
				} else {
					fw.write("copy;" + filename + ";" + path + filename);
					fw.write(System.lineSeparator());
				}
			}

			if (isIncludeAtmExe) {
				fw.write("reboot");
			} else {
				fw.write("reinit");
			}

			/* ALWAYS REBOOT */
//			fw.write("reboot");

			fw.close();
		} catch (Exception e) {
			e.getStackTrace();
			throw e;
		}
	}

	public static ArrayList<String> getListOfFilesFromFolder(String sourceDirPath) throws Exception {
		ArrayList<String> listData = new ArrayList<String>();

		Path pp = Paths.get(sourceDirPath);
		Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {
			String filename = path.getFileName().toString();
			listData.add(filename);
		});

		return listData;
	}

	public static ArrayList<String> getListOfFilesFromAtmFolderBlacklist(List<String> listOfBlacklist,
			String environment, Map<String, String> mapOfConfig) throws Exception {
		ArrayList<String> listData = new ArrayList<String>();

		try {

			String sourceDirPath = "";

			if (environment.equalsIgnoreCase("trunk")) {
				sourceDirPath = mapOfConfig.get("pathSvnTrunk");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (!listOfBlacklist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("sit")) {

				sourceDirPath = mapOfConfig.get("pathSvnSit");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (!listOfBlacklist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("uat")) {

				sourceDirPath = mapOfConfig.get("pathSvnUat");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (!listOfBlacklist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("production-staging1")) {

				sourceDirPath = mapOfConfig.get("pathSvnProdStaging");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (!listOfBlacklist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("production")) {

				sourceDirPath = mapOfConfig.get("pathSvnProd");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (!listOfBlacklist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return listData;
	}

	public static ArrayList<String> getListOfFilesFromAtmFolderWhitelist(List<String> listOfWhitelist,
			String environment, Map<String, String> mapOfConfig) throws Exception {
		ArrayList<String> listData = new ArrayList<String>();

		try {

			String sourceDirPath = "";

			if (environment.equalsIgnoreCase("trunk")) {
				sourceDirPath = mapOfConfig.get("pathSvnTrunk");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (listOfWhitelist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("sit")) {

				sourceDirPath = mapOfConfig.get("pathSvnSit");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (listOfWhitelist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("uat")) {

				sourceDirPath = mapOfConfig.get("pathSvnUat");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (listOfWhitelist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("production-staging1")) {

				sourceDirPath = mapOfConfig.get("pathSvnProdStaging");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (listOfWhitelist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			} else if (environment.equalsIgnoreCase("production")) {

				sourceDirPath = mapOfConfig.get("pathSvnProd");

				String[] arrayOfSubFolders = sourceDirPath.split(Pattern.quote(File.separator));

				int indexTargetSubFolder = arrayOfSubFolders.length;
				int indexTargetSubOfSubFolder = indexTargetSubFolder + 1;

				Path pp = Paths.get(sourceDirPath);
				Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {

					String url = path.toString();
					url = url.replace("\\", "/");
					String[] folderName = url.split("/");
					if (folderName.length == indexTargetSubOfSubFolder
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("masks")
							|| folderName[indexTargetSubFolder].equalsIgnoreCase("pics")) {
						String filename = path.getFileName().toString();
						if (listOfWhitelist.contains(filename)) {
							listData.add(filename);
							File destFile = new File("C:\\tms_patch_packager\\tmp_packer_files\\");
							Path destPath = destFile.toPath();
							try {
								Files.copy(path, destPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

				});

			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return listData;
	}

	public static void packToZip(String sourceDirPath, String zipFilePath) throws IOException {

		File file = new File(zipFilePath);

		try {
			Files.deleteIfExists(file.toPath());
			System.out.println("DELETE TARGET IF EXIST DONE!");
		} catch (IOException e) {
			System.out.println("DELETE TARGET IF EXIST FAILED!");
			e.printStackTrace();
			throw e;
		}

		Path p = Files.createFile(Paths.get(zipFilePath));
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
			Path pp = Paths.get(sourceDirPath);
			Files.walk(pp).filter(path -> !Files.isDirectory(path)).forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
				try {
					if (!zipEntry.getName().contains("Thumbs.db")) {
						zs.putNextEntry(zipEntry);
						Files.copy(path, zs);
						zs.closeEntry();
					} else {
						System.out.println("Thumbs.db detected");
					}
				} catch (IOException e) {
					e.getStackTrace();
				}
			});
		} catch (IOException e) {
			e.getStackTrace();
			throw e;
		}
	}

}
