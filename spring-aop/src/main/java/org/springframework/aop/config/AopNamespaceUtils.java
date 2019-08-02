/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;

/**
 * Utility class for handling registration of auto-proxy creators used internally
 * by the '{@code aop}' namespace tags.
 *
 * <p>Only a single auto-proxy creator should be registered and multiple configuration
 * elements may wish to register different concrete implementations. As such this class
 * delegates to {@link AopConfigUtils} which provides a simple escalation protocol.
 * Callers may request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 * @see AopConfigUtils
 */
public abstract class AopNamespaceUtils {

	/**
	 * The {@code proxy-target-class} attribute as found on AOP-related XML tags.
	 */
	public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

	/**
	 * The {@code expose-proxy} attribute as found on AOP-related XML tags.
	 */
	private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";


	public static void registerAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	public static void registerAspectJAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {

		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {
		//注册或者升级AnnotationAwareAspectJAutoProxyCreator
		BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
				parserContext.getRegistry(), parserContext.extractSource(sourceElement));
		//处理proxy-target-class以及expose-proxy属性
		useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
		//注册组件并通知
		registerComponentIfNecessary(beanDefinition, parserContext);
	}

	private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, @Nullable Element sourceElement) {
		if (sourceElement != null) {
			//对于proxy-target-class的处理
			//如果被代理的目标对象实现了至少一个接口，则会使用jdk的动态代理，所有目标类型实现的接口都将被代理。
			//若该目标对象没有实现任何接口，则创建一个cglib代理，需要设置proxy-target-class=true
			//jdk动态代理，其代理类必须是接口得实现
			//cglib生成代理对象是目标类的子类，底层是依靠asm操作字节码实现的，性能比jdk强
			boolean proxyTargetClass = Boolean.parseBoolean(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
			if (proxyTargetClass) {
				//强制使用的过程其实是一个属性设置的过程
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
			//对于expose-proxy的处理
			//有目标对象内部的自我调用将无法实施切面中的增强
			//public interface AService { 　
			//	public void a(); 　
			//	public void b();
			//}
			//@Service()
			//public class AServiceImpl1 implements AService{ 　

			//	@Transactional( propagation = Propagation. REQUIRED)
			//	public void a() { 　 　
			//		this. b(); 　
			//	} 　
			//	@Transactional( propagation = Propagation. REQUIRES_ NEW) 　
			//	public void b() {
			//}
			//此处的this指向目标对象，因此调用this. b()将不会执行b事务切面， 即不会执行事务增强，
			// 因此b方法的事务定义“@ Transactional( propagation = Propagation. REQUIRES_ NEW)” 将不会实施，
			// 为了解决这个问题， 我们 可以 这样做： < aop: aspectj- autoproxy expose- proxy=" true"/>
			//然后将以上代码中的“ this. b();” 修改 为“(( AService) AopContext.currentProxy()).b();” 即可
			// 通过以上的修改便可以完成对a和b方法的同时增强。
			//https://blog.csdn.net/a1063916463/article/details/53761637
			//AspectJAwareAdvisorAutoProxyCreator
			//AbstractAutoProxyCreator
			boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
			if (exposeProxy) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

	private static void registerComponentIfNecessary(@Nullable BeanDefinition beanDefinition, ParserContext parserContext) {
		if (beanDefinition != null) {
			parserContext.registerComponent(
					new BeanComponentDefinition(beanDefinition, AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME));
		}
	}

}
