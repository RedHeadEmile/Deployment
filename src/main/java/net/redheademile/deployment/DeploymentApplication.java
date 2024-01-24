package net.redheademile.deployment;

import net.redheademile.deployment.exceptions.InvalidConfigurationException;
import net.redheademile.deployment.sevices.IConfigurationService;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

import java.security.Security;

@SpringBootApplication
@EnableAsync
public class DeploymentApplication {

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastlePQCProvider());

		ConfigurableApplicationContext context = SpringApplication.run(DeploymentApplication.class, args);
		try {
			context.getBean(IConfigurationService.class).reloadConfigurationAndEnsureValidity();
		}
		catch (InvalidConfigurationException e) {
			e.printStackTrace();
			context.close();
		}
	}

}
