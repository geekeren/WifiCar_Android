package cn.wangbaiyuan.wificar;

/**
 * Created by BrainWang on 2017-04-27.
 */

public class CarVector {
    public int angle = 90;
    public int speed = 0;
    public boolean isForWard = true;

    public int maxAngle = 150;
    public int minAngle = 30;
    public int maxSpeed =150;

    public void setCarVectorChangeListener(CarVectorChangeListener carVectorChangeListener) {
        this.carVectorChangeListener = carVectorChangeListener;
    }

    CarVectorChangeListener carVectorChangeListener = null;

    public CarVector(int angle, boolean isForWard, int speed) {
        this.angle = angle;
        this.speed = speed;
        this.isForWard = isForWard;
    }

    public void update(int angle, boolean isForWard, int speed) {
        if (angle < minAngle)
            angle = minAngle;
        else if (angle > maxAngle)
            angle = maxAngle;
        this.angle = angle;
        if (speed < 0)
            speed = 0;
        else if (speed > maxSpeed)
            speed = maxSpeed;
        this.speed = speed;
        this.isForWard = isForWard;
        if (carVectorChangeListener != null) {
            carVectorChangeListener.onChange(this);
        }
    }

    public String getString() {
        //String angleStr = (angle==90)? "正"
        return String.format("方向：%s\n偏角：%s\n速度：%s", isForWard ? "前" : "后", angle, speed);
    }

    public interface CarVectorChangeListener {
        public abstract void onChange(CarVector newVector);
    }

}
