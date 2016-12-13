/**
 * Created by deado on 2016/12/14.
 */
public class MultiThreadController extends Thread{
    private InfoRetriever infoRetriever ;
    MultiThreadController(String filename){
        this.infoRetriever = new InfoRetriever(filename);
    }

    public void run(){
        this.infoRetriever.mainController();
    }

    static public void main(String[] argv){
        MultiThreadController thread_1 = new MultiThreadController("MovieUrl_1.txt");
        MultiThreadController thread_2 = new MultiThreadController("MovieUrl_2.txt");
        thread_1.start();
        thread_2.start();

    }
}
