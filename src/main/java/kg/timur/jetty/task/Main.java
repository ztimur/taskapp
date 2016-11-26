package kg.timur.jetty.task;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by tzhamakeev on 11/25/16.
 */
public class Main
{
    private static Logger LOG = LoggerFactory.getLogger( Main.class );


    public static void main( String[] args ) throws Exception
    {
        WebServer webServer = new WebServer( 8080 );
        try
        {
            webServer.start();
            webServer.join();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            webServer.stop();
        }
    }
}
