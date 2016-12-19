import java.io.*;
import java.sql.*;
import java.text.DateFormat;
import java.util.*;
import java.util.Date;
import com.mysql.cj.api.x.JsonValue;
import org.json.*;
import sun.util.calendar.BaseCalendar;

/**
 * Created by deado on 2016/12/15.
 */
public class DataWasher {


    public class Movie {
        public String movieASIN;
        public String movieName;
        public Double price;
        public Float averageComment;
        public String publishTime;
        public String edition;
        public String format;
        public String rate;
        public String actors;
        public String directors;
        public String category;
        public String timeId;
        public String nameId;
        public String categoryId;
        public String directId;
        public String actorsId;

        public Movie(){
            movieName = " ";
            movieName = " ";
            price = 0.0;
            averageComment = 0.0f;
            publishTime = " ";
            edition = " ";
            format = " ";
            rate = " ";
            actors = " ";
            directors = " ";
            category = "|";
            timeId = " ";
            nameId = " ";
            categoryId = "|";
            directId = "|";
            actorsId = "|";
        }
    }


    private Integer actorNextId = 0;
    class actorDimension {
        String id;
        String actorName = " ";
        Integer starringCount = 0;
        Integer supportCount = 0;
    }
    class actor_movie{
        String actorId;
        String productId;
    }

    private Integer categoryNextId=0;
    class categoryDimension{

        String  id;
        String  category;
        Integer count = 0;
    }

    class category_movie{
        String  categoryId;
        String  productId;
    }

    private Integer directorNextId=0;
    class directorDimension{

        String  id;
        String  name;
        Integer count = 0;
    }

    class director_movie{
        String  directorId;
        String  productId;
    }

    private Integer nameNextId=0;
    class nameDimension{

        String  id;
        String  name;
        Integer count = 0;
    }

    class name_movie{
        String  nameId;
        String  productId;
    }

    class timeDimension{
        String  time;
        Integer year;
        Integer month;
        Integer season;
        Integer weekday;
        Integer count;
    }

    class time_movie{
        String  time;
        String  productId;
    }

    private ArrayList<Movie>                movieTable = new ArrayList<Movie>();

    private Map<String,actorDimension>      actorDimensionTable = new HashMap<String, actorDimension>();
    private ArrayList<actor_movie>          star_moviesTable = new ArrayList<actor_movie>();
    private ArrayList<actor_movie>          support_moviesTable = new ArrayList<actor_movie>();

    private Map<String,categoryDimension>   categoryDimensionsTable = new HashMap<String, categoryDimension>();
    private ArrayList<category_movie>       category_moviesTable = new ArrayList<category_movie>();

    private Map<String,directorDimension>   directorDimensionsTable = new HashMap<String, directorDimension>();
    private ArrayList<director_movie>       director_moviesTable = new ArrayList<director_movie>();

    private Map<String,nameDimension>       nameDimensionsTable = new HashMap<String, nameDimension>();
    private ArrayList<name_movie>           name_moviesTable = new ArrayList<name_movie>();

    private Map<String,timeDimension>       timeDimensionsTable = new HashMap<String, timeDimension>();
    private ArrayList<time_movie>           time_moviesTable = new ArrayList<time_movie>();




