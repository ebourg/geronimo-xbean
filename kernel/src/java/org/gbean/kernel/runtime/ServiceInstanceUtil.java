/**
 *
 * Copyright 2005 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gbean.kernel.runtime;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.management.ObjectName;

import org.gbean.kernel.Kernel;
import org.gbean.kernel.ServiceNotFoundException;

/**
 * @version $Rev$ $Date$
 */
public class ServiceInstanceUtil {
    public static Set getRunningTargets(Kernel kernel, Set patterns) {
        Set runningTargets = new HashSet();
        Set services = kernel.listServices(patterns);
        for (Iterator iterator = services.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            if (isRunning(kernel, objectName)) {
                runningTargets.add(objectName);
            }
        }
        return runningTargets;
    }

    /**
     * Is the component in the Running state
     *
     * @param objectName name of the component to check
     * @return true if the component is running; false otherwise
     */
    public static boolean isRunning(Kernel kernel, ObjectName objectName) {
        try {
            int state = kernel.getServiceState(objectName);
            return state == ServiceState.RUNNING_INDEX;
        } catch (ServiceNotFoundException e) {
            // service is no longer registerd
            return false;
        }
    }
}