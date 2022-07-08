package com.kakaologin_sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.Account;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG="사용자";

    private ImageView btn_login;
    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btn_login = findViewById(R.id.btn_login);

        btn_login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                UserApiClient.getInstance().loginWithKakaoTalk(v.getContext(),(oAuthToken, error) -> {
                    if (error != null) {
                        Toast.makeText(v.getContext(), "로그인 실패" + error.toString(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "로그인 실패", error);
                    } else if (oAuthToken != null) {
                        Log.i(TAG, "로그인 성공(토큰) : " + oAuthToken.getAccessToken());

                        UserApiClient.getInstance().me((user, meError) -> {
                            if (meError != null) {
                                Log.e(TAG, "사용자 정보 요청 실패", meError);
                            } else {
                                System.out.println("로그인 완료");
                                account = user.getKakaoAccount();
                                Intent intent = new Intent(v.getContext(), MapFragmentActivity.class);
                                startActivity(intent);
                            }
                            return null;
                        });
                    }

                    return null;
                });
            }
        });
    }
}