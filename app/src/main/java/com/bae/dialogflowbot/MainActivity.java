package com.bae.dialogflowbot;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bae.dialogflowbot.adapters.ChatAdapter;
import com.bae.dialogflowbot.helpers.RequestJavaV2Task;
import com.bae.dialogflowbot.interfaces.BotReply;
import com.bae.dialogflowbot.models.Message;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BotReply {

  RecyclerView chatView;
  ChatAdapter chatAdapter;
  List<Message> messageList = new ArrayList<>();
  EditText editMessage;
  ImageButton btnSend;

  private SessionsClient sessionsClient;
  private SessionName session;
  private String uuid = UUID.randomUUID().toString();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    chatView = findViewById(R.id.chatView);
    editMessage = findViewById(R.id.editMessage);
    btnSend = findViewById(R.id.btnSend);

    messageList.add(new Message("hello", false));
    messageList.add(new Message("hey buddy", true));
    messageList.add(new Message("how are you", false));
    messageList.add(new Message("I am fine. wbu?", true));
    messageList.add(new Message("Good. How is health?", false));
    messageList.add(new Message("Nice yaar.", true));
    messageList.add(new Message("Good to hear from you", false));
    messageList.add(new Message("Yeah! you too.", true));

    chatAdapter = new ChatAdapter(messageList, this);
    chatView.setAdapter(chatAdapter);

    btnSend.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        String message = editMessage.getText().toString();
        if (message != null && !message.isEmpty()) {
          editMessage.setText("");
          sendMessageToBot(message);
          Objects.requireNonNull(chatView.getAdapter()).notifyDataSetChanged();
          Objects.requireNonNull(chatView.getLayoutManager())
              .scrollToPosition(messageList.size() - 1);
        }
      }
    });

    setUpBot();
  }

  private void setUpBot() {
    try {
      InputStream stream = this.getResources().openRawResource(R.raw.credential);
      GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
          .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
      String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

      SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
      SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
          FixedCredentialsProvider.create(credentials)).build();
      sessionsClient = SessionsClient.create(sessionsSettings);

      Toast.makeText(this, "" +projectId, Toast.LENGTH_SHORT).show();
      session = SessionName.of(projectId, uuid);
    } catch (Exception e) {
      e.printStackTrace();
      Log.d("error", "error is : " + e.getMessage());
    }
  }

  private void sendMessageToBot(String message) {
    if (message.trim().isEmpty()) {
      Toast.makeText(MainActivity.this, "Please enter your query!", Toast.LENGTH_LONG).show();
    } else {
      messageList.add(new Message(message, false));
      chatAdapter.notifyDataSetChanged();
      QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
      new RequestJavaV2Task(MainActivity.this, session, sessionsClient, queryInput).execute();
    }
  }

  @Override
  public void callback(DetectIntentResponse response) {
    if (response != null) {
      String botReply = response.getQueryResult().getFulfillmentText();
      if (!botReply.isEmpty()) {
        messageList.add(new Message(botReply, true));
        chatAdapter.notifyDataSetChanged();
      }  else {
        Toast.makeText(this, "something went wrong" , Toast.LENGTH_SHORT).show();
      }
    } else {
      Toast.makeText(this, "failed to connect" , Toast.LENGTH_SHORT).show();
    }
  }
}
