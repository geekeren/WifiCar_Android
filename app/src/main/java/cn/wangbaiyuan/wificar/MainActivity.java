package cn.wangbaiyuan.wificar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;


import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    ImageView videoView;
    Bitmap image;
    public String IP = "192.168.8.1";
    public int ctrlPort = 2001;
    private boolean foreGround = true;
    private Handler handlerMsg;
    private HandlerThread cmdThread;
    private HandlerThread videoThread;
    private Runnable videoRunnable;
    private ImageView sendSignal;
    private TextView cmdStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = (ImageView) findViewById(R.id.video);
        final LinearLayout camera_err = (LinearLayout) findViewById(R.id.camera_err);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("基于机器视觉的智能车辆辅助导航系统");
        toolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sendSignal = (ImageView) findViewById(R.id.send_signal);
        cmdStatus = (TextView) findViewById(R.id.cmd_status);
        cmdThread = new HandlerThread("cmdThread");
        videoThread = new HandlerThread("videoThread");
        handlerMsg = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 200:
//                        Log.e("getImage", "getImage " + System.currentTimeMillis() / 1000 + "");
                        camera_err.setVisibility(View.GONE);
                        videoView.setImageBitmap(image);
                        break;
                    case 201:
//                        Log.e("getImage", "getImage " + System.currentTimeMillis() / 1000 + "");
                        signalShine(false);

                        break;
                    case 500:
                        camera_err.setVisibility(View.VISIBLE);
                        break;
                    case 501:
                        signalShine(true);
                        break;
                    case 0:
                        sendSignal.setImageDrawable(getResources().getDrawable(R.drawable.ic_bright_off));
                        break;
                    case 1:
                        sendSignal.setImageDrawable(getResources().getDrawable(R.drawable.ic_bright_on));

                        break;
                    case 2:
                        sendSignal.setImageDrawable(getResources().getDrawable(R.drawable.ic_bright_err));
                        break;
                }

            }


        };
        videoThread.start();
        cmdThread.start();
        videoRunnable = new Runnable() {
            @Override
            public void run() {

                while (foreGround) {

                    try {
                        image = getImageBitmap("http://192.168.8.1:8083/?action=snapshot");
                        if (image != null)
                            handlerMsg.sendEmptyMessage(200);

                    } catch (IOException e) {
                        handlerMsg.sendEmptyMessage(500);
                    } finally {
                        try {
                            sleep(80);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        ImageButton btnForward = (ImageButton) findViewById(R.id.btnForward);
        ImageButton btnBackWard = (ImageButton) findViewById(R.id.btnBackward);
        ImageButton btnTurnLeft = (ImageButton) findViewById(R.id.btnLeft);
        ImageButton btnTurnRight = (ImageButton) findViewById(R.id.btnRight);
        ImageButton btnStop = (ImageButton) findViewById(R.id.btnStop);

        View.OnClickListener btnOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.btnLeft:
                        sendCmd("c");
                        cmdStatus.setText("指令‘c’:左转");
                        break;
                    case R.id.btnRight:
                        sendCmd("d");
                        cmdStatus.setText("指令‘d’:右转");
                        break;
                    case R.id.btnForward:
                        sendCmd("a");
                        cmdStatus.setText("指令‘a’:前进");
                        break;
                    case R.id.btnBackward:
                        sendCmd("b");
                        cmdStatus.setText("指令‘b’:后退");
                        break;
                    case R.id.btnStop:
                        sendCmd("e");
                        cmdStatus.setText("指令‘e’:停止");
                        break;

                }

            }
        };
        btnForward.setOnClickListener(btnOnClick);
        btnBackWard.setOnClickListener(btnOnClick);
        btnTurnLeft.setOnClickListener(btnOnClick);
        btnTurnRight.setOnClickListener(btnOnClick);
        btnStop.setOnClickListener(btnOnClick);

        ImageView wifiSign = (ImageView) findViewById(R.id.wifi_sign);
//        signalShine();
    }

    private void signalShine(boolean hasErr) {
        final int cmd = hasErr ? 2 : 1;
        final Handler handler = new Handler(cmdThread.getLooper());

        Runnable shine = new Runnable() {
            @Override
            public void run() {


                try {

                    handlerMsg.sendEmptyMessage(cmd);
                    sleep(400);
                    handlerMsg.sendEmptyMessage(0);
                    sleep(300);
                    handlerMsg.sendEmptyMessage(cmd);
                    sleep(200);
                    handlerMsg.sendEmptyMessage(0);
                    sleep(250);
                    handlerMsg.sendEmptyMessage(cmd);
                    sleep(200);
                    handlerMsg.sendEmptyMessage(0);
                    sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                handlerMsg.sendEmptyMessage(0);
            }
        };
        handler.post(shine);

    }


    @Override
    protected void onStop() {
        foreGround = false;
        super.onStop();
    }

    @Override
    protected void onStart() {
        final Handler handler = new Handler(videoThread.getLooper());
        foreGround = true;
        handler.post(videoRunnable);
        super.onRestart();
    }

    /**
     * 从指定URL获取图片
     *
     * @param url
     * @return
     */
    private Bitmap getImageBitmap(String url) throws IOException {
        URL imgUrl = null;
        Bitmap bitmap = null;

        imgUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
        conn.setConnectTimeout(1000);
        conn.setReadTimeout(1000);
        conn.setDoInput(true);
        conn.connect();
        InputStream is = conn.getInputStream();
        bitmap = BitmapFactory.decodeStream(is);
        is.close();

        return bitmap;
    }

    public void sendCmd(final String cmd) {

        final Handler handler = new Handler(cmdThread.getLooper());
        Runnable sendCmdRunable = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(IP, ctrlPort);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeChar(cmd.toCharArray()[0]);
                    handlerMsg.sendEmptyMessage(201);
                    socket.close();
                } catch (IOException e) {
                    handlerMsg.sendEmptyMessage(501);
                    e.printStackTrace();
                }
            }
        };
        handler.post(sendCmdRunable);
    }
}
