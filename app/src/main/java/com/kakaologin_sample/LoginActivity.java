package com.kakaologin_sample;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.Account;
import org.json.JSONException;
import org.json.JSONObject;
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "사용자";
    private ImageView btn_login;
    private Account account;
    private static final String HOST = "143.248.199.127";
    private static final String PORT = "80";
    private static String kakao_id ;
    private static String nickname;
    private static String profileImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        btn_login = findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(v.getContext())) {
                    UserApiClient.getInstance().loginWithKakaoTalk(v.getContext(), (oAuthToken, error) -> {
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error);
                        } else if (oAuthToken != null) {
                            Log.i(TAG, "로그인 성공(토큰) : " + oAuthToken.getAccessToken());
                            UserApiClient.getInstance().me((user, meError) -> {
                                if (meError != null) {
                                    Log.e(TAG, "사용자 정보 요청 실패", meError);
                                } else {
                                    System.out.println("로그인 완료");
                                    account = user.getKakaoAccount();
                                    Log.d("asdf", account.toString());
                                    kakao_id = String.valueOf(user.getId());
                                    nickname = account.getProfile().getNickname();
                                    profileImage=account.getProfile().getThumbnailImageUrl();
                                    RequestQueue requestQueue = Volley.newRequestQueue(LoginActivity.this);
                                    String uri1 = String.format("http://"+HOST+"/create_user?kakao_id="+kakao_id+"&nickname="+nickname+"&user_img="+profileImage);
                                    StringRequest stringRequest = new StringRequest(Request.Method.POST, uri1, new Response.Listener() {
                                        @Override
                                        public void onResponse(Object response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(response.toString());
                                                Log.d("response",jsonObject.toString());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                        }
                                    });
                                    requestQueue.add(stringRequest);
                                    Intent intent = new Intent(v.getContext(), MapFragmentActivity.class);
                                    intent.putExtra("profile_image",profileImage);
                                    intent.putExtra("nickname", nickname);
                                    intent.putExtra("kakao_id", user.getId());
                                    startActivity(intent);
                                }
                                return null;
                            });
                        }
                        return null;
                    });
                }
                else {
                    UserApiClient.getInstance().loginWithKakaoAccount(v.getContext(), (oAuthToken, error) -> {
                        if (error != null) {
                            Log.e(TAG, "로그인 실패", error);
                            Log.e(TAG, "여기 오류 ", error);
                        } else if (oAuthToken != null) {
                            UserApiClient.getInstance().me((user, meError) -> {
                                if (meError != null) {
                                    Log.e(TAG, "사용자 정보 요청 실패", meError);
                                } else {
                                    System.out.println("로그인 완료");
                                    account = user.getKakaoAccount();
                                    Log.d("asdf", account.toString());
                                    kakao_id = String.valueOf(user.getId());
                                    nickname = account.getProfile().getNickname();
                                    profileImage=account.getProfile().getThumbnailImageUrl();
                                    RequestQueue requestQueue = Volley.newRequestQueue(LoginActivity.this);
                                    String uri1 = String.format("http://"+HOST+"/create_user?id="+kakao_id+"&nickname="+nickname+"&user_img"+profileImage);
                                    StringRequest stringRequest = new StringRequest(Request.Method.GET, uri1, new Response.Listener() {
                                        @Override
                                        public void onResponse(Object response) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(response.toString());
                                                Log.d("response",jsonObject.toString());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.d("로딩 reservation에러", "에러: " + error.toString());
                                            //추가
                                        }
                                    });
                                    requestQueue.add(stringRequest);
                                    Intent intent = new Intent(v.getContext(), MapFragmentActivity.class);
                                    intent.putExtra("profile_image", profileImage);
                                    intent.putExtra("nickname", nickname);
                                    intent.putExtra("kakao_id", kakao_id);
                                    startActivity(intent);
                                }
                                return null;
                            });
                        }
                        return null;
                    });
                }
            }
        });
    }
}