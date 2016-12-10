package kg.timur.jetty.task.service;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kg.timur.jetty.task.CassandraClient;
import kg.timur.jetty.task.model.GenericResponse;
import kg.timur.jetty.task.model.Task;


@Path( "/rest/tasks" )
@Consumes( MediaType.APPLICATION_JSON )
@Produces( MediaType.APPLICATION_JSON )
public class TaskServiceImpl implements TaskService
{
    private static Logger LOG = LoggerFactory.getLogger( TaskServiceImpl.class );

    private static int CASS_PORT = 9042;

    private String[] CASS_CONFIG_POINTS = { "127.0.0.1" };

    private CassandraClient cassandraClient;


    public TaskServiceImpl()
    {
        LOG.info( "Initializing TaskServiceImpl......" );
        this.cassandraClient = new CassandraClient();
        try
        {
            initCassandra();
        }
        catch ( InterruptedException | IOException e )
        {
            e.printStackTrace();
        }
    }


    private void initCassandra() throws InterruptedException, IOException
    {
        cassandraClient = new CassandraClient();

        boolean notConnected = true;


        while ( !Thread.interrupted() && notConnected )
        {
            updateConfigs3();
            try
            {
                cassandraClient.connect( CASS_CONFIG_POINTS, CASS_PORT );

                notConnected = false;
            }
            catch ( Exception e )
            {
                LOG.error( e.getMessage() );
                TimeUnit.SECONDS.sleep( 1 );
            }
        }

        if ( cassandraClient.getTableMetadata( "tasks" ) == null )
        {
            List<String> script = cassandraClient.getCql();

            LOG.info( "tasks.cql: {}", script );

            cassandraClient.createSchema( script );
        }
    }


    private void updateConfigs3() throws SocketException
    {
        File cassandraConfigFile = new File( "/etc/cassandra/cassandra.yaml" );
        Pattern p = Pattern.compile( "\\s*-\\s*seeds\\s*:\\s*\"(.*)\"", Pattern.CASE_INSENSITIVE );

        BufferedReader bf = null;
        String seeds = null;
        try
        {
            bf = new BufferedReader( new FileReader( cassandraConfigFile ) );
            int linecount = 0;
            String line;
            while ( ( line = bf.readLine() ) != null && seeds == null )
            {
                linecount++;

                Matcher m = p.matcher( line );

                if ( m.find() )
                {
                    seeds = m.group( 1 );
                    LOG.debug( "Seed definition found at position {} on line {}. Seeds: {}", m.start(), linecount,
                            m.group() );
                }
            }
        }
        catch ( IOException e )
        {
            LOG.error( e.getMessage(), e );
        }
        finally
        {
            if ( bf != null )
            {
                try
                {
                    bf.close();
                }
                catch ( IOException ignore )
                {
                    // ignore
                }
            }
        }

        String defaultIp = getEth0Ip();
        if ( defaultIp == null )
        {
            defaultIp = "127.0.0.1";
        }

        if ( seeds == null )
        {
            LOG.warn( "Seeds configuration not found in cassandra configuration file. Using {} for connections.",
                    defaultIp );
            CASS_CONFIG_POINTS = new String[] { defaultIp };
        }
        else
        {
            String[] hostList = seeds.trim().split( "," );
            Set<String> configPoints = new HashSet<>();
            configPoints.add( defaultIp );
            for ( String host : hostList )
            {
                LOG.debug( "Adding C* config point {}.", host );
                configPoints.add( host );
            }
            CASS_CONFIG_POINTS = configPoints.toArray( new String[0] );
            LOG.debug( "CASS_CONFIG_POINTS: {}", configPoints );
        }
    }


    private String getEth0Ip() throws SocketException
    {
        NetworkInterface networkInterface = NetworkInterface.getByName( "eth0" );

        for ( InetAddress inetAddress : Collections.list( networkInterface.getInetAddresses() ) )
        {
            if ( inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress() )
            {
                return inetAddress.getHostAddress();
            }
        }
        return null;
    }


    private void updateConfigs2() throws SocketException
    {
        NetworkInterface networkInterface = NetworkInterface.getByName( "eth0" );

        for ( InetAddress inetAddress : Collections.list( networkInterface.getInetAddresses() ) )
        {
            if ( inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress() )
            {
                CASS_CONFIG_POINTS = new String[] { inetAddress.getHostAddress() };

                return;
            }
        }
    }


