package com.bsolz.spring_proxy;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@SpringBootApplication
public class SpringProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringProxyApplication.class, args);

	}

	@Bean
	ApplicationRunner applicationRunner() {
		return new ApplicationRunner() {
			@Override
			public void run(ApplicationArguments args) throws Exception {
				var defaultCustomer = new DefaultCustomerService();

				final CustomerService proxyInstance = (CustomerService) Proxy.newProxyInstance(
						defaultCustomer.getClass().getClassLoader(),
						defaultCustomer.getClass().getInterfaces(),
						(proxy, method, args1) -> {
							System.out.println("Calling " + method.getName());
							try {
								if (method.getAnnotation(MyTransactional.class) != null){
									System.out.println("Transaction started");
								}
								return method.invoke(defaultCustomer, args1);
							} finally {
								if (method.getAnnotation(MyTransactional.class) != null){
									System.out.println("Transaction ended");
								}
							}
						}
				);
				proxyInstance.create();
			}
		};
	}


	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Reflective
	@interface MyTransactional { }

	static class DefaultCustomerService implements CustomerService {

		@Override
		public void create() {
			System.out.println("create()");
		}
	}

	interface CustomerService {

		@MyTransactional
		void create();
	}
}
