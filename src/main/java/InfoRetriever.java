/**
 * Created by deado on 2016/12/6.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

//example:
//Movie Url:https://www.amazon.com/dp/B00006HAXW/
//User Url: https://www.amazon.com/gp/cdp/member-reviews/A1RSDE90N6RSZF
public class InfoRetriever {

    static String UserOutputFile = "";
    static String MovieOutPutFile = "";

    public class Movie{
        public String         movieASIN;
        public String         movieName;
        public Double         price;
        public Float          averageComment;
        public List<String>   directors;
        public List<String>   actors;
        public String         publishTime;
        public String         edition;
        public String         format;
        public String         rate;
        public List<String>   category;
        public List<Comment>  comments;
        public Movie(){
            this.directors = new ArrayList<String>();
            this.actors    = new ArrayList<String>();
            this.comments  = new ArrayList<Comment>();
            this.category  = new ArrayList<String>();
        }
    }
    public class Comment{
        public String  userASIN;
        public String  userName;
        public Float   commentScore;
        public String  commentSummery;
        public String  commentText;
        public String  commentTime;
        public Integer helpfulness;
    }


    //unit method to get information of one movie (Page type normal)
    //Example:https://www.amazon.com/dp/B002MVPPMI/
    private Movie retrieveMovieInfoTypeNormal(Document doc, Elements ProDetails, String url){

        try{
            //return value
            Movie ret = new Movie();

            //ASIN:
            String asin = url.substring(url.indexOf("dp/")+3);
            if(asin.charAt(asin.length()-1)=='/'){
                asin = asin.substring(0,asin.length()-1);
            }
            ret.movieASIN = asin;




            /**********************************Part one*****************************/
            //name and edition
            String proTitle = doc.getElementById("productTitle").text();
            Integer pareBeg = proTitle.indexOf("(");
            Integer mPareBeg = proTitle.indexOf("[");
            if(pareBeg > 0){
                ret.movieName = proTitle.substring(0,pareBeg);
                ret.edition = proTitle.substring(pareBeg+1, proTitle.indexOf(")"));
            }
            else{
                if(mPareBeg > 0){// without [ and ]
                    ret.movieName = proTitle.substring(0,mPareBeg);
                }else{
                    ret.movieName = proTitle.substring(0,proTitle.length());
                }
                ret.edition = null;
            }

            //byline Element to get format
            Element byline = doc.getElementById("byline");
            //format
            String  formatHtml = byline.html();
            String  formatKey = "<span class=\"a-color-secondary\">Format: </span>";
            Integer formatStartPt = formatHtml.indexOf(formatKey);
            String  formatContentWithTag = formatHtml.substring(formatStartPt+formatKey.length(), formatHtml.length());
            ret.format = formatContentWithTag.substring(formatContentWithTag.indexOf("<span>")+6, formatContentWithTag.indexOf("</span>"));

            //tmmSwatches to get price
            Element tmm = doc.getElementById("tmmSwatches");
            //price
            String Price = tmm.getElementsByTag("ul").first().getElementsByClass("selected").first().
                    getElementsByClass("a-color-price").first().text();
            ret.price = Double.valueOf(Price.substring(1));


            //get category
            Element categoryDiv = doc.getElementsByClass("content").last();
            Elements categoryLi = categoryDiv.getElementsByTag("li");
            for(Element e :categoryLi){
                String cateText = e.getElementsByTag("a").last().text();
                Integer middlePot = cateText.indexOf("&");
                if(0 > middlePot){
                    ret.category.add(cateText);
                } else{
                    ret.category.add(cateText.substring(0,middlePot-1));
                    ret.category.add(cateText.substring(middlePot+2));
                }
            }




            /***********************************************************************/

            Pattern release = Pattern.compile("<b>DVD Release Date:</b>");
            Pattern review = Pattern.compile("<b>Average Customer Review:</b>");
            Pattern actors = Pattern.compile("<b>Actors:</b>");
            Pattern director = Pattern.compile("<b>Directors:</b>");
            Pattern rated = Pattern.compile("<b>Rated:");
            //from product details
            Matcher matcher = null;
            for(Element e: ProDetails){
                String html = e.html();

                //release time
                matcher = release.matcher(html);
                if(matcher.find()){
                    Integer startPt = html.indexOf("</b>");
                    ret.publishTime = html.substring(startPt+4, html.length());
                }

                //review point
                //target string  title="4.4 out of 5 stars
                matcher = review.matcher(html);
                if(matcher.find()){
                    //get title text
                    Pattern reviewPattern = Pattern.compile("(\\d.\\d) out of 5 stars");
                    Matcher reviewMatcher = reviewPattern.matcher(html);
                    //get float point
                    if(reviewMatcher.find()){
                        String strAverageComment= reviewMatcher.group().substring(0,3);
                        ret.averageComment = Float.valueOf(strAverageComment);
                    }
                }

                //actors
                matcher = actors.matcher(html);
                if(matcher.find()){
                    Elements actorsEles = e.getElementsByTag("a");
                    for(Element ae: actorsEles){
                        ret.actors.add(ae.text());
                    }
                }

                //directors
                matcher = director.matcher(html);
                if(matcher.find()){
                   Elements directorsEles = e.getElementsByTag("a");
                   for(Element ae: directorsEles){
                       ret.directors.add(ae.text());
                   }

                }

                //rated
                matcher = rated.matcher(html);
                if(matcher.find()){
                    ret.rate = e.getElementsByClass("a-size-small").first().text();
                }

            }
            return ret;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //unit method to get information of one movie (Page type Amazon Video)
    //Examples:https://www.amazon.com/dp/B01KH23NVS
    private Movie retrieveMovieInfoAmazonVideo(Document doc, Elements ProDetails, String url){
        try{
            Movie ret = new Movie();
            //ASIN:
            String asin = url.substring(url.indexOf("dp/")+3);
            if(asin.charAt(asin.length()-1)=='/'){
                asin = asin.substring(0,asin.length()-1);
            }
            ret.movieASIN = asin;

            //movie name
            Element titleDiv = doc.getElementById("dv-dp-title-content");
            ret.movieName = titleDiv.getElementsByTag("h1").first().text();
            //publish time
            ret.publishTime = titleDiv.getElementsByTag("h2").first().text();

            //averageComment
            Element reviewStarI = doc.getElementById("reviewStars");
            Pattern reviewValuePattern = Pattern.compile("(\\d.\\d) out of 5 stars");
            Matcher reviewMatcher = reviewValuePattern.matcher(reviewStarI.text());
            if(reviewMatcher.find()){
                ret.averageComment = Float.valueOf(reviewMatcher.group().substring(0,3));
            }

            //price
            Element priceDiv = doc.getElementById("dv-action-box").getElementsByClass("dv-purchase-options").first();
            Elements allFormInPriceDiv = priceDiv.getElementsByTag("form");
            for(Element e: allFormInPriceDiv){
                Element buyEle = e.getElementsByClass("dv-button-text").first();

                if(-1 != buyEle.text().indexOf("Buy")){
                    String buyContent = buyEle.text();
                    String priceStr = buyContent.substring(buyContent.indexOf("$")+1);
                    ret.price = Double.valueOf(priceStr);
                    break;
                }
            }

            /******************************************************************************************/

            Matcher matcher = null;
            //get info from Product details
            for(Element e: ProDetails){
                Element tr = e.getElementsByTag("tr").first();
                Element th = tr.getElementsByTag("th").first();
                Element td = tr.getElementsByTag("td").first();

                String detailStr = th.text();

                //category
                if(0 == detailStr.compareTo("Genres")){
                    Elements aEles = td.getElementsByTag("a");
                    for(Element ae:aEles){
                        ret.category.add(ae.text());
                    }
                }

                //director
                if(0 == detailStr.compareTo("Director")){
                    Elements aEles = td.getElementsByTag("a");
                    for(Element ae:aEles){
                        ret.directors.add(ae.text());
                    }
                }

                //actors
                if(0 == detailStr.compareTo("Starring")){
                    Elements aEles = td.getElementsByTag("a");
                    for(Element ae:aEles){
                        ret.actors.add(ae.text());
                    }
                }

                //rate
                if(0 == detailStr.compareTo("MPAA rating")){
                    String rateTemp = td.text();
                    Integer pareIndex = rateTemp.indexOf("(");
                    if(0 < pareIndex){
                        ret.rate = rateTemp.substring(0,pareIndex-1);
                    }
                    else{
                        ret.rate = rateTemp;
                    }
                }

                //format
                if(0 == detailStr.compareTo("Format")){
                    String formatTemp = td.text();
                    Integer pareIndex = formatTemp.indexOf("(");
                    if(0 < pareIndex){
                        ret.format = formatTemp.substring(0,pareIndex-1);
                    }
                    else{
                        ret.format = formatTemp;
                    }
                }


            }

            return ret;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //unit method to get information of one movie (Page type TV serials)
    //Examples:https://www.amazon.com/dp/B01MXDAMKE/
    private Movie retrieveMovieInfoTVSerials(Document doc, Elements ProDetails, String url){
        try{
            Movie ret = new Movie();
            //ASIN:
            String asin = url.substring(url.indexOf("dp/")+3);
            if(asin.charAt(asin.length()-1)=='/'){
                asin = asin.substring(0,asin.length()-1);
            }
            ret.movieASIN = asin;

            //serial name
            //id:dv-dp-title-content
            Element titleName = doc.getElementById("dv-dp-title-content");
            Element h = titleName.getElementsByTag("h1").first();
            ret.movieName = h.text();

            //price
            //dv-sub-box
            Element priceDiv = doc.getElementById("dv-sub-box");
            String  priceText = priceDiv.getElementsByTag("p").text();
            String  priceStr = priceText.substring(priceText.indexOf("$")+1, priceText.indexOf("/"));
            ret.price = Double.valueOf(priceStr);

            //averageComment
            //id:reviewStars
            Element starSpan = doc.getElementById("reviewStars").getElementsByTag("span").first();
            String  starText = starSpan.text();
            ret.averageComment = Float.valueOf(starText.substring( 0, starText.indexOf(" out") ));

            //directors--no such info on web page
            //TODO:null

            //actors & format
            //from the input para
            for(Element e: ProDetails){
                Element tr = e.getElementsByTag("tr").first();
                Element th = tr.getElementsByTag("th").first();
                Element td = tr.getElementsByTag("td").first();
                String detailStr = th.text();

                //actors
                if(0 == detailStr.compareTo("Starring")){
                    Elements aEles = td.getElementsByTag("a");
                    for(Element ae:aEles){
                        ret.actors.add(ae.text());
                    }
                }

                //format
                if(0 == detailStr.compareTo("Format")){
                    String formatText = td.text();
                    Integer pareIndex = formatText.indexOf("(");
                    if(0 < pareIndex){
                        ret.format = formatText.substring(0,pareIndex-1);
                    }
                    else{
                        ret.format = formatText;
                    }
                }
            }

            //publish time -- get from the first episode
            //rate  -- get from the first episode
            //id:dv-el-id-1
            Element timeDiv = doc.getElementById("dv-el-id-1").getElementsByClass("dv-el-synopsis-content").first();
            Element timeSpan = timeDiv.getElementsByClass("dv-el-attr-value").last();
            ret.publishTime = timeSpan.text();

            Element  rateSpan = timeDiv.getElementsByClass("dv-el-badge").first();
            ret.rate = rateSpan.text();

            //edition -- no such info on web page
            //TODO:null
            //category -- no such info on web page
            //TODO:null

            return ret;
        }catch(Exception e){
            return null;
        }
    }

    //to handle different pages
    //amazon suckers
    private Movie SpiderDispatcher(String url){
        try{
            System.out.println(url + "...");

            Movie ret = null;

            //get html
            HttpClient client = new DefaultHttpClient();
            HttpGet    getHttp = new HttpGet(url);

            String  htmlContext = EntityUtils.toString(client.execute(getHttp).getEntity());

            Document doc = Jsoup.parse(htmlContext);
            Element  ProDetails ;
            //case one
            ProDetails = doc.getElementById("detail-bullets");
            if(null != ProDetails){
                ret = this.retrieveMovieInfoTypeNormal(doc,ProDetails.getElementsByTag("li"), url);
                System.out.println("a");
            }
            //case two
            ProDetails = doc.getElementById("dv-center-features");
            if(null != ProDetails){
                Element typeCheck = doc.getElementById("dv-sub-box");
                ProDetails = ProDetails.getElementsByTag("tbody").first();
                if(null == typeCheck){
                    ret = this.retrieveMovieInfoAmazonVideo(doc,ProDetails.getElementsByTag("tr"), url);
                }else{
                    ret = this.retrieveMovieInfoTVSerials(doc,ProDetails.getElementsByTag("tr"), url);
                }
            }

            return ret;

        }catch(Exception e){
            System.out.println(url + ": error!");
            return null;
        }
    }


    //main controller method
    //attentions:
    //  1. change browser info
    //  2. change frequency
    //  3. change ip (two serves or restart router)
    public void mainController(){

    }



    //attribute pattern



    //multiple client info




    public static void main(String[] argv){
        InfoRetriever ir = new InfoRetriever();
        ir.SpiderDispatcher("https://www.amazon.com/dp/B01MXDAMKE/");
    }

}
