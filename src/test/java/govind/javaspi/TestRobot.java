package govind.javaspi;

import govind.spi.ExtensionLoader;
import org.junit.Test;

import java.util.ServiceLoader;

public class TestRobot {
	@Test
	public void testJavaSPI() {
		ServiceLoader<Robot>  loader = ServiceLoader.load(Robot.class);
		System.out.println("Java SPI");
		loader.forEach(Robot::sayHello);
	}

	@Test
	public void testDubboSPI() {
		ExtensionLoader<Robot> loader = ExtensionLoader.getExtensionLoader(Robot.class);
		Robot optimusPrime = loader.getExtension("optimusPrime");
  		optimusPrime.sayHello();

		Robot bumblebee = loader.getExtension("bumblebee");
		bumblebee.sayHello();
	}

}
