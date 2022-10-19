/**
 * Copyright Google Inc. All Rights Reserved.
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
 */
package com.google.firebase.codelab.friendlychat

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver

/** RecyclerView.Adapterへの変更を監視するためのObserver **/
class MyScrollToBottomObserver(
    private val recycler: RecyclerView,
    private val adapter: FriendlyMessageAdapter,
    private val manager: LinearLayoutManager
) : AdapterDataObserver() {
    //項目が挿入された直後にこのonItemRangeInserted()が呼ばれる.positionStartは挿入開始位置なので、明示的にこの位置にリストをスクロールするように指定すればよい。
    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        val count = adapter.itemCount
        val lastVisiblePosition = manager.findLastCompletelyVisibleItemPosition()
        // リサイクルビューが最初にロードされたときや、ユーザーがリストの最下部にいるときは
        //// リストの一番下にいる場合、リストの一番下までスクロールして、新しく追加されたメッセージを表示します。
        //// 新たに追加されたメッセージを表示するために、リストの一番下までスクロールします。
        val loading = lastVisiblePosition == -1
        val atBottom = positionStart >= count - 1 && lastVisiblePosition == positionStart - 1
        if (loading || atBottom) {
            recycler.scrollToPosition(positionStart)
        }
    }
}
