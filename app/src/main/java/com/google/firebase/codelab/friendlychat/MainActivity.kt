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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.Config
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.*
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.codelab.friendlychat.databinding.ActivityMainBinding
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

//yuuutaas124@gmail.com
//test
//testtest
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var manager: LinearLayoutManager

    //結果のコールバックを受け取る。参照：https://developer.android.com/training/basics/intents/result?hl=ja, https://qiita.com/guijiu/items/9b6c3d0b9a13117d8c08
//    private val openDocument = registerForActivityResult(MyOpenDocumentContract()) { uri ->
//        uri?.let { onImageSelected(it) }
//    }
    /** ↑なんとなく下記の書き方に変更 、画像選択インテントの起動**/
    private val openDocument = registerForActivityResult(MyOpenDocumentContract(), this::pickImage)
    private fun pickImage(uri: Uri?){
        uri?.let { onImageSelected(it) }
    }

    // TODO: firbaseインスタンス
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: FriendlyMessageAdapter
    private lateinit var db: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This codelab uses View Binding
        // See: https://developer.android.com/topic/libraries/view-binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /** デバッグモードで実行する場合、Firebase Emulator Suiteに接続します。firebase.json参照 **/
        //note: BuildConfig.DEBUGがfalseのため、trueに変更した
        if(true) {
            //databaseインスタンスをRealtime Databaseエミュレーターと通信するように変更、その他同意
            //host →　10.0.2.2 "は、Android Emulatorがホストコンピュータの "localhost "に接続するための特別なIPアドレスです。
            //port →　ホストコンピューター上の "localhost "に接続するための特別なIPアドレスです。
            Firebase.database.useEmulator("10.0.2.2", 9000)
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.storage.useEmulator("10.0.2.2", 9199)
        }

        // Firebase Authを初期化し、ユーザーがサインインしているかどうかを確認する
        auth = Firebase.auth
        if (auth.currentUser == null) { //サインインしているFirebaseUser か、存在しない場合は null を返します。
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Realtime DatabaseとFirebaseRecyclerAdapterを初期化する。
        db = Firebase.database
        //note: ここを選択されたユーザーによって変えれば相手によってトーク表示を変えられる？
        val messagesRef = db.reference.child(MESSAGES_CHILD)    //realtimedatabaseのmessagesを参照

        /** FirebaseRecyclerAdapter は、RecyclerView に Query をバインドします。
         * データが追加、削除、変更されると、これらの更新は自動的にリアルタイムで UI に適用されます。 **/
        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
            .setQuery(messagesRef, FriendlyMessage::class.java)
            .build()

        adapter = FriendlyMessageAdapter(options, getUserName())
        binding.progressBar.visibility = ProgressBar.INVISIBLE
        manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = manager    //レイアウト マネージャーによって、リスト内の個々の要素が配置される
        binding.messageRecyclerView.adapter = adapter

        //データの変更をリッスンする新しいオブザーバーを登録,引数：登録するオブザーバー
        adapter.registerAdapterDataObserver(MyScrollToBottomObserver(binding.messageRecyclerView, adapter, manager))

        //入力フィールドにテキストがないとき、送信ボタンを無効にする
        binding.messageEditText.addTextChangedListener(MyButtonObserver(binding.sendButton))

        /** メッセージの送信　**/
        // TODO: 元に戻す 
        // 送信ボタンがクリックされたら、テキストメッセージを送信する
//        binding.sendButton.setOnClickListener{
//            val friendlyMessage = FriendlyMessage(
//                binding.messageEditText.text.toString(),
//                getUserName(),
//                getPhotoUrl(),
//                null,
//            )
            binding.sendButton.setOnClickListener{
                val friendlyMessage = FriendlyMessage(
                    binding.messageEditText.text.toString(),
                    getUserName(),
                    getPhotoUrl(),
                    null
//                    auth.currentUser!!.uid.toString()
                )
            //メッセージの追加。push()メソッドは、自動生成された ID をプッシュされたオブジェクトのパスに追加します。
            // これらの ID は連続しているため、新しいメッセージは確実にリストの最後に追加されます。
            db.reference.child(MESSAGES_CHILD).push().setValue(friendlyMessage)
            binding.messageEditText.setText("")

        }

        // 画像ピッカーを起動する
        binding.addMessageImageView.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }
    }

    public override fun onStart() {
        super.onStart()
        // ユーザーのサインインチェック
        if (auth.currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
    }

    public override fun onPause() {
        super.onPause()
        //Firebase Realtime Database からの更新のリッスンを終了
        adapter.stopListening()
    }

    public override fun onResume() {
        super.onResume()
        //Firebase Realtime Database からの更新のリッスンを開始
        adapter.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** StorageReference から Cloud Storage に保存された画像ファイルをダウンロードし、
     * 人気の Glide ライブラリを使用して表示するバインディングを提供します **/
    //一時的な画像の URL を含むメッセージをデータベースに書き込み、画像がアップロードする
    private fun onImageSelected(uri: Uri) {
        Log.d(TAG, "Uri: $uri")
        val user = auth.currentUser
        val tempMessage = FriendlyMessage(null, getUserName(), getPhotoUrl(), LOADING_IMAGE_URL)
        db.reference
            .child(MESSAGES_CHILD)
            .push()
            .setValue(
                tempMessage,
                DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    if (databaseError != null) {
                        Log.w(TAG, "Unable to write message to database", databaseError.toException())
                    return@CompletionListener
                    }

                    //StorageReferenceを構築し、ファイルをアップロードする。
                    val key = databaseReference.key
                    val storageReference = Firebase.storage
                        .getReference(user!!.uid)     //自分？
                        .child(key!!)
                        .child(uri.lastPathSegment!!)

                    putImageInStorage(storageReference, uri, key)
                }
            )
    }

    //選択した画像のアップロードを開始する
    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        //.addOnSuccessListener →　タスクが正常に完了した場合に呼び出されるリスナーを追加
        storageReference.putFile(uri).addOnSuccessListener(this) { taskSnapshot ->
            // クラウドストレージに画像をアップロードする。画像が読み込まれたら、
            // その画像の public downloadUrl を取得し、それをメッセージに追加する。
            Log.i("TEST", "reference/ ${taskSnapshot.metadata!!.reference!!}")
            Log.i("TEST", "downloadUrl/ ${taskSnapshot.metadata!!.reference!!.downloadUrl}")

           taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                val friendlyMessage = FriendlyMessage(null, getUserName(), getPhotoUrl(), uri.toString())
               db.reference
                   .child(MESSAGES_CHILD)
                   .child(key!!)
                   .setValue(friendlyMessage)
           }
        }
            .addOnFailureListener(this) { e ->
                Log.w(TAG, "Image upload task was unsuccessful", e)
            }
    }

    private fun signOut() {
        AuthUI.getInstance().signOut(this)  //ユーザーがサインインしている場合、サインアウトする
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    //ユーザー情報取得
    private fun getPhotoUrl(): String? {
        val user = auth.currentUser //FirebaseUser
        return user?.photoUrl?.toString()
    }

    //ユーザー情報取得
    private fun getUserName(): String? {
        val user = auth.currentUser
        return if (user != null) user.displayName else ANONYMOUS
    }

    companion object {
        private const val TAG = "MainActivity"
        const val MESSAGES_CHILD = "messages"
        const val ANONYMOUS = "anonymous"
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }
}
