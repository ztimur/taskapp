package kg.timur.jetty.task;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;

import kg.timur.jetty.task.model.Task;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static kg.timur.jetty.task.model.Task.ID;
import static kg.timur.jetty.task.model.Task.KEYSPACE;
import static kg.timur.jetty.task.model.Task.TABLE;


public class CassandraClient
{
    private static Logger LOG = LoggerFactory.getLogger( CassandraClient.class );
    private Cluster cluster;

    private Session session;
    private MappingManager mappingManager;


    public void connect( String[] contactPoints, int port )
    {

        LOG.info( "Connecting to Cassandra..." );

        cluster = Cluster.builder().addContactPoints( contactPoints ).withPort( port )
                         /*.withCredentials( "cassandra", "cassandra" )*/.build();

        session = cluster.connect();

        LOG.info( "Connected to cluster: {}", cluster.getMetadata().getClusterName() );

        mappingManager = new MappingManager( session );
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
        LOG.debug( "Querying data...." );

        final Statement statement =
                QueryBuilder.select().all().from( KEYSPACE, TABLE ).setConsistencyLevel( ConsistencyLevel.ONE );

        ResultSet results = session.execute( statement );

        LOG.debug( "Result set was applied: {}. Session state: {}", results.wasApplied(), session.getState() );

        Result<Task> result = getMapper().map( results );
        return result.all();
    }


    public Task getTask( final String id )
    {
        final Statement statement = QueryBuilder.select().from( KEYSPACE, TABLE ).where( eq( ID, id ) ).limit( 1 )
                                                .setConsistencyLevel( ConsistencyLevel.ONE );
        final ResultSet results = session.execute( statement );

        final Result<Task> r = getMapper().map( results );
        List<Task> all = r.all();
        if ( all.size() > 0 )
        {
            return all.get( 0 );
        }
        else
        {
            return null;
        }
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


    private Mapper<Task> getMapper()
    {
        return mappingManager.mapper( Task.class );
    }
}
