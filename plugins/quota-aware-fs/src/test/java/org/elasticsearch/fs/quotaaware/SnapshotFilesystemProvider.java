/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.fs.quotaaware;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.spi.FileSystemProvider;

/**
 * A {@link FileSystemProvider} implementation that delegates to another provider, but
 * takes a snapshot of the total, usable and unallocated space values upon creation.
 * This prevents tests failing due to activity on the underlying filesystem.
 */
class SnapshotFilesystemProvider extends DelegatingProvider {
    private final long totalSpace;
    private final long usableSpace;
    private final long unallocatedSpace;

    SnapshotFilesystemProvider(FileSystemProvider provider) throws Exception {
        super(provider);

        final FileStore fileStore = provider.getFileStore(provider.getPath(new URI("file:///")));
        this.totalSpace = fileStore.getTotalSpace();
        this.usableSpace = fileStore.getUsableSpace();
        this.unallocatedSpace = fileStore.getUnallocatedSpace();
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return new SnapshotFileStore(super.getFileStore(path));
    }

    private class SnapshotFileStore extends FileStore {
        private final FileStore delegate;

        SnapshotFileStore(FileStore fileStore) {
            this.delegate = fileStore;
        }

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public String type() {
            return delegate.type();
        }

        @Override
        public boolean isReadOnly() {
            return delegate.isReadOnly();
        }

        @Override
        public long getTotalSpace() {
            return totalSpace;
        }

        @Override
        public long getUsableSpace() {
            return usableSpace;
        }

        @Override
        public long getUnallocatedSpace() {
            return unallocatedSpace;
        }

        @Override
        public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
            return delegate.supportsFileAttributeView(type);
        }

        @Override
        public boolean supportsFileAttributeView(String name) {
            return delegate.supportsFileAttributeView(name);
        }

        @Override
        public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
            return delegate.getFileStoreAttributeView(type);
        }

        @Override
        public Object getAttribute(String attribute) throws IOException {
            return delegate.getAttribute(attribute);
        }
    }
}
