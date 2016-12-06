package info.hbraun.resolver.aether;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;


/**
 * A helper to boot the repository system and a repository system session.
 */
public class Booter
{

    static RepositoryPolicy DEFAULT_POLICY = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_IGNORE);

    public static DefaultRepositorySystemSession newRepositorySystemSession( RepositorySystem system )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        String localM2 = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        LocalRepository localRepo = new LocalRepository(localM2);
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        //session.setTransferListener( new ConsoleTransferListener() );
        //session.setRepositoryListener( new ConsoleRepositoryListener() );

        return session;
    }

    public static List<RemoteRepository> newRepositories( RepositorySystem system, RepositorySystemSession session )
    {
        String localM2 = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";


        return new ArrayList<RemoteRepository>(Arrays.asList(
                new RemoteRepository.Builder("local", "default", "file:/"+localM2)
                        .setPolicy(DEFAULT_POLICY).build(),
                newCentralRepository()
        ) );
    }

    private static RemoteRepository newCentralRepository()
    {
        return new RemoteRepository.Builder( "central", "default", "http://central.maven.org/maven2/" )
                .setPolicy(DEFAULT_POLICY).build();
    }

    public static RepositorySystem newRepositorySystem()
    {
           /*
            * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
            * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
            * factories.
            */
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService(TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
            {
                exception.printStackTrace();
            }
        } );

        return locator.getService( RepositorySystem.class );
    }

}