    private boolean updateConfigs()
    {
        Supplier<Stream<String>> streamSupplier = () ->
        {
            try
            {
                return Files.lines( Paths.get( "/etc/cassandra/cassandra.yaml" ) );
            }
            catch ( IOException e )
            {
                return Stream.empty();
            }
        };

        Optional<String> cassPortOpt =
                streamSupplier.get().filter( l -> l.contains( "native_transport_port" ) ).findFirst();

        if ( cassPortOpt.isPresent() )
        {
            CASS_PORT = Integer.parseInt( cassPortOpt.get().split( ":" )[1].trim() );
        }


        Optional<String> cassIpsOpt = streamSupplier.get().filter( l -> l.contains( "seeds:" ) ).findFirst();

        if ( cassIpsOpt.isPresent() )
        {
            CASS_CONFIG_POINTS = cassIpsOpt.get().split( ":" )[1].trim().replace( "\"", "" ).split( "," );
        }

        return !Arrays.equals( CASS_CONFIG_POINTS, new String[] { "127.0.0.1" } );
    }


    @Override
    @POST
    @Path( "/add" )
    public Response addTask( Task e )
    {
        GenericResponse response = new GenericResponse();
        if ( cassandraClient.getTask( e.getId() ) != null )
        {
            response.setStatus( false );
            response.setMessage( "Task Already Exists" );
            response.setErrorCode( "EC-01" );
            return Response.status( 422 ).entity( response ).build();
        }
        cassandraClient.addTask( e );
        response.setStatus( true );
        response.setMessage( "Task created successfully" );
        return Response.ok( response ).build();
    }


    @Override
    @DELETE
    @Path( "/{id}/delete" )
    public Response deleteTask( @PathParam( "id" ) String id )
    {
        GenericResponse response = new GenericResponse();
        if ( cassandraClient.getTask( id ) == null )
        {
            response.setStatus( false );
            response.setMessage( "Task Doesn't Exists" );
            response.setErrorCode( "EC-02" );
            return Response.status( 404 ).entity( response ).build();
        }
        cassandraClient.removeTask( id );
        response.setStatus( true );
        response.setMessage( "Task deleted successfully" );
        return Response.ok( response ).build();
    }


    @Override
    @PUT
    @Path( "/{id}/update/{status}" )
    public Response updateTask( @PathParam( "id" ) String id, @PathParam( "status" ) int status )
    {
        GenericResponse response = new GenericResponse();
        if ( cassandraClient.getTask( id ) == null )
        {
            response.setStatus( false );
            response.setMessage( "Task Doesn't Exists" );
            response.setErrorCode( "EC-03" );
            return Response.status( 404 ).entity( response ).build();
        }
        Task task = cassandraClient.getTask( id );
        task.setStatus( status );

        cassandraClient.addTask( task );
        response.setStatus( true );
        response.setMessage( "Task updated successfully" );
        return Response.ok( response ).build();
    }


    @Override
    @GET
    @Path( "/{id}/get" )
    public Task getTask( @PathParam( "id" ) String id )
    {
        return cassandraClient.getTask( id );
    }


    @GET
    @Path( "/{id}/getDummy" )
    public Task getDummyTask( @PathParam( "id" ) String id )
    {
        return new Task( id, "Dummy", 0, new Date() );
    }


    @Override
    @GET
    @Path( "/getAll" )
    public Task[] getAllTasks()
    {
        return cassandraClient.getAllTasks().toArray( new Task[0] );
    }


    @Override
    @GET
    @Path( "/getClusterStatus" )
    public String getClusterStatus()
    {
        final StringBuilder result = new StringBuilder("Cluster status:\n");
        try
        {
            final Process p = Runtime.getRuntime().exec( "nodetool status" );

            new Thread( () ->
            {
                BufferedReader input = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
                String line = null;

                try
                {
                    while ( ( line = input.readLine() ) != null )
                    {
                        result.append( line +"\n");
                    }
                }
                catch ( IOException e )
                {
                    LOG.error( e.getMessage() );
                    result.append( e.getMessage() );
                }
            } ).start();

            p.waitFor();
        }
        catch ( IOException | InterruptedException e )
        {
            LOG.error( e.getMessage() );
            result.append( e.getMessage() );
        }

        return result.toString();
    }
}
