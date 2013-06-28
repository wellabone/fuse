/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.groups.internal;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.NodeState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class OsgiManagedGroupFactory implements ManagedGroupFactory {

    private final ManagedGroupFactory delegate;

    public OsgiManagedGroupFactory(ClassLoader loader) {
        this.delegate = new OsgiTrackingManagedGroupFactory(loader);
    }

    @Override
    public CuratorFramework getCurator() {
        return delegate.getCurator();
    }

    @Override
    public <T> Group<T> createGroup(String path, Class<T> clazz) {
        return delegate.createGroup(path, clazz);
    }

    @Override
    public void close() {
        delegate.close();
    }

    static class OsgiTrackingManagedGroupFactory implements ManagedGroupFactory, ServiceTrackerCustomizer<CuratorFramework, CuratorFramework> {

        private final BundleContext bundleContext;
        private final ServiceTracker<CuratorFramework, CuratorFramework> tracker;
        private CuratorFramework curator;
        private final List<DelegateZooKeeperGroup<?>> groups = new ArrayList<DelegateZooKeeperGroup<?>>();

        OsgiTrackingManagedGroupFactory(ClassLoader loader) {
            this(((BundleReference) loader).getBundle().getBundleContext());
        }

        OsgiTrackingManagedGroupFactory(BundleContext bundleContext) {
            this.bundleContext = bundleContext;
            this.tracker = new ServiceTracker<CuratorFramework, CuratorFramework>(
                    bundleContext, CuratorFramework.class, this);
            this.tracker.open();
        }

        @Override
        public CuratorFramework addingService(ServiceReference<CuratorFramework> reference) {
            CuratorFramework curator = OsgiTrackingManagedGroupFactory.this.bundleContext.getService(reference);
            useCurator(curator);
            return curator;
        }

        @Override
        public void modifiedService(ServiceReference<CuratorFramework> reference, CuratorFramework service) {
        }

        @Override
        public void removedService(ServiceReference<CuratorFramework> reference, CuratorFramework service) {
            useCurator(null);
            OsgiTrackingManagedGroupFactory.this.bundleContext.ungetService(reference);
        }

        protected void useCurator(CuratorFramework curator) {
            this.curator = curator;
            for (DelegateZooKeeperGroup<?> group : groups) {
                group.useCurator(curator);
            }
        }

        @Override
        public CuratorFramework getCurator() {
            return curator;
        }

        @Override
        public <T> Group<T> createGroup(String path, Class<T> clazz) {
            return new DelegateZooKeeperGroup<T>(path, clazz) {
                @Override
                public void start() {
                    useCurator(curator);
                    groups.add(this);
                    super.start();
                }

                @Override
                public void close() throws IOException {
                    groups.remove(this);
                    super.close();
                }
            };
        }

        @Override
        public void close() {
            this.tracker.close();
        }
    }

}
