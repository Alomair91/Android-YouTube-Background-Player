/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smedic.tubtub.interfaces;

import com.smedic.tubtub.YouTubePlaylist;

import java.util.ArrayList;

/**
 * Interface which enables passing playlists to the fragments
 * Created by Stevan Medic on 15.3.16..
 */
public interface YouTubePlaylistsReceiver {
    void onPlaylistsReceived(ArrayList<YouTubePlaylist> youTubePlaylists);
}
