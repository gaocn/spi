package govind.javaspi;

import org.junit.Test;

import java.util.ServiceLoader;

public class TestRobot {
	@Test
	public void sayHello() {
		ServiceLoader<Robot>  loader = ServiceLoader.load(Robot.class);
		System.out.println("Java SPI");
		loader.forEach(Robot::sayHello);
	}
}
