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

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.*
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.codelab.friendlychat.MainActivity.Companion.ANONYMOUS
import com.google.firebase.codelab.friendlychat.databinding.ImageMessageBinding
import com.google.firebase.codelab.friendlychat.databinding.MessageBinding
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

// The FirebaseRecyclerAdapter class and options come from the FirebaseUI library
// See: https://github.com/firebase/FirebaseUI-Android

/** FirebaseRecyclerAdapter →　アイテムが追加、削除、移動、または変更されるなど、すべてのリアルタイムイベントに応答します
 * public abstract class FirebaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<VH> implements FirebaseAdapter<T> { ~ }
 *  options →　FirebaseRecyclerAdapter を設定するためのオプション.
 * **/
class FriendlyMessageAdapter(
    private val options: FirebaseRecyclerOptions<FriendlyMessage>,
    private val currentUserName: String?
) : FirebaseRecyclerAdapter<FriendlyMessage, ViewHolder>(options) {

    //新しいViewHolderを作成する必要があるたびにこのメソッドを呼び出します。
    // 各項目について、R.layout.message or R.layout.image_message というカスタムレイアウトを使用しています。
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder { //viewType →　新しいviewのviewタイプ
        val inflater = LayoutInflater.from(parent.context)

            // TODO: viewTypeの変化は？
        return if (viewType == VIEW_TYPE_TEXT) {    //通常のメッセージ
            val view = inflater.inflate(R.layout.message, parent, false)
            //レイアウトがすでにインフレートされている場合は、代わりにバインディング クラスの静的 bind() メソッドを呼ぶ
            val binding = MessageBinding.bind(view)
            MessageViewHolder(binding)  //新しいViewHolder
        } else {    //画像
            val view = inflater.inflate(R.layout.image_message, parent, false)
            val binding = ImageMessageBinding.bind(view)
            ImageMessageViewHolder(binding)
        }
    }

//    private fun ViewInflater(layout: Int, parent: ViewGroup, ) {
//        val inflater = LayoutInflater.from(parent.context)
//        val view = inflater.inflate(layout, parent, false)
//        //レイアウトがすでにインフレートされている場合は、代わりにバインディング クラスの静的 bind() メソッドを呼ぶ
//        val binding = MessageBinding.bind(view)
//        MessageViewHolder(binding)  //新しいViewHolder
//    }
    //RecyclerViewは、このメソッドでViewHolderにデータを関連付けます。
    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: FriendlyMessage) {
        if (options.snapshots[position].text != null) {     //textがnullだったら画像と認識するため、
            (holder as MessageViewHolder).bind(options.snapshots[position])
        } else {
            (holder as ImageMessageViewHolder).bind(model)
        }
    }

    /** viewTypeの戻り値を決める。もう少し膨らませて自分のメッセージか相手のメッセージかも入れ込む**/
    override fun getItemViewType(position: Int): Int {
            //textがnullでなく、スナップショットが自身の崇信したメッセージであった場合
//        val myMessage = options.snapshots[position].uid == Firebase.auth.currentUser?.uid   //自分が送信元のメッセージ　true or false
//         if(myMessage){   //textに値があり、かつuidが自身と一致
//             return if (options.snapshots[position].text != null) MY_VIEW_TYPE_TEXT else MY_VIEW_TYPE_IMAGE
//        } else {
//             return if (options.snapshots[position].text != null) OTHER_VIEW_TYPE_TEXT else OTHER_VIEW_TYPE_IMAGE
//        }
        return if (options.snapshots[position].text != null) VIEW_TYPE_TEXT else VIEW_TYPE_IMAGE
    }


    inner class MessageViewHolder(private val binding: MessageBinding) : ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            binding.messengerTextView.text = item.name ?: ANONYMOUS   //名前
            binding.messageTextView.text = item.text    //メッセージ
            setTextColor(item.name, binding.messageTextView)


            if (item.photoUrl != null){
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }

        //自分のメッセージか相手からのメッセージでテキストの色分けをする
        private fun setTextColor(userName: String?, textView: TextView) {
            if (userName != ANONYMOUS && currentUserName == userName && userName != null) {
                textView.setBackgroundResource(R.drawable.rounded_message_blue)
                textView.setTextColor(Color.WHITE)
            } else {
                textView.setBackgroundResource(R.drawable.rounded_message_gray)
                textView.setTextColor(Color.BLACK)
            }
        }
    }

    inner class ImageMessageViewHolder(private val binding: ImageMessageBinding) :
        ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            loadImageIntoView(binding.messageImageView, item.imageUrl!!)

            binding.messengerTextView.text = item.name ?: ANONYMOUS
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl)
            } else {
                binding.messageImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }

    private fun loadImageIntoView(view: ImageView, url: String) {
        if (url.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(url)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Glide.with(view.context)
                        .load(downloadUrl)
                        .into(view)
                }
                .addOnFailureListener { e ->
                    Log.w(
                        TAG,
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            Glide.with(view.context).load(url).into(view)
        }
    }

    companion object {
        const val TAG = "MessageAdapter"
        const val VIEW_TYPE_TEXT = 1
        const val VIEW_TYPE_IMAGE = 2
//        const val MY_VIEW_TYPE_TEXT = 1
//        const val OTHER_VIEW_TYPE_TEXT = 2
//        const val MY_VIEW_TYPE_IMAGE = 3
//        const val OTHER_VIEW_TYPE_IMAGE = 4
    }
}
