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

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.codelab.friendlychat.databinding.ActivitySignInBinding
import com.google.firebase.ktx.Firebase
import java.util.*
import java.util.Locale.JAPAN


/** 参照(一連の流れ有り)：https://firebase.google.com/docs/auth/android/firebaseui?hl=ja　**/
class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    /** firebaseUIでブイラープレートなしにサインイン機能を実装,サインイン及びログインが可能であり、
     * 今回では、登録済みのメールアドレスであればパスワード要求される(パスワード忘れは？)。ユーザーへの確認メールは,
     * 「https://firebase.google.com/docs/auth/android/manage-users#get_the_currently_signed-in_user」の
     * 「ユーザーに確認メールを送信する」から。**/
    //FirebaseAuthUIActivityResultContract() →　public FirebaseAuthUIAuthenticationResult parseResultの戻り値を利用してonSignInResultを実行する?
    private val signIn: ActivityResultLauncher<Intent> = registerForActivityResult(FirebaseAuthUIActivityResultContract(), this::onSignInResult) /** メソッド参照、使わないと下記のようなラムダ式になる **/
//    private val signIn: ActivityResultLauncher<Intent> = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
//        onSignInResult(result)
//    }

    // Firebase instance variables
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This codelab uses View Binding
        // See: https://developer.android.com/topic/libraries/view-binding
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth
        auth = Firebase.auth

    }

    public override fun onStart() {
        super.onStart()
        //FirebaseUIサインインフロー開始, サインインしているユーザがいない場合、FirebaseUIを起動する(簡単にサインイン画面を実現する)
        if (auth.currentUser == null) {
            val signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()//インインインテントの作成処理を開始
                    .setLogo(R.mipmap.ic_launcher)
                    .setTheme(R.style.AppTheme)
                    .setAvailableProviders(listOf(
                            AuthUI.IdpConfig.EmailBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build()
                    )).build()  //build -> Intent

            /** サインインフローが完了すると、onSignInResult に結果が返されます。launch(I input) →　ActivityResultContractを実行するために必要な入力。 **/
            signIn.launch(signInIntent)
        } else {
            goToMainActivity()
        }
    }

    private fun signIn() {
        // TODO: implement
    }

    //サインイン結果を処理する
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Sign in successful!")
            goToMainActivity()
        } else {
            Toast.makeText(this, "There was an error sugning in", Toast.LENGTH_SHORT).show()

            val response = result.idpResponse
            if (response == null) {
                Log.w(TAG, "Sign in canceled")
            } else {
                Log.w(TAG, "Sign in error", response.error)
            }
        }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "SignInActivity"
    }
}
