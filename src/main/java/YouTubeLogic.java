import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.youtube.model.*;
import com.google.api.services.youtube.YouTube;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


class YouTubeLogic {

    private static final String APPLICATION_NAME = "API Sample";

    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/java-youtube-api-tests");

    private static FileDataStoreFactory DATA_STORE_FACTORY;

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static HttpTransport HTTP_TRANSPORT;

    private static final Collection<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/youtube.force-ssl https://www.googleapis.com/auth/youtubepartner");

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static Credential authorize() throws Exception {

        InputStream in = YouTubeLogic.class.getResourceAsStream("client_id.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader( in ));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    private static YouTube getYouTubeService() throws Exception {
        Credential credential = authorize();
        return new YouTube.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    private static boolean intersect(List<String> arr1, String[] arr2){
        if (arr1==null){
            arr1=new ArrayList<String>();
        }
        Set<String> strSet1 = new HashSet<>(arr1);
        Set<String> strSet2= Arrays.stream(arr2).collect(Collectors.toSet());
        strSet1.retainAll(strSet2);
        //Еще один способ для пересечения
        //Set<String> intersect = strSet1.stream().filter(strSet2::contains).collect(Collectors.toSet());
        return strSet1.size() > 0;
    }
    private static Object[][] viewSort(VideoListResponse response, int len){

        int counter=0;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        Object[][] arr=new Object[10][3];
        for (int i=0;i<len;i++){
            long difference = ChronoUnit.HOURS.between(ZonedDateTime.parse(String.valueOf(response.getItems().get(i).getSnippet().getPublishedAt()), formatter.withZone(ZoneId.of("UTC"))), ZonedDateTime.now());
            if (difference<=24
                    && counter<10
                    && !Arrays.asList(BotLogic.TitleExceptions).contains(response.getItems().get(i).getSnippet().getChannelTitle())
                    && !intersect(response.getItems().get(i).getSnippet().getTags(), BotLogic.TagExceptions)) {

                arr[counter][0] = response.getItems().get(i).getSnippet().getTitle();
                arr[counter][1] = response.getItems().get(i).getStatistics().getViewCount();
                arr[counter][2] = "https://www.youtube.com/watch?v="+response.getItems().get(i).getId();
                counter++;
            }
            else if (counter==10){break;}
        }

        Arrays.sort(arr, Comparator.comparing((Object[] a) -> (BigInteger)a[1]).reversed());

        return arr;
    }

    static Object[][]  getVideoList() throws Exception {

        YouTube youtube = getYouTubeService();

        try {
            HashMap<String, String> parameters = new HashMap<String, String>();
            parameters.put("part", "snippet,contentDetails,statistics");
            parameters.put("maxResults", "25");
            parameters.put("chart", "mostPopular");
            parameters.put("regionCode", "RU");
            parameters.put("videoCategoryId", "");

            YouTube.Videos.List videosListMostPopularRequest = youtube.videos().list(parameters.get("part").toString());
            if (parameters.containsKey("chart") && parameters.get("chart") != "") {
                videosListMostPopularRequest.setChart(parameters.get("chart").toString());
            }

            if (parameters.containsKey("maxResults")) {
                videosListMostPopularRequest.setMaxResults(Long.parseLong(parameters.get("maxResults").toString()));
            }

            if (parameters.containsKey("regionCode") && parameters.get("regionCode") != "") {
                videosListMostPopularRequest.setRegionCode(parameters.get("regionCode").toString());
            }

            if (parameters.containsKey("videoCategoryId") && parameters.get("videoCategoryId") != "") {
                videosListMostPopularRequest.setVideoCategoryId(parameters.get("videoCategoryId").toString());
            }

            VideoListResponse response = videosListMostPopularRequest.execute();

            int len= Integer.parseInt(parameters.get("maxResults"))-1; //КАКОГО ХУЯ ТАК ПРОИЗОШЛО

            return viewSort(response, len);





        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return new Object[0][];
    }
}
