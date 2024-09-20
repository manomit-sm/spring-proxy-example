package com.bsolz.spring_proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class SpringProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringProxyApplication.class, args);

	}

	static boolean transactional(Object o) {
		var hasTransactional = new AtomicBoolean(false);
		var classes = new ArrayList<Class<?>>();
		classes.add(o.getClass());
		Collections.addAll(classes, o.getClass().getInterfaces());
		classes.forEach(clzz -> ReflectionUtils.doWithMethods(o.getClass(), method -> {
			if (method.getAnnotation(MyTransactional.class) != null)
				hasTransactional.set(true);
		}));

		return hasTransactional.get();
	}

	@Bean
	DefaultCustomerService defaultCustomerService() {
		return new DefaultCustomerService();
	}

	@Bean
	static MyTransactionalBeanPostProcessor myTransactionalBeanPostProcessor() {
		return new MyTransactionalBeanPostProcessor();
	}
	static class MyTransactionalBeanPostProcessor implements BeanPostProcessor {
		@Override
		public Object postProcessAfterInitialization(Object target, String beanName) throws BeansException {
			if (transactional(target)) {
				var pf = new ProxyFactory();
				pf.setInterfaces(target.getClass().getInterfaces());
				pf.setTarget(target);
				pf.addAdvice((MethodInterceptor) invocation -> {
					Method method = invocation.getMethod();
					final Object[] arguments = invocation.getArguments();
					System.out.println("Calling " + method.getName());
					try {
						if (method.getAnnotation(MyTransactional.class) != null){
							System.out.println("Transaction started");
						}
						return method.invoke(target, arguments);
					} finally {
						if (method.getAnnotation(MyTransactional.class) != null){
							System.out.println("Transaction ended");
						}
					}
				});

				return pf.getProxy(getClass().getClassLoader());
			}
			return BeanPostProcessor.super.postProcessAfterInitialization(target, beanName);
		}
	}
	@Bean
	ApplicationRunner applicationRunner(CustomerService customerService) {
		return new ApplicationRunner() {
			@Override
			public void run(ApplicationArguments args) throws Exception {
				customerService.create();
			}
		};
	}


	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Reflective
	@interface MyTransactional { }

	class DefaultCustomerService implements CustomerService {

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
