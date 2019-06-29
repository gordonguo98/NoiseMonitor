package com.gordon.noisemonitor;

import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.scichart.charting.ClipMode;
import com.scichart.charting.model.dataSeries.XyDataSeries;
import com.scichart.charting.modifiers.AxisDragModifierBase;
import com.scichart.charting.modifiers.ModifierGroup;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.annotations.TextAnnotation;
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.charting.visuals.pointmarkers.EllipsePointMarker;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.core.annotations.Orientation;
import com.scichart.core.framework.UpdateSuspender;
import com.scichart.drawing.utility.ColorUtil;
import com.scichart.extensions.builders.SciChartBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Calendar;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GlobalHandler.HandleMsgListener{

    private GlobalHandler mHandler;

    private Thread mThread = null;

    public static final int PLAY = 1;
    public static final int SUCCESS = 2;
    public static final int FAIL = 3;

    //mqtt连接参数
    final String broker       = "tcp://eiddk72.mqtt.iot.gz.baidubce.com:1883";
    final String clientId     = "test_mqtt_java_" + UUID.randomUUID().toString();
    final String username     = "eiddk72/noiseOfSensor";
    final String password     = "yeaaz3kmu35ynd8n";
    final String topic        = "$baidu/iot/shadow/noiseOfSensor/update/accepted";
    //final String content      = "{\"requestId\": \"requestId_00003\",\"reported\": {\"noise\": \"630\"}}";

    public long time = 0;
    public long lastTime = 0;
    public long noise = 0;

    XyDataSeries lineData;
    XyDataSeries scatterData;
    SciChartSurface chartSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = GlobalHandler.getInstance();
        mHandler.setHandleMsgListener(this);
        chartSurface = (SciChartSurface) findViewById(R.id.chartView);
        addChart(chartSurface);
    }

    public void addChart(final SciChartSurface chartSurface){
        // Initialize the SciChartBuilder
        SciChartBuilder.init(this);
        // Obtain the SciChartBuilder instance
        final SciChartBuilder sciChartBuilder = SciChartBuilder.instance();
        // Create a numeric X axis
        final IAxis xAxis = sciChartBuilder.newNumericAxis()
                .withAxisTitle("TimeLine")
                .withVisibleRange(0, 10000)
                .build();
        // Create a numeric Y axis
        final IAxis yAxis = sciChartBuilder.newNumericAxis()
                .withAxisTitle("Noise value").withVisibleRange(0, 1000).build();
        // Create interactivity modifiers
        ModifierGroup chartModifiers = sciChartBuilder.newModifierGroup()
                .withPinchZoomModifier().withReceiveHandledEvents(true).build()
                .withZoomPanModifier().withReceiveHandledEvents(true).build()
                .build();
        // Add the Y axis to the YAxes collection of the surface
        Collections.addAll(chartSurface.getYAxes(), yAxis);
        // Add the X axis to the XAxes collection of the surface
        Collections.addAll(chartSurface.getXAxes(), xAxis);
        // Add the interactions to the ChartModifiers collection of the surface
        Collections.addAll(chartSurface.getChartModifiers(), chartModifiers);

        // Create a couple of DataSeries for numeric (Int, Double) data
        // Set FIFO capacity to 500 on DataSeries
        final int fifoCapacity = 500;
        lineData = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class)
                .withFifoCapacity(fifoCapacity)
                .build();
        scatterData = sciChartBuilder.newXyDataSeries(Integer.class, Integer.class)
                .withFifoCapacity(fifoCapacity)
                .build();

        // Create and configure a line series
        final IRenderableSeries lineSeries = sciChartBuilder.newLineSeries()
                .withDataSeries(lineData)
                .withStrokeStyle(ColorUtil.LightBlue, 2f, true)
                .build();

        // Create an Ellipse PointMarker for the Scatter Series
        EllipsePointMarker pointMarker = sciChartBuilder
                .newPointMarker(new EllipsePointMarker())
                .withFill(ColorUtil.LightBlue)
                .withStroke(ColorUtil.Green, 2f)
                .withSize(10)
                .build();

        // Create and configure a scatter series
        final IRenderableSeries scatterSeries = sciChartBuilder.newScatterSeries()
                .withDataSeries(scatterData)
                .withPointMarker(pointMarker)
                .build();

        // Add a RenderableSeries onto the SciChartSurface
        chartSurface.getRenderableSeries().add(scatterSeries);
        chartSurface.getRenderableSeries().add(lineSeries);

        // Add a bunch of interaction modifiers to a ModifierGroup
        ModifierGroup additionalModifiers = sciChartBuilder.newModifierGroup()
                .withPinchZoomModifier().build()
                .withZoomPanModifier().withReceiveHandledEvents(true).build()
                .withZoomExtentsModifier().withReceiveHandledEvents(true).build()
                .withXAxisDragModifier().withReceiveHandledEvents(true).withDragMode(AxisDragModifierBase.AxisDragMode.Scale).withClipModex(ClipMode.None).build()
                .withYAxisDragModifier().withReceiveHandledEvents(true).withDragMode(AxisDragModifierBase.AxisDragMode.Pan).build()
                .build();
        // Add the modifiers to the SciChartSurface
        chartSurface.getChartModifiers().add(additionalModifiers);

        // Create a LegendModifier and configure a chart legend
        ModifierGroup legendModifier = sciChartBuilder.newModifierGroup()
                .withLegendModifier()
                .withOrientation(Orientation.HORIZONTAL)
                .withPosition(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 10)
                .build()
                .build();
        // Add the LegendModifier to the SciChartSurface
        chartSurface.getChartModifiers().add(legendModifier);

        // Create and configure a CursorModifier
        ModifierGroup cursorModifier = sciChartBuilder.newModifierGroup()
                .withCursorModifier().withShowTooltip(true).build()
                .build();
        // Add the CursorModifier to the SciChartSurface
        chartSurface.getChartModifiers().add(cursorModifier);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean pressed = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(!pressed && item.getItemId() == R.id.connect) {
            pressed = true;
            item.setIcon(R.drawable.ic_cast_connected_black_pressed_24dp);
            startConnect();
        }
        return super.onOptionsItemSelected(item);
    }

    public void startConnect(){

        if(mThread == null) {
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        MqttConnectOptions connOpts = new MqttConnectOptions();
                        connOpts.setUserName(username);
                        connOpts.setPassword(password.toCharArray());
                        connOpts.setConnectionTimeout(0);

                        //System.out.println("Connecting to broker: " + broker);
                        Log.d("测试MQTT", "Connecting to broker: " + broker);
                        final MqttAndroidClient mqttClient = new MqttAndroidClient(getApplicationContext(), broker, clientId);
                        mqttClient.connect(connOpts, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                                disconnectedBufferOptions.setBufferEnabled(true);
                                disconnectedBufferOptions.setBufferSize(100);
                                disconnectedBufferOptions.setPersistBuffer(false);
                                disconnectedBufferOptions.setDeleteOldestMessages(false);
                                mqttClient.setBufferOpts(disconnectedBufferOptions);
                                Message successMassage = new Message();
                                successMassage.what = SUCCESS;
                                mHandler.sendMessage(successMassage);
                                Log.d("测试MQTT", "Connected. Client id is " + clientId);
                                lastTime = Calendar.getInstance().getTimeInMillis();

                                try {
                                    mqttClient.subscribe(topic, 0, null, new IMqttActionListener() {
                                        @Override
                                        public void onSuccess(IMqttToken asyncActionToken) {
                                            Log.d("测试MQTT", "Subscribed to topic: " + topic);
                                        }

                                        @Override
                                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                            Log.d("测试MQTT", "Fail to subscribe");
                                        }
                                    });

                                    mqttClient.subscribe(topic, 0, new IMqttMessageListener() {
                                        @Override
                                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                                            Log.d("测试MQTT", "MQTT message received: " + message.toString());
                                            Log.d("测试MQTT", String.valueOf(Calendar.getInstance().getTimeInMillis()));
                                            // message Arrived!
                                            long newTime = Calendar.getInstance().getTimeInMillis();
                                            time = (newTime-lastTime);  //time unit: ms
                                            noise = parseJSONWithJSONObject(message.toString());
                                            Log.d("测试MQTT", "时间，噪声：" + time + ", " + noise);
                                            Message playMassage = new Message();
                                            playMassage.what = PLAY;
                                            playMassage.arg1 = (int) noise;
                                            mHandler.sendMessage(playMassage);
                                        }
                                    });
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Message failMassage = new Message();
                                failMassage.what = FAIL;
                                mHandler.sendMessage(failMassage);
                                Log.d("测试MQTT", "Connect fail");
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            mThread.start();
        }
        else {
            notify();
        }
    }

    private long parseJSONWithJSONObject(String jsonData) {
        Log.d("测试MQTT", jsonData);
        MessageBean messageBean=new MessageBean();
        try {
            messageBean = JSON.parseObject(jsonData, MessageBean.class);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        Log.d("测试MQTT", "完成");
        return messageBean.getReported().getNoise();
    }

    @Override
    public void handleMsg(Message msg) {
        switch (msg.what){
            case PLAY:
                setTitle("noise: " + noise);
                lineData.append((int)time/100, (int)noise);
                scatterData.append((int)time/100, (int)noise);
                // Zoom series to fit the viewport
                chartSurface.zoomExtents();
                Toast.makeText(MainActivity.this,getHint(),Toast.LENGTH_LONG).show();
                break;
            case SUCCESS:
                Toast.makeText(MainActivity.this,"Connect successful in MainActivity",Toast.LENGTH_LONG).show();
                break;
            case FAIL:
                Toast.makeText(MainActivity.this,"Connect failed in MainActivity",Toast.LENGTH_LONG).show();
                break;
        }
    }
    public String getHint(){
        if(noise>=0 && noise<200){
            return "好安静，害怕";
        }
        else if(noise>=200 && noise<400){
            return "有点安静，是不是有人在说悄悄话？";
        }
        else if(noise>=400 && noise<600){
            return "这点噪声，小意思啦！";
        }
        else if(noise>=600 && noise<700){
            return "有点吵哦。。。";
        }
        else if(noise>=700 && noise<900){
            return "怎么这么吵啊？";
        }
        else if(noise>=900 && noise<1000){
            return "啊。。。我的耳朵";
        }
        else{
            return "我聋了。你呢？";
        }
    }

}
