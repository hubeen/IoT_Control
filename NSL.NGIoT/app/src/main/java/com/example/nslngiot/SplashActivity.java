package com.example.nslngiot;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.nslngiot.Network_Utill.NetworkCheck;
import com.example.nslngiot.Network_Utill.VolleyQueueSingleTon;
import com.example.nslngiot.Security_Utill.AES;
import com.example.nslngiot.Security_Utill.KEYSTORE;
import com.example.nslngiot.Security_Utill.RSA;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;



/**     암호 로직    **/
// 모든 키는 최초 1회만 생성되어 계속 사용된다.
// Keystore 초기화 및 AES대칭키 생성 및 컨테이너에 저장
// 클라이언트의 공개키/비공개키 생성 Keystore에서 대칭키로 비공개키 암호화
// 데이터 전송 시 Keystore의 대칭키로 가져와 데이터 암호화 / 대칭키는 전달받은 서버의 공개키로 암호화
// 암호화된 대칭키/데이터, 클라이언트 공개키 전송
// 서버의 개인키로 암호화된 대칭키 복호화 -> 복호화된 대칭키로 암호문 복호화
// 서버는 전송할 데이터를 대칭키로 암호화, 대칭키는 클라이언트의 공개키로 암호화 후 전송
// Keystore의 대칭키로 클라이언트 비공개키 복호화, 전송받은 대칭키는 비공개키로 복호화, 복호화된 대칭키로 암호화 데이터 복호화

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 네트워크 연결 되어 있으면 진행
        if(NetworkCheck.networkCheck(SplashActivity.this)){
            try {

                RSA.rsaKeyGen(); // 최초 한번 클라이언트의 RSA 비대칭키 생성
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    KEYSTORE.keyStore_init(); // 최초 1회 KeyStore에 저장할 AES 대칭키 생성
                rsaKeyRequest(); // 서버로부터 RSA공개키 요청

            } catch (NoSuchAlgorithmException e) {
                System.err.println("Splash Activty NoSuchAlgorithmException error");
            }
        }else{
            Toast.makeText(this, "네트워크연결이 되지 않습니다.\n" + "네트워크 수신상태를 확인하세요.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void rsaKeyRequest(){ // RSA암호화에 사용할 공개키를 서버에게 요청
        StringBuffer url = new StringBuffer("http://210.125.212.191:8888/IoT/key받기.jsp");

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, String.valueOf(url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String[] resPonse_split = response.split(" ");
                        if("키받음".equals(resPonse_split[1])) {

                            RSA.serverPublicKey=resPonse_split[0]; // 서버의 공개키 저장
                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                            finish();

                        }else if("키 못받음".equals(resPonse_split[1])) {
                            Toast.makeText(getApplicationContext(), "암호화 셋팅 실패. 다시 실행해주세요.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                // 회원가입 정보 push 진행
                params.put("type","키주세요");
                return params;
            }
        };

        // 캐시 데이터 가져오지 않음 왜냐면 기존 데이터 가져올 수 있기때문
        // 항상 새로운 데이터를 위해 false
        stringRequest.setShouldCache(false);
        VolleyQueueSingleTon.getInstance(this.getApplicationContext()).addToRequestQueue(stringRequest);
    }
}