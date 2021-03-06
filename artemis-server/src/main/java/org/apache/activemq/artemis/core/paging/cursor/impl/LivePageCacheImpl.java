/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.core.paging.cursor.impl;

import org.apache.activemq.artemis.core.paging.PagedMessage;
import org.apache.activemq.artemis.core.paging.cursor.LivePageCache;
import org.apache.activemq.artemis.core.paging.impl.Page;
import org.apache.activemq.artemis.core.server.LargeServerMessage;
import org.apache.activemq.artemis.utils.collections.ConcurrentAppendOnlyChunkedList;
import org.jboss.logging.Logger;

/**
 * This is the same as PageCache, however this is for the page that's being currently written.
 */
public final class LivePageCacheImpl implements LivePageCache {

   private static final Logger logger = Logger.getLogger(LivePageCacheImpl.class);

   private static final int CHUNK_SIZE = 32;

   private final ConcurrentAppendOnlyChunkedList<PagedMessage> messages;

   private final Page page;

   private volatile boolean isLive = true;

   public LivePageCacheImpl(final Page page) {
      this.page = page;
      this.messages = new ConcurrentAppendOnlyChunkedList<>(CHUNK_SIZE);
   }

   @Override
   public long getPageId() {
      return page.getPageId();
   }

   @Override
   public int getNumberOfMessages() {
      return messages.size();
   }

   @Override
   public void setMessages(PagedMessage[] messages) {
      // This method shouldn't be called on liveCache, but we will provide the implementation for it anyway
      for (PagedMessage message : messages) {
         addLiveMessage(message);
      }
   }

   @Override
   public PagedMessage getMessage(int messageNumber) {
      return messages.get(messageNumber);
   }

   @Override
   public boolean isLive() {
      return isLive;
   }

   @Override
   public void addLiveMessage(PagedMessage message) {
      if (message.getMessage().isLargeMessage()) {
         ((LargeServerMessage) message.getMessage()).incrementDelayDeletionCount();
      }
      messages.add(message);
   }

   @Override
   public void close() {
      logger.tracef("Closing %s", this);
      this.isLive = false;
   }

   @Override
   public PagedMessage[] getMessages() {
      return messages.toArray(PagedMessage[]::new);
   }

   @Override
   public String toString() {
      return "LivePacheCacheImpl::page=" + page.getPageId() + " number of messages=" + getNumberOfMessages() + " isLive = " + isLive;
   }
}