    private void generateTable(){
        try{
            this.readDataFromDatabase();
            Iterator<Map.Entry<String, String>> itr = this.jsonText.entrySet().iterator();
            while(itr.hasNext()){
                Map.Entry entry = itr.next();
                /***************ParseJson************/
                JSONObject jb = new JSONObject(new JSONTokener((String)entry.getValue()));
                /***************Movie****************/
                Movie movieItem = new Movie();
                //asin
                movieItem.movieASIN = (String)entry.getKey();
                //format
                try{
                    movieItem.format = jb.getString("Format");
                }catch(Exception e){
                    //ignore
                }

                //movie name
                String[] candidateNameKey={"productTitle", "title", "movieTitle"};
                for(int i=0; i<3; i++){
                    try{
                        movieItem.movieName = jb.getString(candidateNameKey[i]);
                    }catch(Exception e){
                        continue;
                    }

                    if(null!=movieItem.movieName){
                        //edition
                        Integer cirPareLeftIndex = movieItem.movieName.indexOf("(");
                        Integer cirPareRightIndex = movieItem.movieName.indexOf(")");
                        Integer squPareLeftIndex = movieItem.movieName.indexOf("[");
                        if(cirPareLeftIndex>0){
                            if(cirPareRightIndex<cirPareLeftIndex){
                                cirPareRightIndex = movieItem.movieName.length();
                            }
                            movieItem.edition = movieItem.movieName.substring(cirPareLeftIndex+1,cirPareRightIndex);
                            movieItem.movieName = movieItem.movieName.substring(0,cirPareLeftIndex); // get pure name
                        }else if(squPareLeftIndex>0){
                            movieItem.movieName = movieItem.movieName.substring(0,squPareLeftIndex); // get pure name
                        }
                        //dimension
                        name_movie nm = new name_movie();
                        nm.productId = movieItem.movieASIN;
                        nameDimension nd = this.nameDimensionsTable.get(movieItem.movieName);
                        if(null != nd){
                            nm.nameId = nd.id;
                            movieItem.nameId = nm.nameId;
                            nd.count++;
                        }else{
                            nd = new nameDimension();
                            nd.count=1;
                            nd.id = (this.nameNextId++).toString();
                            nd.name = movieItem.movieName;
                            nm.nameId = nd.id;
                            movieItem.nameId = nm.nameId;
                            this.nameDimensionsTable.put(nd.name, nd);
                        }
                        this.name_moviesTable.add(nm);

                        break;
                    }

                }

                //time --DVD Release Date

                try{
                    String time = jb.getString("DVD Release Date");
                    if(null != time){
                        movieItem.timeId = time;
                        movieItem .publishTime = time;
                        time_movie tm = new time_movie();
                        tm.time = time;
                        tm.productId = movieItem.movieASIN;
                        timeDimension td = this.timeDimensionsTable.get(time);
                        if(null != td){
                            td.count++;
                        }else{
                            td = this.processTime(time);
                            td.count = 1;
                            this.timeDimensionsTable.put(time,td);
                        }
                        this.time_moviesTable.add(tm);
                    }
                }catch(Exception e){
                    //not found
                }



                //average comment
                String[] candidateACKey = {"averageCustomerReviews","reviewStars"};
                for(int i=0; i<2; i++){
                    try{
                        String strComm = jb.getString(candidateACKey[i]);
                        if(null != strComm){//找到了这个字段
                            Integer begIndex = strComm.indexOf(" out");
                            if(begIndex > 0){
                                movieItem.averageComment = Float.valueOf( strComm.substring(0, begIndex) );
                            }else{
                                movieItem.averageComment = 0.0f;
                            }
                            break;
                        }
                    }catch(Exception e){
                        continue;
                    }
                }

                String[] rateCandidateKey = {"Rated", "MPAA rating"};
                for(String rateKey: rateCandidateKey){
                    try{
                        String strRate = jb.getString(rateKey);
                        if(null != strRate){
                            movieItem.rate = strRate;
                            break;
                        }
                    }catch(Exception e){
                        continue;
                    }

                }


                //price
                try{
                    JSONArray otherFormat = jb.getJSONArray("otherFormat");
                    if(null != otherFormat){
                        String strPrice = otherFormat.getJSONObject(0).getString("value");
                        movieItem.price = Double.valueOf(strPrice.substring(strPrice.indexOf("$")+1));
                    }
                }catch(Exception e){
                    movieItem.price = 0.0;
                }

                //actors
                String[] starCandidateKey = {"Starring", "Actors","Supporting actors"};
                for(int i=0; i<3; i++){
                    try{
                        String value = jb.getString(starCandidateKey[i]);
                        if(null != value){
                            movieItem.actors = value;
                            String[] stars = value.split(", ");
                            //find in star dimension
                            for(String name: stars){
                                actorDimension starDim = null;
                                starDim = this.actorDimensionTable.get(name);
                                actor_movie am = new actor_movie();
                                am.productId = movieItem.movieASIN;
                                //star_dimension & support_dimension
                                if(null!=starDim){
                                    //找到了这个演员的记录
                                    am.actorId = starDim.id;
                                    movieItem.actorsId = movieItem.actorsId + am.actorId + "|" ;
                                    if(2 == i){
                                        starDim.supportCount++;
                                    }else{
                                        starDim.starringCount++;
                                    }

                                    starDim.starringCount++;
                                }else{
                                    //没有找到这个演员的记录
                                    //新建记录项，并插入表
                                    am.actorId = (this.actorNextId++).toString();
                                    movieItem.actorsId = movieItem.actorsId + am.actorId + "|";
                                    //新建维度项,并插入表
                                    actorDimension ad = new actorDimension();
                                    ad.id = am.actorId;
                                    if(2 == i){
                                        ad.starringCount = 1;
                                    }else{
                                        ad.supportCount = 1;
                                    }

                                    ad.actorName = name;
                                    this.actorDimensionTable.put(name, ad);
                                }
                                //插入到对应的记录表
                                if(2 != i){
                                    this.star_moviesTable.add(am);
                                }else{
                                    this.support_moviesTable.add(am);
                                }

                            }
                        }
                    }catch(Exception e){
                        continue;
                    }

                }

                //director
                String[] directorCandidateKey = {"Director", "Directors"};
                for(int i=0; i<2; i++){
                    try{
                        String value = jb.getString(directorCandidateKey[i]);
                        if(null!=value){
                            movieItem.directors = value;
                            String[] nameList = value.split(",");
                            for(String name: nameList){
                                directorDimension dirDim = null;
                                dirDim =this.directorDimensionsTable.get(name);
                                director_movie dm = new director_movie();
                                dm.productId = movieItem.movieASIN;
                                if(null != dirDim){
                                    dm.directorId = dirDim.id;
                                    movieItem.directId = movieItem.directId + dm.directorId + "|";
                                    dirDim.count++;
                                }else{
                                    dm.directorId = (this.directorNextId++).toString();
                                    movieItem.directId = movieItem.directId + dm.directorId + "|";
                                    directorDimension dd = new directorDimension();
                                    dd.count=1;
                                    dd.id=dm.directorId;
                                    dd.name = name;
                                    this.directorDimensionsTable.put(name, dd);
                                }
                                this.director_moviesTable.add(dm);
                            }
                        }
                    }catch(Exception e){
                        continue;
                    }
                }

                //category
                String categoryKey = "Amazon Best Sellers Rank";
                try{
                    String categoryStr = jb.getString(categoryKey);
                    if(null != categoryStr){
                        String[] cateStrList = categoryStr.split("#(.*?)in");
                        for(int i=2; i<cateStrList.length;i++){
                            String[] categoryList = cateStrList[i].split(" > ");
                            for(String cateItem: categoryList){
                                movieItem.category = movieItem.category + cateItem + "|";
                                categoryDimension cd = this.categoryDimensionsTable.get(cateItem);
                                category_movie cm = new category_movie();
                                cm.productId = movieItem.movieASIN;
                                if(null != cd){
                                    cd.count++;
                                    cm.categoryId = cd.id;
                                    movieItem.categoryId = movieItem.categoryId +cm.categoryId + "|";
                                }else{
                                    cd = new categoryDimension();
                                    cd.id = (this.categoryNextId++).toString();
                                    cd.category = cateItem;
                                    cd.count = 1;
                                    cm.categoryId = cd.id;
                                    movieItem.categoryId = movieItem.categoryId +cm.categoryId + "|";
                                    this.categoryDimensionsTable.put(cateItem, cd);
                                }
                                this.category_moviesTable.add(cm);

                            }
                        }


                    }
                }catch(Exception e){

                }
                this.movieTable.add(movieItem);
            }

            this.outputFile();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void outputFile(){
        try{
            //movie
            FileWriter movieWriter = new FileWriter("res/movie.csv",true);
            Iterator<Movie> itr = this.movieTable.iterator();
            while(itr.hasNext()){
                Movie item = itr.next();
                movieWriter.write(
                            "\"" + item.movieASIN       + "\"," +
                                "\"" + item.movieName       + "\"," +
                                "\"" + item.format          + "\"," +
                                "\"" + item.edition         + "\"," +
                                "\"" + item.price           + "\"," +
                                "\"" + item.publishTime     + "\"," +
                                "\"" + item.rate            + "\"," +
                                "\"" + item.directors       + "\"," +
                                "\"" + item.actors          + "\"," +
                                "\"" + item.category        + "\"," +
                                "\"" + item.timeId          + "\"," +
                                "\"" + item.nameId          + "\"," +
                                "\"" + item.categoryId      + "\"," +
                                "\"" + item.directId        + "\"," +
                                "\"" + item.actorsId        + "\"," +
                                "\"" + " "                  + "\r\n"
                );
            }
            movieWriter.close();

            //actorDimension
            FileWriter adWriter = new FileWriter("res/actorDimension.csv", true);
            Iterator<Map.Entry<String, actorDimension>> itrAD = this.actorDimensionTable.entrySet().iterator();
            while(itrAD.hasNext()){
                actorDimension item = itrAD.next().getValue();
                adWriter.write(item.id + ",\"" + item.actorName + "\"," + item.starringCount.toString() + "," + item.supportCount + "\r\n" );
            }

            //star_movie
            FileWriter starWriter = new FileWriter("res/starring_movie.csv", true);
            Iterator<actor_movie> itrAM = this.star_moviesTable.iterator();
            while(itrAM.hasNext()){
                actor_movie item = itrAM.next();
                starWriter.write(item.actorId.toString() + "," + item.productId + "\r\n");
            }

            //supporting_movie
            FileWriter supWriter = new FileWriter("res/supporting_movie.csv", true);
            Iterator<actor_movie> itrAMS = this.support_moviesTable.iterator();
            while(itrAMS.hasNext()){
                actor_movie item = itrAMS.next();
                supWriter.write(item.actorId.toString() + "," + item.productId + "\r\n");
            }

            //categoryDimension
            FileWriter cdWriter = new FileWriter("res/categoryDimension.csv", true);
            Iterator<Map.Entry<String,categoryDimension>> itrCD = this.categoryDimensionsTable.entrySet().iterator();
            while(itrCD.hasNext()){
                categoryDimension item = itrCD.next().getValue();
                cdWriter.write(item.id.toString() + ",\"" + item.category + "\"," + item.count.toString() + "\r\n");
            }

            //category_movie
            FileWriter cmWriter = new FileWriter("res/category_movie.csv", true);
            Iterator<category_movie> itrCM = this.category_moviesTable.iterator();
            while(itrCM.hasNext()){
                category_movie item = itrCM.next();
                supWriter.write(item.categoryId.toString() + "," + item.productId + "\r\n");
            }

            //directorDimension
            FileWriter ddWriter = new FileWriter("res/directorDimension.csv",true);
            Iterator<Map.Entry<String,directorDimension>> itrDD = this.directorDimensionsTable.entrySet().iterator();
            while(itrDD.hasNext()){
                directorDimension item = itrDD.next().getValue();
                ddWriter.write(item.id + ",\"" + item.name + "\"," + item.count.toString() + "\r\n");
            }

            //director_movie
            FileWriter dmWriter = new FileWriter("res/director_movie.csv", true);
            Iterator<director_movie> itrDM = this.director_moviesTable.iterator();
            while(itrDM.hasNext()){
                director_movie item = itrDM.next();
                dmWriter.write(item.directorId.toString() + "," + item.productId + "\r\n");
            }

            //nameDimension
            FileWriter ndWriter = new FileWriter("res/nameDimension.csv",true);
            Iterator<Map.Entry<String,nameDimension>> itrND = this.nameDimensionsTable.entrySet().iterator();
            while(itrND.hasNext()){
                nameDimension item = itrND.next().getValue();
                ndWriter.write(item.id + ",\"" + item.name + "\"," + item.count.toString() + "\r\n");
            }

            //name_movie
            FileWriter nmWriter = new FileWriter("res/name_movie.csv", true);
            Iterator<name_movie> itrNM = this.name_moviesTable.iterator();
            while(itrNM.hasNext()){
                name_movie item = itrNM.next();
                nmWriter.write(item.nameId.toString() + "," + item.productId + "\r\n");
            }

            //timeDimension
            FileWriter tdWriter = new FileWriter("res/timeDimension.csv",true);
            Iterator<Map.Entry<String,timeDimension>> itrTD = this.timeDimensionsTable.entrySet().iterator();
            while(itrTD.hasNext()){
                timeDimension item = itrTD.next().getValue();
                tdWriter.write("\"" +item.time + "\","+ item.year.toString() + "," + item.month.toString() + "," +
                        item.season.toString() + "," + item.weekday.toString() + "," + item.count.toString() + "\r\n");
            }

            //time_movie
            FileWriter tmWriter = new FileWriter("res/time_movie.csv", true);
            Iterator<time_movie> itrTM = this.time_moviesTable.iterator();
            while(itrTM.hasNext()){
                time_movie item = itrTM.next();
                tmWriter.write("\""+ item.time.toString() + "\"," + item.productId + "\r\n");
            }

        }catch(Exception e){

        }



    }


    //get data from database

    private void readDataFromDatabase(){
        try{
            //载入驱动程序
            Class.forName(this.databaseDriver);
            //创建连接
            Connection con = DriverManager.getConnection(this.databaseUrl,"root","qiangwudi");
            //创建语句对象
            Statement statement = con.createStatement();
            //语句
            String sqlSelect = "SELECT endUrl, jsonText FROM crawstatus WHERE state=1";
            //执行语句
            ResultSet resSet = statement.executeQuery(sqlSelect);

            int c = resSet.getMetaData().getColumnCount();
            while(resSet.next()){
                String json = resSet.getObject(2).toString();
                this.jsonText.put(resSet.getString(1), json);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private timeDimension processTime(String time){
        timeDimension ret = new timeDimension();
        ret.time = time;

        StringBuilder timeBuilder = new StringBuilder(time);
        timeBuilder.delete(timeBuilder.indexOf(","), timeBuilder.indexOf(",")+1);
        time = timeBuilder.toString();

        String day;
        String[] timeList = time.split(" ");
        String[] monthList = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        for(Integer i=1; i<=12; i++){
            if(0 == monthList[i-1].compareTo(timeList[0])){
                ret.season = ((i-1)%4)+1;
                ret.month=i;
                break;
            }
        }

        ret.year = Integer.valueOf(timeList[2]);
        day = timeList[1];

        Calendar calToGetWeek = Calendar.getInstance();
        calToGetWeek.set(ret.year,ret.month, Integer.valueOf(day));
        ret.weekday = calToGetWeek.get(Calendar.DAY_OF_WEEK);

        return ret;
    }


    private String sourceFile = "res.csv";
    private String databaseDriver = "com.mysql.cj.jdbc.Driver";
    private String databaseUrl = "jdbc:mysql://localhost:3306/datawarehouse?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private Map<String,String> jsonText = new HashMap<String, String>();

    static public void main(String[] argv){
        DataWasher dw = new DataWasher();
        dw.generateTable();
        //dw.preProcessMovieData();
    }
}
