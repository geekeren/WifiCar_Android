package cn.wangbaiyuan.wificar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.anderson.dashboardview.view.DashboardView;

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
    private TextView carStatus;
    public CarVector carVector = new CarVector(90, true, 0);
    private DashboardView speedDashBoard;

    private ImageButton btnFaster;
    private ImageButton btnDirectMid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = (ImageView) findViewById(R.id.video);
        speedDashBoard = (DashboardView) findViewById(R.id.speedDashBoard);
        speedDashBoard.setPercent(carVector.speed * 100 / carVector.maxSpeed);

        final LinearLayout camera_err = (LinearLayout) findViewById(R.id.camera_err);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("基于机器视觉的智能车辆辅助导航系统");
        toolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sendSignal = (ImageView) findViewById(R.id.send_signal);
        carStatus = (TextView) findViewById(R.id.car_status);
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
                    case 3:
                        speedDashBoard.setPercent(carVector.speed * 100 / carVector.maxSpeed);
                        carStatus.setText(carVector.getString());
                }

            }


        };
        videoThread.start();
        cmdThread.start();
        videoRunnable = new Runnable() {
            @Override
            public void run() {

                while (foreGround) {
//                    if (btnFaster.isPressed()) {
//                        carVector.update(carVector.angle, carVector.isForWard, carVector.speed + 1);
//                    } else if (btnSlower.isPressed()) {
//                        carVector.update(carVector.angle, carVector.isForWard, carVector.speed - 1);
//                    }
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
        btnDirectMid = (ImageButton) findViewById(R.id.btnDirectMid);
        btnFaster = (ImageButton) findViewById(R.id.btn_faster);
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float minMove = 0;        //最小滑动距离
                float minVelocity = 0;     //最小滑动速度

                float beginY = e1.getY();
                float endY = e2.getY();

                if (beginY - endY > minMove && Math.abs(velocityY) > Math.abs(velocityX)) {  //上滑
                    carVector.update(carVector.angle, carVector.isForWard, carVector.speed + 1);
                } else if (endY - beginY > minMove &&  Math.abs(velocityY) > Math.abs(velocityX)) {  //下滑
                    carVector.update(carVector.angle, carVector.isForWard, carVector.speed - 1);
                }

                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

                return false;
            }
        });

//        View.OnTouchListener speedListner = new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                final boolean isSlowerBtn = (view.getId() == R.id.btn_slower);
////                switch (motionEvent.getAction()){
////                    case MotionEvent.ACTION_DOWN:
////                        break;
////                    case MotionEvent.ACTION_MOVE:
////                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
////                            handlerMsg.post(new Runnable() {
////                                @Override
////                                public void run() {
//                if (isSlowerBtn) {
//                    carVector.update(carVector.angle, carVector.isForWard, carVector.speed - 1);
//                } else {
//                    carVector.update(carVector.angle, carVector.isForWard, carVector.speed + 1);
//                }
////                                }
////                            });
////                        }
////                        break;
////                    case MotionEvent.ACTION_UP:
////                        break;
////                }
//
//                return true;
//            }
//        };
        btnFaster.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });
        // btnFaster.setOnTouchListener(speedListner);


        View.OnClickListener btnOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.btnLeft:
                        carVector.update(carVector.angle + 5, carVector.isForWard, carVector.speed);
                        break;
                    case R.id.btnRight:
                        carVector.update(carVector.angle - 5, carVector.isForWard, carVector.speed);
                        break;
                    case R.id.btnForward:
                        carVector.update(carVector.angle, true, carVector.speed == 0 ? 40 : carVector.speed);
                        break;
                    case R.id.btnBackward:

                        carVector.update(carVector.angle, false, carVector.speed == 0 ? 30 : carVector.speed);
                        break;
                    case R.id.btnDirectMid:
                        carVector.update(90, carVector.isForWard, carVector.speed);

                        break;
                    case R.id.btnStop:
//                        sendCmd("e");
                        carVector.update(carVector.angle, true, 0);
                        break;

                }

            }
        };
        btnForward.setOnClickListener(btnOnClick);
        btnBackWard.setOnClickListener(btnOnClick);
        btnTurnLeft.setOnClickListener(btnOnClick);
        btnTurnRight.setOnClickListener(btnOnClick);
        btnStop.setOnClickListener(btnOnClick);
        btnDirectMid.setOnClickListener(btnOnClick);
        ImageView wifiSign = (ImageView) findViewById(R.id.wifi_sign);
//        signalShine();
        carVector.setCarVectorChangeListener(new CarVector.CarVectorChangeListener() {
            @Override
            public void onChange(CarVector newVector) {
                handlerMsg.sendEmptyMessage(3);
            }
        });
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
