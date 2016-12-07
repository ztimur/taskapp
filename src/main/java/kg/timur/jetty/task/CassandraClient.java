package kg.timur.jetty.task;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;

import kg.timur.jetty.task.model.Task;


public class CassandraClient
{
    private static Logger LOG = LoggerFactory.getLogger( CassandraClient.class );
    private Cluster cluster;

    private Session session;


    public void connect( String[] contactPoints, int port )
    {

        LOG.info( "Connecting to Cassandra..." );

        cluster = Cluster.builder().addContactPoints( contactPoints ).withPort( port )
                         /*.withCredentials( "cassandra", "cassandra" )*/.build();

        session = cluster.connect();

        LOG.info( "Connected to cluster: {}", cluster.getMetadata().getClusterName() );
    }


    public TableMetadata getTableMetadata( String tableName )
    {
        final KeyspaceMetadata keyspaceMetadata = cluster.getMetadata().getKeyspace( "taskdemo" );
        if ( keyspaceMetadata == null )
        {
            return null;
        }
        return keyspaceMetadata.getTable( tableName );
    }


    public void createSchema( List<String> initScripts )
    {
        LOG.info( "Creating schema...." );

        for ( String s : initScripts )
        {
            session.execute( s );
        }
        LOG.info( "Schema created" );
    }


    public List<Task> getAllTasks()
    {

        List<Task> tasks = new ArrayList<>();

        LOG.debug( "Querying data...." );

        ResultSet results = session.execute( "SELECT id, task, status, createdOn FROM taskdemo.tasks" );

        LOG.debug( "Fetch result: {}", results );
        for ( Row row : results )
        {
            tasks.add( new Task( row.getString( 0 ), row.getString( 1 ), row.getInt( 2 ), row.getTimestamp( 3 ) ) );
        }

        return tasks;
    }


    public Task getTask( final String id )
    {
        final ResultSet results =
                session.execute( "select id, task, status, createdOn from taskdemo.tasks where id = ?;", id );
        Row row = results.one();
        if ( row == null )
        {
            return null;
        }
        return new Task( row.getString( 0 ), row.getString( 1 ), row.getInt( 2 ), row.getTimestamp( 3 ) );
    }


    public void addTask( Task task )
    {
        session.execute( "insert into taskdemo.tasks(id, task, status, createdOn) values(?, ?, ?, ?);", task.getId(),
                task.getTask(), task.getStatus(), task.getCreatedOn() );
    }


    public void removeTask( final String id )
    {
        session.execute( "DELETE FROM taskdemo.tasks WHERE id = ?;", id );
    }


    public List<String> getCql()
    {
        InputStream in = getClass().getResourceAsStream( "/tasks.cql" );

        if ( in == null )
        {
            LOG.warn( "Cassandra init CQL file not found." );
            return null;
        }

        List<String> result = new ArrayList<>();

        try ( Scanner scanner = new Scanner( in ) )
        {
            while ( scanner.hasNextLine() )
            {
                String line = scanner.nextLine();
                result.add( line );
            }

            scanner.close();
        }
        catch ( Exception e )
        {
            LOG.error( "Error reading init CQL file: " + e.getMessage() );
        }
        return result;
    }


    public void close()
    {
        try
        {
            session.close();
            cluster.close();
        }
        catch ( Exception ignore )
        {
        }
    }
}
