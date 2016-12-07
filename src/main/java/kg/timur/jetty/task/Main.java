package kg.timur.jetty.task;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main
{
    private static Logger LOG = LoggerFactory.getLogger( Main.class );


    public static void main( String[] args ) throws Exception
    {
        WebServer webServer = new WebServer( 80 );
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
