// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.apps.facemeshgpu;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.Manifest;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Calendar;
import android.view.View;
import android.widget.EditText;
import android.view.LayoutInflater;
import android.os.AsyncTask;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;



import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;

/** Main activity of MediaPipe face mesh app. */
public class MainActivity extends com.google.mediapipe.apps.basic.MainActivity {
  private static final String TAG = "MainActivity";

  private static final String INPUT_NUM_FACES_SIDE_PACKET_NAME = "num_faces";
  private static final String OUTPUT_LANDMARKS_STREAM_NAME = "multi_face_landmarks";
  // Max number of faces to detect/process.
  private static final int NUM_FACES = 1;

  private float ry1, ry2, ay1, ay2, ratio;
  private TextView tv,message;
  private int counterOpen;
  private int counterClose;
  private boolean eye_blinked, eye_open;
  private int randomNumber,errors, counter;
  private ImageView aimIv;
  private Button button;
  private List<AttentionTest> attentionTestList;
  private List<FatigueTest> fatigueTestList;
  private AttentionTest attTest;
  private FatigueTest fatigueTest;
  private Long timeWhenClosed;
  private Long timeWhenOpen;
  private Long timeCalculated;
  private boolean validator;
  

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    AndroidPacketCreator packetCreator = processor.getPacketCreator();
    Map<String, Packet> inputSidePackets = new HashMap<>();
    inputSidePackets.put(INPUT_NUM_FACES_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_FACES));
    processor.setInputSidePackets(inputSidePackets);

    // tv = findViewById(R.id.tv);
    //imgv = findViewById(R.id.imageView);
    message = findViewById(R.id.message);
    button = findViewById(R.id.floatingActionButton);
    aimIv = findViewById(R.id.aimIv);
    Random rand = new Random();
    eye_open = true;
    eye_blinked = true;
    counterOpen = 0;
    counterClose = 0;
    attentionTestList = new ArrayList<>();
    fatigueTestList = new ArrayList<>();
    validator = true;


    new CountDownTimer(6000000, 750) {

            public void onTick(long millisUntilFinished) {
                if(aimIv.getVisibility() == View.VISIBLE){
                    aimIv.setVisibility(View.INVISIBLE);
                    randomNumber = rand.nextInt(9);
                    message.setText(String.valueOf(randomNumber));
                    message.setVisibility(View.VISIBLE);
                    counter = counter + 1;
                    attTest = new AttentionTest(String.valueOf(Calendar.getInstance().getTimeInMillis())
                            ,"000000000",randomNumber,0);
                }else{
                    aimIv.setVisibility(View.VISIBLE);
                    message.setVisibility(View.INVISIBLE);
                    attentionTestList.add(attTest);
                }

                if(counter == 225){
                    onFinish();
                }
            }

            public void onFinish() {
                   this.cancel();
                   validator = false;
                   callInputDialog();
            }
        }.start();

        

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attTest.setButtonPressed(1);
                attTest.setButtonPressedTime(String.valueOf(Calendar.getInstance().getTimeInMillis()));
                if (randomNumber == 3){
                    errors=errors+1;
                }
            }
        });

    // To show verbose logging, run:
    // adb shell setprop log.tag.MainActivity VERBOSE

    // if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.d("ApplicationConstant.TAG","Eyes" + "control1");
      processor.addPacketCallback(
        OUTPUT_LANDMARKS_STREAM_NAME,
        (packet) -> {
          Log.v(TAG, "Received multi face landmarks packet.");
          Log.d("ApplicationConstant.TAG","Eyes" + "control2");
          List<NormalizedLandmarkList> multiFaceLandmarks =
              PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser());

          ry1 = multiFaceLandmarks.get(0).getLandmarkList().get(5).getY()*2340f;
          ry2 = multiFaceLandmarks.get(0).getLandmarkList().get(4).getY()*2340f;

          ay1 = multiFaceLandmarks.get(0).getLandmarkList().get(373).getY()*2340f;
          ay2 = multiFaceLandmarks.get(0).getLandmarkList().get(386).getY()*2340f;

          ratio = (ay1 - ay2) / (ry2 - ry1);
          Log.d("ApplicationConstant.TAG","Eyes" + ratio);
        //if(validator){
          if(ratio < 0.8){
            if(eye_blinked){
              eye_blinked = false;
              eye_open = true;
              timeWhenClosed = System.currentTimeMillis();
              counterClose = counterClose + 1;
              Log.d("ApplicationConstant.TAG","Eyes Close");
              fatigueTest = new FatigueTest(String.valueOf(Calendar.getInstance().getTimeInMillis()),3450L);
            }
          }
          else{
            if(eye_open)
            {
              timeWhenOpen = System.currentTimeMillis();
              timeCalculated = timeWhenOpen - timeWhenClosed;
              fatigueTest.setBlinkDuration(timeCalculated);
              fatigueTestList.add(fatigueTest);
              eye_blinked = true;
              eye_open = false;
              Log.d("ApplicationConstant.TAG","Eyes Open");
              counterOpen = counterOpen + 1;
            }
          }
        //}
          
        //   Log.v(
        //       TAG,
        //       "[TS:"
        //           + packet.getTimestamp()
        //           + "] "
        //           + getMultiFaceLandmarksDebugString(multiFaceLandmarks));
        });
    // }
  }
  private void callInputDialog(){
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                String dataObject = createJsonObject(attentionTestList
                                ,fatigueTestList);
                                String my_url = "http://20.79.221.9/results/"+userInput.getText().toString();// Replace this with your own url
                                String my_data = dataObject;// Replace this with your data
                                System.out.println(my_data);
                                new MyHttpRequestTask().execute(my_url,my_data);
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


  private void closeActivity(){
        this.finish();
    }

  private String createJsonObject(List<AttentionTest> atteTestList
            , List<FatigueTest> fatigue_TestList){
        String jsonFatigue = "\"fatigue_test\":[\n";
        String jsonAttention = "\"attention_test\":[\n";
        StringBuilder arrayFatigue = new StringBuilder();
        StringBuilder arrayAttention = new StringBuilder();
        StringBuilder data = new StringBuilder();
        arrayFatigue.append(jsonFatigue);
        arrayAttention.append(jsonAttention);
        for (FatigueTest value : fatigue_TestList){
            arrayFatigue.append("{\n").append("\"blink_time\":")
                    .append('\"'+value.blinkTime+'\"').append(",\n")
                    .append("\"blink_duration\":").append(value.blinkDuration+"\n")
                    .append("}").append(",\n").toString();
        }
        for (AttentionTest value : atteTestList){
            arrayAttention.append("{\n").append("\"number_shown_time\":")
                    .append('\"'+value.numberShowTime+'\"').append(",\n")
                    .append("\"button_pressed_time\":")
                    .append('\"'+value.buttonPressedTime+'\"').append(",\n")
                    .append("\"number_shown\":")
                    .append(value.numberShown).append(",\n")
                    .append("\"button_pressed\":")
                    .append(value.buttonPressed+"\n")
                    .append("}\n").append(",\n").toString();
        }
        arrayFatigue.deleteCharAt(arrayFatigue.length()-2);
        arrayAttention.deleteCharAt(arrayAttention.length()-2);
        data.append("{\n\"test_session\": {\n").append(arrayFatigue).append("],\n")
                .append(arrayAttention).append("]\n" +
                "}\n}\n").toString();
        Log.d("ApplicationConstant.TAG",data.toString());
        return data.toString();
    }

  private static String getMultiFaceLandmarksDebugString(
      List<NormalizedLandmarkList> multiFaceLandmarks) {
    if (multiFaceLandmarks.isEmpty()) {
      return "No face landmarks";
    }
    String multiFaceLandmarksStr = "Number of faces detected: " + multiFaceLandmarks.size() + "\n";
    int faceIndex = 0;
    for (NormalizedLandmarkList landmarks : multiFaceLandmarks) {
      multiFaceLandmarksStr +=
          "\t#Face landmarks for face[" + faceIndex + "]: " + landmarks.getLandmarkCount() + "\n";
      int landmarkIndex = 0;
      for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
        multiFaceLandmarksStr +=
            "\t\tLandmark ["
                + landmarkIndex
                + "]: ("
                + landmark.getX()
                + ", "
                + landmark.getY()
                + ", "
                + landmark.getZ()
                + ")\n";
        ++landmarkIndex;
      }
      ++faceIndex;
    }
    return multiFaceLandmarksStr;
  }

  class FatigueTest{
        String blinkTime;
        Long blinkDuration;

        public FatigueTest(String blinkTime, Long blinkDuration) {
            this.blinkTime = blinkTime;
            this.blinkDuration = blinkDuration;
        }

        public void setBlinkDuration(Long blinkDuration) {
            this.blinkDuration = blinkDuration;
        }
    }

    class AttentionTest {
        String numberShowTime;
        String buttonPressedTime;
        int numberShown;
        int buttonPressed;

        public void setButtonPressedTime(String buttonPressedTime) {
            this.buttonPressedTime = buttonPressedTime;
        }

        public AttentionTest(String numberShowTime, String buttonPressedTime, int numberShown, int buttonPressed) {
            this.numberShowTime = numberShowTime;
            this.buttonPressedTime = buttonPressedTime;
            this.numberShown = numberShown;
            this.buttonPressed = buttonPressed;
        }

        public void setButtonPressed(int buttonPressed) {
            this.buttonPressed = buttonPressed;
        }
    }

  class MyHttpRequestTask extends AsyncTask<String,Integer,String> {

        @Override
        protected String doInBackground(String... params) {
            String my_url = params[0];
            String my_data = params[1];
            try {
                URL url = new URL(my_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                // setting the  Request Method Type
                httpURLConnection.setRequestMethod("POST");
                // adding the headers for request
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Authorization","Basic aGFtdDpBc3NpZ25tZW50NElzSGFyZCE=");
                try{
                    //to tell the connection object that we will be wrting some data on the server and then will fetch the output result
                    httpURLConnection.setDoOutput(true);
                    // this is used for just in case we don't know about the data size associated with our request
                    httpURLConnection.setChunkedStreamingMode(0);

                    // to write tha data in our request
                    OutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                    outputStreamWriter.write(my_data);
                    outputStreamWriter.flush();
                    outputStreamWriter.close();

               // to log the response code of your request
               Log.d("ApplicationConstant.TAG", "MyHttpRequestTask doInBackground : " +httpURLConnection.getResponseCode());
               // to log the response message from your server after you have tried the request.
               Log.d("ApplicationConstant.TAG", "MyHttpRequestTask doInBackground : " +httpURLConnection.getResponseMessage());


                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    // this is done so that there are no open connections left when this task is going to complete
                    httpURLConnection.disconnect();
                    closeActivity();

                }


            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }
}
