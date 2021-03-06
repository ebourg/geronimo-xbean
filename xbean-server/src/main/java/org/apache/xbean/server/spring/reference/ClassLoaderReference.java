/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.server.spring.reference;

import java.io.Serializable;

import org.apache.xbean.kernel.ServiceContext;
import org.apache.xbean.kernel.ServiceContextThreadLocal;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * ClassLoaderReference is a the class loader in the ServiceContextThreadLocal.
 * @author Dain Sundstrom
 * @version $Id$
 * @since 2.0
 */
public class ClassLoaderReference implements FactoryBean, Serializable {
    /**
     * Creates a bean definition for ServiceContextThreadLocal.
     * @return a bean definition for ServiceContextThreadLocal
     */
    public static BeanDefinitionHolder createBeanDefinition() {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(ClassLoaderReference.class, 0);
        return new BeanDefinitionHolder(beanDefinition, ClassLoaderReference.class.getName());
    }

    public final Class getObjectType() {
        return ClassLoader.class;
    }

    public synchronized final Object getObject() {
        ServiceContext serviceContext = ServiceContextThreadLocal.get();
        if (serviceContext == null) {
            throw new IllegalStateException("Service context has not been set");
        }
        return serviceContext.getClassLoader();
    }

    public boolean isSingleton() {
        return true;
    }
}
