package info.hbraun.resolver.aether;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import info.hbraun.resolver.Demo;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * See http://git.eclipse.org/c/aether/aether-demo.git/tree/aether-demo-snippets
 *
 * @author Heiko Braun
 * @since 06/12/2016
 */
public class AetherDemo implements Demo {

    public AetherDemo() {
        system = Booter.newRepositorySystem();
    }

    public void execute(File pomFile) throws Exception {
        MavenProject proj = loadFromPom(pomFile);
        final List<org.apache.maven.model.Dependency> explicitDeps = proj.getDependencies();

        List<Dependency> aetherDeps = explicitDeps
                .stream()
                .map( d -> {
                    // TODO: currently fails to resolve dependencyManagement sections (and very likely others parts as well, i.e. properties, etc)
                    String version = d.getVersion() !=null ? d.getVersion() : "2017.1.0-SNAPSHOT";
                    DefaultArtifact a = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getType(), version);
                    return new Dependency(a, d.getScope());
                })
                .collect(Collectors.toList());

        List<ArtifactResult> transientDeps = resolveTransient(aetherDeps);
        List<ArtifactResult> resolvedExplicitDeps = transientDeps.stream()
                .filter(d -> isDirectDep(d, explicitDeps))
                .collect(Collectors.toList());

        System.out.println(getClass().getSimpleName());
        System.out.println("Direct: "+resolvedExplicitDeps.size());
        System.out.println("Transitive: "+transientDeps.size());
        System.out.println();
    }

    private boolean isDirectDep(ArtifactResult aetherDep, List<org.apache.maven.model.Dependency> explicitDeps) {
        boolean match = false;
        for(org.apache.maven.model.Dependency mavenDep : explicitDeps) {
            if(aetherDep.getArtifact().getArtifactId().equals(mavenDep.getArtifactId())
                    && aetherDep.getArtifact().getGroupId().equals(mavenDep.getGroupId())) {
                match = true;
                break;
            }
        }
        return match;
    }

    private List<ArtifactResult> resolveTransient(List<Dependency> deps) throws  Exception {

        RepositorySystemSession session = Booter.newRepositorySystemSession(system);

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME, JavaScopes.TEST);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setDependencies(deps);
        collectRequest.setRepositories( Booter.newRepositories( system, session ) );

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter );

        List<ArtifactResult> artifactResults = system.resolveDependencies( session, dependencyRequest ).getArtifactResults();

        return artifactResults;
    }


    private MavenProject loadFromPom(File pomFile) throws Exception {
        MavenProject proj = loadProject(pomFile);

        proj.setRemoteArtifactRepositories(
                Arrays.asList(
                        (ArtifactRepository) new MavenArtifactRepository(
                                "maven-central", "http://repo1.maven.org/maven2/", new DefaultRepositoryLayout(),
                                new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy()
                        ),
                        (ArtifactRepository) new MavenArtifactRepository(
                                "local", "file://Users/hbraun/.m2/repository", new DefaultRepositoryLayout(),
                                new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy()
                        )
                )
        );


        return proj;
    }

    public static MavenProject loadProject(File pomFile) throws IOException, XmlPullParserException
    {
        MavenProject ret = null;
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();

        if (pomFile != null && pomFile.exists())
        {
            FileReader reader = null;

            try
            {
                reader = new FileReader(pomFile);
                Model model = mavenReader.read(reader);
                model.setPomFile(pomFile);

                ret = new MavenProject(model);
            }
            finally
            {
                reader.close();
            }
        }

        return ret;
    }


    private final RepositorySystem system;
}
