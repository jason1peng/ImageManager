/*******************************************************************************
 * Copyright 2013 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.	
 *******************************************************************************/
package idv.jason.lib.imagemanager.tasks;

import java.util.concurrent.ThreadFactory;

public class ImageManagerThreadFactory implements ThreadFactory {

	private final String mThreadName;

	public ImageManagerThreadFactory(String threadName) {
		mThreadName = threadName;
	}

	public ImageManagerThreadFactory() {
		this(null);
	}

	public Thread newThread(final Runnable r) {
		if (null != mThreadName) {
			return new Thread(r, mThreadName);
		} else {
			return new Thread(r);
		}
	}
}