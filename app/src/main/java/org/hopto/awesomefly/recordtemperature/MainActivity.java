package org.hopto.awesomefly.recordtemperature;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    TextView txtTemperature;
    TextView txtHumidity;

    SensorManager sensorManager;
    Sensor temperatureSensor, humiditySensor;

    float ambientTemperature = 0;
    float lastKnownRelativeHumidity = 0;
    float absoluteHumidity = 0;

    String TEMPERATURE_NOT_SUPPORTED = "Temperature sensor not found on this device.";
    String HUMIDITY_NOT_SUPPORTED = "Humidity sensor not found on this device.";

    private static final String TAG = "MyApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String date = new SimpleDateFormat("dd-MMMM-yyyy", Locale.getDefault()).format(new Date());
        TextView txtDate = findViewById(R.id.txtDate);
        txtDate.setText(date);

        txtTemperature = findViewById(R.id.txtViewTemperature);
        txtHumidity = findViewById(R.id.txtViewAbsoluteHumidity);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

        if (temperatureSensor == null)
            txtTemperature.setText(TEMPERATURE_NOT_SUPPORTED);
        if (humiditySensor == null)
            txtHumidity.setText(HUMIDITY_NOT_SUPPORTED);

        final Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "SAVE", Toast.LENGTH_SHORT);
                toast.show();
                insertData(String.valueOf(ambientTemperature), String.valueOf(absoluteHumidity));
            }
        });

        final Button btnDatabase = findViewById(R.id.btnDatabase);
        btnDatabase.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "DATABASE", Toast.LENGTH_SHORT);
                toast.show();
                startActivity(new Intent(getApplicationContext(), DatabaseActivity.class));
            }
        });

        final Button btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(), "EXIT", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            lastKnownRelativeHumidity = event.values[0];
        }

        if(event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            ambientTemperature = event.values[0];

          /*  Toast.makeText(this.getApplicationContext(),"Sensed Temperature change",
                    Toast.LENGTH_SHORT).show(); */
        }
        if(lastKnownRelativeHumidity !=0) {
            absoluteHumidity = calculateAbsoluteHumidity(ambientTemperature, lastKnownRelativeHumidity);
        }

        txtTemperature.setText(String.valueOf(ambientTemperature) + getResources().getString(R.string.celsius));

        txtHumidity.setText(String.valueOf(absoluteHumidity)+ "%");

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String accuracyMsg = "";

        switch(accuracy){
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyMsg="Sensor has high accuracy";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyMsg="Sensor has medium accuracy";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyMsg="Sensor has low accuracy";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyMsg="Sensor has unreliable accuracy";
                break;
            default:
                break;
        }
        Toast accuracyToast = Toast.makeText(this.getApplicationContext(), accuracyMsg, Toast.LENGTH_SHORT);
        accuracyToast.show();

    }

     /* Meaning of the constants
     Dv: Absolute humidity in grams/meter3
     m: Mass constant
     Tn: Temperature constant
     Ta: Temperature constant
     Rh: Actual relative humidity in percent (%) from phone’s sensor
     Tc: Current temperature in degrees C from phone’ sensor
     A: Pressure constant in hP
     K: Temperature constant for converting to kelvin
     */

    public float calculateAbsoluteHumidity(float temperature, float relativeHumidity)
    {
        float Dv = 0;
        float m = 17.62f;
        float Tn = 243.12f;
        float Ta = 216.7f;
        float Rh = relativeHumidity;
        float Tc = temperature;
        float A = 6.112f;
        float K = 273.15f;

        Dv =   (float) (Ta * (Rh/100) * A * Math.exp(m*Tc/(Tn+Tc)) / (K + Tc));

        return Dv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    public void insertData(final String temperature, final String humidity) {
        class SendPostRequestAsyncTask extends AsyncTask<String, Void, String> {
            String ServerURL = "http://awesomefly.hopto.org/site02/api/insert_records_form.php";
            InputStream is = null;
            String result = null;
            String line = null;
            int code;

            @Override
            protected String doInBackground(String... params) {
                String temperatureHolder = temperature;
                String humidityHolder = humidity;

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("temperature", temperatureHolder));
                nameValuePairs.add(new BasicNameValuePair("humidity", humidityHolder));

                try {
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(ServerURL);
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity httpEntity = httpResponse.getEntity();
                    is = httpEntity.getContent();


                } catch (ClientProtocolException e) {
                    Toast.makeText(MainActivity.this, "Client Protocol Exception", Toast.LENGTH_LONG).show();
                }
                  catch (IOException e) {
                      Toast.makeText(MainActivity.this, "IO Exception", Toast.LENGTH_LONG).show();
                  }


                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is, "iso-8859-1"), 8);
                    StringBuilder sb = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    is.close();
                    result = sb.toString();
                    Log.i("TAG", "Result Retrieved");
                } catch (Exception e) {
                    Log.i("TAG", e.toString());
                }

                try {
                    JSONObject json = new JSONObject(result);
                    code = (json.getInt("code"));
                    if (code == 1) {
                        Log.i("msg", "Data Successfully Inserted");
                        //Data Successfully Inserted
                    } else {
                        //Data Not Inserted
                    }
                } catch (Exception e) {
                    Log.i("TAG", e.toString());
                }
                return "Data inserted successfully";
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                Toast.makeText(MainActivity.this, "Data inserted successfully", Toast.LENGTH_LONG).show();
            }
        }
        SendPostRequestAsyncTask sendPostReqAsyncTask = new SendPostRequestAsyncTask();
        sendPostReqAsyncTask.execute(temperature, humidity);
    }
}
