package com.wanglu.cn;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class UserTest {
	public static void main(String[] args) {
		//解析标签在DefaultBeanDefinitionDocumentReader
		ApplicationContext bf =new ClassPathXmlApplicationContext("spring.xml");
		User user=(User)bf.getBean("testBean");
		System.out.println(user.getUserName()+","+user.getEmail());
	}
}
