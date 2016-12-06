package info.hbraun.resolver;

import java.io.File;
import java.nio.file.Paths;

import info.hbraun.resolver.aether.AetherDemo;
import info.hbraun.resolver.shrinkwrap.ShrinkwrapDemo;

/**
 * @author Heiko Braun
 * @since 06/12/2016
 */
public class Main {

    public Main(File pomFile) {
        this.pomFile = pomFile;

    }

    public static void main(String[] args) throws Exception {

        File pom = Paths.get("/Users/hbraun/dev/prj/swarm-cases/wfs-demo-jaxrs/pom.xml").toFile();
        Main main = new Main(pom);
        main.run();

    }

    private void run() throws Exception {
        timed(new AetherDemo());
        timed(new ShrinkwrapDemo());
    }

    private void timed(Demo demo) {
        long s0 = System.currentTimeMillis();
        try {
            demo.execute(pomFile);
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println((System.currentTimeMillis()-s0) + " ms");
        }
    }

    private final File pomFile;

}
