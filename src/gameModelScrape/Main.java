package gameModelScrape;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Main {

	public static void main(String[] args) throws InterruptedException, IOException  {
		
		// Opens chromedriver.exe as a web driver

		System.setProperty("webdriver.chrome.driver", args[2]);
		
		try  {
			if(args[0]==null) { System.out.println("args[0] = " + args[0]); }
		}
		catch(Exception e) {
			System.out.println("Use the format:\n\t$ java -jar ModelScrapeNBA.jar <E-mail> <Password> <Path to WebDriver>\n\t(Cleaning the Glass [Memberful] Log-in Info)\n");
		}
		
		// Runs method for scraping model data
		scrapeModelData(args[0], args[1]);

	}

	public static void scrapeModelData (String email, String password) throws InterruptedException, IOException {
		
	    Calendar calendar = Calendar.getInstance();
	    double time = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE)/60.0;

	    String injRepTime;
	    if (time < 13.5) {
	    	System.out.println("No injury report available.\n");
	    	return;
	    }
	    else if (time < 17.5) {	injRepTime = "1"; }
	    else if (time < 20.5) { injRepTime = "5"; }
	    else 				  { injRepTime = "8"; }
	    
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		LocalDate yesterdayCTG = today.minusDays(2);
		LocalDate twoWksAgo = today.minusDays(15);
		
		// Download PDF on open
		HashMap<String,Object> chromePrefs = new HashMap<String, Object>();
		chromePrefs.put("plugins.always_open_pdf_externally", true);

		ChromeOptions options = new ChromeOptions();
		options.setExperimentalOption("prefs", chromePrefs);

		WebDriver driver = new ChromeDriver(options);
						
		// Sets implicit wait times for 10 seconds
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		
		// Maximizes driver window
		driver.manage().window().maximize();
		
		// Downloads Official NBA Injury Report PDF
		driver.get("https://ak-static.cms.nba.com/referee/injury/Injury-Report_" + today.getYear() + "-" + String.format("%02d", today.getMonthValue()) + "-" + String.format("%02d", today.getDayOfMonth()) + "_0" + injRepTime + "PM.pdf");
		Thread.sleep(2000);
		
		File file = new File("C:\\Users\\fnoon\\Downloads\\Injury-Report_" + today.getYear() + "-" + String.format("%02d", today.getMonthValue()) + "-" + String.format("%02d", today.getDayOfMonth()) + "_0" + injRepTime + "PM.pdf");
		
		@SuppressWarnings("resource")
		PDDocument pd = new PDDocument();
		pd = PDDocument.load(file);

		PDFTextStripper pdf = new PDFTextStripper();
		String pdfText = pdf.getText(pd);
		
		pd.close();
		
		// Gets the login page
		driver.get("https://cleaningtheglass.com?memberful_endpoint=auth");
		System.out.print("Logging in...\t\t\t\t\t");
		
		// Fills login/password fields with Email/Password & clicks 'Sign in' button
		driver.findElement(By.id("login")).sendKeys(email);
		driver.findElement(By.id("password")).sendKeys(password);
		driver.findElement(By.xpath("/html/body/div[2]/div[2]/div/form/button")).click();
		
		// Waits for log-in to be processed, jumps to CTG's Four Factors page for 'Home' performance, then waits for page load
		Thread.sleep(1000);
		System.out.println("Done!");
		driver.get("https://www.cleaningtheglass.com/stats/league/fourfactors?season=2019&seasontype=regseason&start=10/15/2019&end=07/1/2020&venue=home");
		System.out.print("Saving Home data...\t\t\t\t");
		Thread.sleep(1000);
		
		// Gets page's source code and saves all table data ("td") elements
		Document doc = Jsoup.parse(driver.getPageSource());
		Elements data = doc.getElementsByTag("td");
		
		// Saves td element's texts as strings in a 2D ArrayList of Strings
		ArrayList<ArrayList<String>> homeSeasonDataTableText = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < 30; i++) {
			ArrayList<String> temp = new ArrayList<String>();
			for(int j = 0; j < data.size()/30; j++) {
				temp.add(data.get((i*(data.size()/30))+j).text());
			}
			temp.remove(0);
			temp.remove(3);
			temp.remove(5);
			temp.remove(15);
			homeSeasonDataTableText.add(temp);
		}
		
		// Sorts table data by team name alphabetically
		Collections.sort(homeSeasonDataTableText, teamNameComparator);
		
		// Removes team names so the table can be processed as all Double values
		ArrayList<String> homeTeamNames = new ArrayList<String>();
		for(int i = 0; i < homeSeasonDataTableText.size(); i++) {
			homeTeamNames.add(homeSeasonDataTableText.get(i).get(0));
			homeSeasonDataTableText.get(i).remove(0);
		}
		
		// Saves table data as Double values in a 2D ArrayList of Doubles
		ArrayList<ArrayList<Double>> homeSeasonDataTable = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < homeSeasonDataTableText.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for(int j = 0; j < homeSeasonDataTableText.get(0).size(); j++) {
				if(j == 3) {
					if(homeSeasonDataTableText.get(i).get(j).substring(0,1).equals("+")) {
						temp.add(Double.parseDouble(homeSeasonDataTableText.get(i).get(j).substring(1)));
					}
					else {
						temp.add(Double.parseDouble(homeSeasonDataTableText.get(i).get(j)));
					}
				}
				else if(j == 7 || j == 9 || j == 11 || j == 17 || j == 19 || j == 21) {
					temp.add(Double.parseDouble(homeSeasonDataTableText.get(i).get(j).substring(0,homeSeasonDataTableText.get(i).get(j).length()-1)));
				}
				else {
					temp.add(Double.parseDouble(homeSeasonDataTableText.get(i).get(j)));
				}
			}
			homeSeasonDataTable.add(temp);
		}
		System.out.println("Done!");		
		
		// Jumps to CTG's Four Factors page for 'Home' performance AS OF YESTERDAY, then waits for page load
		driver.get("https://www.cleaningtheglass.com/stats/league/fourfactors?season=2019&seasontype=regseason&start=10/15/2019&end=" + yesterdayCTG.getMonthValue() + "/" + yesterdayCTG.getDayOfMonth() + "/" + yesterdayCTG.getYear() + "&venue=home");
		System.out.print("Saving Yesterday's Home data...\t\t\t");
		Thread.sleep(500);
		
		// Gets page's source code and saves all table data ("td") elements
		doc = Jsoup.parse(driver.getPageSource());
		data = doc.getElementsByTag("td");
		
		ArrayList<ArrayList<String>> yesHomeTableText = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < 30; i++) {
			ArrayList<String> temp = new ArrayList<String>();
			for(int j = 0; j < data.size()/30; j++) {
				temp.add(data.get((i*(data.size()/30))+j).text());
			}
			temp.remove(0);
			temp.remove(3);
			temp.remove(5);
			temp.remove(15);
			yesHomeTableText.add(temp);
		}
		
		// Sorts table data by team name alphabetically
		Collections.sort(yesHomeTableText, teamNameComparator);
		
		// Removes team names so the table can be processed as all Double values
		ArrayList<String> yesHomeTeamNames = new ArrayList<String>();
		for(int i = 0; i < yesHomeTableText.size(); i++) {
			yesHomeTeamNames.add(yesHomeTableText.get(i).get(0));
			yesHomeTableText.get(i).remove(0);
		}
		
		// Saves table data as Double values in a 2D ArrayList of Doubles
		ArrayList<ArrayList<Double>> yesHomeTable = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < yesHomeTableText.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for(int j = 0; j < yesHomeTableText.get(0).size(); j++) {
				if(j == 3) {
					if(yesHomeTableText.get(i).get(j).substring(0,1).equals("+")) {
						temp.add(Double.parseDouble(yesHomeTableText.get(i).get(j).substring(1)));
					}
					else {
						temp.add(Double.parseDouble(yesHomeTableText.get(i).get(j)));
					}
				}
				else if(j == 7 || j == 9 || j == 11 || j == 17 || j == 19 || j == 21) {
					temp.add(Double.parseDouble(yesHomeTableText.get(i).get(j).substring(0,yesHomeTableText.get(i).get(j).length()-1)));
				}
				else {
					temp.add(Double.parseDouble(yesHomeTableText.get(i).get(j)));
				}
			}
			yesHomeTable.add(temp);
		}
		System.out.println("Done!");
		
		
		// Jumps to CTG's Four Factors page for 'Away' performance, then waits for page load
		driver.get("https://www.cleaningtheglass.com/stats/league/fourfactors?season=2019&seasontype=regseason&start=10/15/2019&end=07/1/2020&venue=away");
		System.out.print("Saving Away data...\t\t\t\t");
		Thread.sleep(500);
		
		// Gets page's source code and saves all table data ("td") elements
		doc = Jsoup.parse(driver.getPageSource());
		data = doc.getElementsByTag("td");
		
		ArrayList<ArrayList<String>> awaySeasonDataTableText = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < 30; i++) {
			ArrayList<String> temp = new ArrayList<String>();
			for(int j = 0; j < data.size()/30; j++) {
				temp.add(data.get((i*(data.size()/30))+j).text());
			}
			temp.remove(0);
			temp.remove(3);
			temp.remove(5);
			temp.remove(15);
			awaySeasonDataTableText.add(temp);
		}
		
		// Sorts table data by team name alphabetically
		Collections.sort(awaySeasonDataTableText, teamNameComparator);
		
		// Removes team names so the table can be processed as all Double values
		ArrayList<String> awayTeamNames = new ArrayList<String>();
		for(int i = 0; i < awaySeasonDataTableText.size(); i++) {
			awayTeamNames.add(awaySeasonDataTableText.get(i).get(0));
			awaySeasonDataTableText.get(i).remove(0);
		}
		
		// Saves table data as Double values in a 2D ArrayList of Doubles
		ArrayList<ArrayList<Double>> awaySeasonDataTable = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < awaySeasonDataTableText.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for(int j = 0; j < awaySeasonDataTableText.get(0).size(); j++) {
				if(j == 3) {
					if(awaySeasonDataTableText.get(i).get(j).substring(0,1).equals("+")) {
						temp.add(Double.parseDouble(awaySeasonDataTableText.get(i).get(j).substring(1)));
					}
					else {
						temp.add(Double.parseDouble(awaySeasonDataTableText.get(i).get(j)));
					}
				}
				else if(j == 7 || j == 9 || j == 11 || j == 17 || j == 19 || j == 21) {
					temp.add(Double.parseDouble(awaySeasonDataTableText.get(i).get(j).substring(0,awaySeasonDataTableText.get(i).get(j).length()-1)));
				}
				else {
					temp.add(Double.parseDouble(awaySeasonDataTableText.get(i).get(j)));
				}
			}
			awaySeasonDataTable.add(temp);
		}
		System.out.println("Done!");
		
		
		// Jumps to CTG's Four Factors page for 'Away' performance AS OF YESTERDAY, then waits for page load
		driver.get("https://www.cleaningtheglass.com/stats/league/fourfactors?season=2019&seasontype=regseason&start=10/15/2019&end=" + yesterday.getMonthValue() + "/" + yesterday.getDayOfMonth() + "/" + yesterday.getYear() + "&venue=away");
		System.out.print("Saving Yesterday's Home data...\t\t\t");
		Thread.sleep(500);
		
		// Gets page's source code and saves all table data ("td") elements
		doc = Jsoup.parse(driver.getPageSource());
		data = doc.getElementsByTag("td");
		
		ArrayList<ArrayList<String>> yesAwayTableText = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < 30; i++) {
			ArrayList<String> temp = new ArrayList<String>();
			for(int j = 0; j < data.size()/30; j++) {
				temp.add(data.get((i*(data.size()/30))+j).text());
			}
			temp.remove(0);
			temp.remove(3);
			temp.remove(5);
			temp.remove(15);
			yesAwayTableText.add(temp);
		}
		
		// Sorts table data by team name alphabetically
		Collections.sort(yesAwayTableText, teamNameComparator);
		
		// Removes team names so the table can be processed as all Double values
		ArrayList<String> yesAwayTeamNames = new ArrayList<String>();
		for(int i = 0; i < yesAwayTableText.size(); i++) {
			yesAwayTeamNames.add(yesAwayTableText.get(i).get(0));
			yesAwayTableText.get(i).remove(0);
		}
		
		// Saves table data as Double values in a 2D ArrayList of Doubles
		ArrayList<ArrayList<Double>> yesAwayTable = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < yesAwayTableText.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for(int j = 0; j < yesAwayTableText.get(0).size(); j++) {
				if(j == 3) {
					if(yesAwayTableText.get(i).get(j).substring(0,1).equals("+")) {
						temp.add(Double.parseDouble(yesAwayTableText.get(i).get(j).substring(1)));
					}
					else {
						temp.add(Double.parseDouble(yesAwayTableText.get(i).get(j)));
					}
				}
				else if(j == 7 || j == 9 || j == 11 || j == 17 || j == 19 || j == 21) {
					temp.add(Double.parseDouble(yesAwayTableText.get(i).get(j).substring(0,yesAwayTableText.get(i).get(j).length()-1)));
				}
				else {
					temp.add(Double.parseDouble(yesAwayTableText.get(i).get(j)));
				}
			}
			yesAwayTable.add(temp);
		}
		System.out.println("Done!");
		
		// Jumps to CTG's Four Factors page for 'Last 2 Weeks' performance, then waits for page load
		driver.get("https://www.cleaningtheglass.com/stats/league/fourfactors?season=2019&seasontype=regseason&start=" + twoWksAgo.getMonthValue() + "/" + twoWksAgo.getDayOfMonth() + "/" + twoWksAgo.getYear() + "&end=07/1/" + (twoWksAgo.getYear()+1));
		System.out.print("Saving Recent (Last 2 Weeks) data...\t\t");
		Thread.sleep(500);
		
		// Gets page's source code and saves all table data ("td") elements
		doc = Jsoup.parse(driver.getPageSource());
		data = doc.getElementsByTag("td");
		
		ArrayList<ArrayList<String>> recentDataTableText = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < 30; i++) {
			ArrayList<String> temp = new ArrayList<String>();
			for(int j = 0; j < data.size()/30; j++) {
				temp.add(data.get((i*(data.size()/30))+j).text());
			}
			temp.remove(0);
			temp.remove(3);
			temp.remove(5);
			temp.remove(15);
			recentDataTableText.add(temp);
		}
		
		// Sorts table data by team name alphabetically
		Collections.sort(recentDataTableText, teamNameComparator);
		
		// Removes team names so the table can be processed as all Double values
		ArrayList<String> recentTeamNames = new ArrayList<String>();
		for(int i = 0; i < recentDataTableText.size(); i++) {
			recentTeamNames.add(recentDataTableText.get(i).get(0));
			recentDataTableText.get(i).remove(0);
		}
		
		// Saves table data as Double values in a 2D ArrayList of Doubles
		ArrayList<ArrayList<Double>> recentDataTable = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < recentDataTableText.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for(int j = 0; j < recentDataTableText.get(0).size(); j++) {
				if(j == 3) {
					if(recentDataTableText.get(i).get(j).substring(0,1).equals("+")) {
						temp.add(Double.parseDouble(recentDataTableText.get(i).get(j).substring(1)));
					}
					else {
						temp.add(Double.parseDouble(recentDataTableText.get(i).get(j)));
					}
				}
				else if(j == 7 || j == 9 || j == 11 || j == 17 || j == 19 || j == 21) {
					temp.add(Double.parseDouble(recentDataTableText.get(i).get(j).substring(0,recentDataTableText.get(i).get(j).length()-1)));
				}
				else {
					temp.add(Double.parseDouble(recentDataTableText.get(i).get(j)));
				}
			}
			recentDataTable.add(temp);
		}
		System.out.println("Done!");
		
		// Jumps to CTG's Four Factors page for 'Last 2 Weeks' performance, then waits for page load
		driver.get("https://www.cleaningtheglass.com/stats/league/fourfactors?season=2019&seasontype=regseason&start=" + twoWksAgo.getMonthValue() + "/" + twoWksAgo.getDayOfMonth() + "/" + twoWksAgo.getYear() + "&end=" + yesterdayCTG.getMonthValue() + "/" + yesterdayCTG.getDayOfMonth() + "/" + yesterdayCTG.getYear()); // Fix next season
		System.out.print("Saving Yesterday's Recent data...\t\t");
		Thread.sleep(500);
		
		// Gets page's source code and saves all table data ("td") elements
		doc = Jsoup.parse(driver.getPageSource());
		data = doc.getElementsByTag("td");
		
		ArrayList<ArrayList<String>> yesRecentDataTableText = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < 30; i++) {
			ArrayList<String> temp = new ArrayList<String>();
			for(int j = 0; j < data.size()/30; j++) {
				temp.add(data.get((i*(data.size()/30))+j).text());
			}
			temp.remove(0);
			temp.remove(3);
			temp.remove(5);
			temp.remove(15);
			yesRecentDataTableText.add(temp);
		}
		
		// Sorts table data by team name alphabetically
		Collections.sort(yesRecentDataTableText, teamNameComparator);
		
		// Removes team names so the table can be processed as all Double values
		ArrayList<String> yesRecentTeamNames = new ArrayList<String>();
		for(int i = 0; i < yesRecentDataTableText.size(); i++) {
			yesRecentTeamNames.add(yesRecentDataTableText.get(i).get(0));
			yesRecentDataTableText.get(i).remove(0);
		}
		
		// Saves table data as Double values in a 2D ArrayList of Doubles
		ArrayList<ArrayList<Double>> yesRecentDataTable = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < yesRecentDataTableText.size(); i++) {
			ArrayList<Double> temp = new ArrayList<Double>();
			for(int j = 0; j < yesRecentDataTableText.get(0).size(); j++) {
				if(j == 3) {
					if(yesRecentDataTableText.get(i).get(j).substring(0,1).equals("+")) {
						temp.add(Double.parseDouble(yesRecentDataTableText.get(i).get(j).substring(1)));
					}
					else {
						temp.add(Double.parseDouble(yesRecentDataTableText.get(i).get(j)));
					}
				}
				else if(j == 7 || j == 9 || j == 11 || j == 17 || j == 19 || j == 21) {
					temp.add(Double.parseDouble(yesRecentDataTableText.get(i).get(j).substring(0,yesRecentDataTableText.get(i).get(j).length()-1)));
				}
				else {
					temp.add(Double.parseDouble(yesRecentDataTableText.get(i).get(j)));
				}
			}
			yesRecentDataTable.add(temp);
		}
		System.out.println("Done!");
		
		// Jumps to the night's game lines/spreads on Bovada, waits for page load, clicks element that filters for games being played within the next 24 hours, and waits for page load again
		driver.get("https://www.bovada.lv/sports/basketball/nba");
		System.out.print("Saving Bovada Lines data...\t\t\t");
		Thread.sleep(3000);
		driver.findElement(By.xpath("/html/body/bx-site/ng-component/div/sp-sports-ui/div/main/div/section/main/sp-path-event/div/header/sp-filter/section/div[1]/sp-tabbed-filter/div/ul/sp-tabbed-filter-element[1]/li")).click();
		Thread.sleep(3000);
		
		// Gets page's source code and saves all elements with the class "name" or "market-line bet-handicap"
		doc = Jsoup.parse(driver.getPageSource());
		Elements teamNames = doc.getElementsByClass("name");
		Elements spreadElements = doc.getElementsByClass("market-line bet-handicap");
	
		// (Change variable if the number of games on Bovada that occur the following day is greater than 0
		int numEarlyGames = 0;
		for(int i = 0; i < numEarlyGames; i++) { 
			teamNames.remove(teamNames.size()-1);
			teamNames.remove(teamNames.size()-1);
			spreadElements.remove(spreadElements.size()-1);
			spreadElements.remove(spreadElements.size()-1);
		}
		
		// Instantiates a HashMap that abbreviates each team name
		Map<String, String> teamNameCodes = new HashMap<String, String>();
		teamNameCodes.put("Atlanta Hawks", "ATL");
		teamNameCodes.put("Boston Celtics", "BOS");
		teamNameCodes.put("Brooklyn Nets", "BKN");
		teamNameCodes.put("Charlotte Hornets", "CHA");
		teamNameCodes.put("Chicago Bulls", "CHI");
		teamNameCodes.put("Cleveland Cavaliers", "CLE");
		teamNameCodes.put("Dallas Mavericks", "DAL");
		teamNameCodes.put("Denver Nuggets", "DEN");
		teamNameCodes.put("Detroit Pistons", "DET");
		teamNameCodes.put("Golden State Warriors", "GSW");
		teamNameCodes.put("Houston Rockets", "HOU");
		teamNameCodes.put("Indiana Pacers", "IND");
		teamNameCodes.put("Los Angeles Clippers", "LAC");
		teamNameCodes.put("Los Angeles Lakers", "LAL");
		teamNameCodes.put("Memphis Grizzlies", "MEM");
		teamNameCodes.put("Miami Heat", "MIA");
		teamNameCodes.put("Milwaukee Bucks", "MIL");
		teamNameCodes.put("Minnesota Timberwolves", "MIN");
		teamNameCodes.put("New Orleans Pelicans", "NOP");
		teamNameCodes.put("New York Knicks", "NYK");
		teamNameCodes.put("Oklahoma City Thunder", "OKC");
		teamNameCodes.put("Orlando Magic", "ORL");
		teamNameCodes.put("Philadelphia 76ers", "PHI");
		teamNameCodes.put("Phoenix Suns", "PHX");
		teamNameCodes.put("Portland Trail Blazers", "POR");
		teamNameCodes.put("Sacramento Kings", "SAC");
		teamNameCodes.put("San Antonio Spurs", "SAS");
		teamNameCodes.put("Toronto Raptors", "TOR");
		teamNameCodes.put("Utah Jazz", "UTA");
		teamNameCodes.put("Washington Wizards", "WAS");
		
		// Saves team names as their respective abbreviation
		ArrayList<String> teamAbvs = new ArrayList<String>();
		for(int i = 0; i < teamNames.size(); i++) {
			teamAbvs.add(teamNameCodes.get(teamNames.get(i).text()));
		}
		
		// Saves spreads into an ArrayList of Doubles
		ArrayList<Double> spreads = new ArrayList<Double>();
		for(int i = 0; i < spreadElements.size(); i+=2) {
			spreads.add(Double.parseDouble(spreadElements.get(i).text()));
			spreads.add(Double.parseDouble(spreadElements.get(i+1).text()));		
		}
		System.out.println("Done!");
		
		/*
		// Jumps to rotoworld.com's injury report, then waits for page load
		driver.get("https://www.rotoworld.com/basketball/nba/injury-report");
		System.out.print("Saving Injury Report data...\t\t\t");
		Thread.sleep(1000);
		
		// Gets page's source code and saves all elements with the class ""
		doc = Jsoup.parse(driver.getPageSource());
		Elements injRepElements = doc.getElementsByTag("td");

		ArrayList<ArrayList<String>> injRepTable = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < injRepElements.size(); i+=6) {
			ArrayList<String> temp = new ArrayList<String>();
			if(injRepElements.get(i).text().substring(injRepElements.get(i).text().length()-2).contains(" X")) {
				temp.add(injRepElements.get(i).text().substring(0, injRepElements.get(i).text().length()-2));
			}
			else {
				temp.add(injRepElements.get(i).text());
			}
			temp.add(injRepElements.get(i+1).text());
			temp.add(injRepElements.get(i+2).text());
			temp.add(injRepElements.get(i+3).text());
			temp.add(injRepElements.get(i+4).text());
			temp.add(injRepElements.get(i+5).text());
			injRepTable.add(temp);
		}
		System.out.println("Done!");
		*/
		
		
		
		
		
		// Closes WebDriver
		driver.close();
		
		// Output
		
		System.out.print("Converting data to CSV format...\t\t");
		String basePath = new File("").getAbsolutePath();
		File csvFile1 = new File(basePath + "\\output.csv");
		File csvFile2 = new File(basePath + "\\output-history.csv");
		File injRep   = new File(basePath + "\\injury-report.txt");
		//File logFile  = new File(basePath + "\\log.txt");
		FileWriter fileWriter1 = new FileWriter(csvFile1);
		FileWriter fileWriter2 = new FileWriter(csvFile2, true);
		//FileWriter fileWriter3 = new FileWriter(logFile, true);
		FileWriter fileWriter4 = new FileWriter(injRep);
		BufferedWriter buffWriter1 = new BufferedWriter(fileWriter2);
		//BufferedWriter buffWriter2 = new BufferedWriter(fileWriter3);
		
		fileWriter1.append("As of " + today.getMonthValue() + "-" + today.getDayOfMonth() + "-" + today.getYear() + "\n\n");
		
		fileWriter1.append("Home Data\n");
		for(int i = 0; i < homeSeasonDataTableText.size(); i++) {
			for(int j = 0; j < homeSeasonDataTableText.get(0).size(); j++) {
				fileWriter1.append(homeSeasonDataTableText.get(i).get(j) + ",");
			}
			fileWriter1.append("\n");
		}
		fileWriter1.append("\n");
		
		fileWriter1.append("Away Data\n");
		for(int i = 0; i < awaySeasonDataTableText.size(); i++) {
			for(int j = 0; j < awaySeasonDataTableText.get(0).size(); j++) {
				fileWriter1.append(awaySeasonDataTableText.get(i).get(j) + ",");
			}
			fileWriter1.append("\n");
		}
		fileWriter1.append("\n");
		
		fileWriter1.append("Last 2 Weeks Data\n");
		for(int i = 0; i < recentDataTableText.size(); i++) {
			for(int j = 0; j < recentDataTableText.get(0).size(); j++) {
				fileWriter1.append(recentDataTableText.get(i).get(j) + ",");
			}
			fileWriter1.append("\n");
		}
		fileWriter1.append("\n");
		/*
		fileWriter1.append("Injury Report\n" + injRepTable.size() + "\n");
		for(int i = 0; i < injRepTable.size(); i++) {
			for(int j = 0; j < injRepTable.get(0).size(); j++) {
				fileWriter1.append(injRepTable.get(i).get(j)+",");
			}
			fileWriter1.append("\n");
		}
		*/
		fileWriter1.append("\nAs of " + yesterday.getMonthValue() + "-" + yesterday.getDayOfMonth() + "-" + yesterday.getYear() + "\n\n");
		
		fileWriter1.append("Yesterday's Home Data\n");
		for(int i = 0; i < yesHomeTableText.size(); i++) {
			for(int j = 0; j < yesHomeTableText.get(0).size(); j++) {
				fileWriter1.append(yesHomeTableText.get(i).get(j) + ",");
			}
			fileWriter1.append("\n");
		}
		fileWriter1.append("\n");
		
		fileWriter1.append("Yesterday's Away Data\n");
		for(int i = 0; i < yesAwayTableText.size(); i++) {
			for(int j = 0; j < yesAwayTableText.get(0).size(); j++) {
				fileWriter1.append(yesAwayTableText.get(i).get(j) + ",");
			}
			fileWriter1.append("\n");
		}
		fileWriter1.append("\n");
		
		fileWriter1.append("Yesterday's Last 2 Weeks Data\n");
		for(int i = 0; i < yesRecentDataTableText.size(); i++) {
			for(int j = 0; j < yesRecentDataTableText.get(0).size(); j++) {
				fileWriter1.append(yesRecentDataTableText.get(i).get(j) + ",");
			}
			fileWriter1.append("\n");
		}
		fileWriter1.append("\n");
		/*
		fileWriter1.append("Yesterday's Injury Report\n" + injRepTable.size() + "\n");
		for(int i = 0; i < injRepTable.size(); i++) {
			for(int j = 0; j < injRepTable.get(0).size(); j++) {
				fileWriter1.append(injRepTable.get(i).get(j)+",");
			}
			fileWriter1.append("\n");
		}
		*/
		
		fileWriter1.flush();
		fileWriter1.close();
		
		// Output file 2
		
		buffWriter1.write(today.getMonthValue() + "-" + today.getDayOfMonth() + "-" + today.getYear() + "\n" + spreads.size()/2 + "\n");
		for(int i = 0; i < spreads.size()/2; i++) {
			buffWriter1.write("Away,");
			buffWriter1.write(teamAbvs.get(i*2)+",");
			buffWriter1.write(spreads.get(i*2)+"\n");
			buffWriter1.write("Home,");
			buffWriter1.write(teamAbvs.get(i*2+1)+",");
			buffWriter1.write(spreads.get(i*2+1)+"\n\n");
		}
		
		// Bovada Output
		buffWriter1.close();
		
		fileWriter4.append(pdfText);
		fileWriter4.flush();
		fileWriter4.close();
		
		System.out.println("Done!");
		
		// Notifies user to where their data has been output
		System.out.println("\nYour data can be found in 'output.csv', 'output-history.csv', & 'injury-report.txt.\n");
		
	}

	//Custom comparator that compares ArrayList<String>'s by the first element (in this case: team name)
	public static Comparator<ArrayList<String>> teamNameComparator = new Comparator<ArrayList<String>>() {
	
		public int compare(ArrayList<String> s1, ArrayList<String> s2) {
		   String teamName1 = s1.get(0).toUpperCase();
		   String teamName2 = s2.get(0).toUpperCase();
	
		   //ascending order
		   return teamName1.compareTo(teamName2);
	
	    }
		
	};
	
}