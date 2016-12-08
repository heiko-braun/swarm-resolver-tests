package info.hbraun.resolver.shrinkwrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info.hbraun.resolver.Demo;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.wildfly.swarm.arquillian.resolver.FailureReportingTransferListener;
import org.wildfly.swarm.arquillian.resolver.ShrinkwrapArtifactResolvingHelper;

/**
 * @author Heiko Braun
 * @since 06/12/2016
 */
public class ShrinkwrapNew implements Demo {
    @Override
    public void execute(File pomFile) throws Exception {

        ConfigurableMavenResolverSystem resolver = ShrinkwrapDemo.RESOLVER_INSTANCE;

        DefaultRepositorySystemSession session = ShrinkwrapDemo.session(resolver);
        session.setTransferListener(new FailureReportingTransferListener());

        ShrinkwrapArtifactResolvingHelper resolvingHelper = new ShrinkwrapArtifactResolvingHelper(resolver);
        PomEquippedResolveStage pomEquipped = loadPom(resolver, pomFile);

        // NonTransitiveStrategy
        final MavenResolvedArtifact[] explicitDeps =
                 resolvingHelper.withResolver(r -> pomEquipped
                        .importRuntimeAndTestDependencies()
                        .resolve()
                        .withoutTransitivity()
                        .asResolvedArtifact()
                 );

        // TransitiveStrategy
        // buckets
        List<MavenResolvedArtifact> transientDeps = new ArrayList<>();
        for (MavenResolvedArtifact directDep : explicitDeps) {
            MavenResolvedArtifact[] bucket = resolvingHelper.withResolver(
                    r -> pomEquipped
                    .resolve(directDep.getCoordinate().toCanonicalForm())
                    .withTransitivity()
                    .asResolvedArtifact()
            );

            transientDeps.addAll(Arrays.asList(bucket));
        }

        System.out.println(getClass().getSimpleName());
        System.out.println("Direct: "+explicitDeps.length);
        System.out.println("Transitive: "+transientDeps.size());
        System.out.println();
    }

    public static PomEquippedResolveStage loadPom(ConfigurableMavenResolverSystem resolver, File pom) {
        return resolver.loadPomFromFile(pom);
    }


}
