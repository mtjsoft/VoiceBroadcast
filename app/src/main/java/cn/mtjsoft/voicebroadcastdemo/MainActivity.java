package cn.mtjsoft.voicebroadcastdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import cn.mtjsoft.voice.SoundPoolPlay;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private StringBuilder stringBuilder;
    private EditText editText;
    private TextView textView;
    private CheckBox checkBox;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stringBuilder = new StringBuilder();
        editText = findViewById(R.id.et_amount);
        textView = findViewById(R.id.tv_showMsg);
        checkBox = findViewById(R.id.checkbox);
        // 提前初始化装载音频文件
        SoundPoolPlay.with(getBaseContext());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btu) {
            String amount = editText.getText().toString().trim();
            if (TextUtils.isEmpty(amount)) {
                Toast.makeText(getBaseContext(), "请输入金额", Toast.LENGTH_SHORT).show();
                return;
            }
            stringBuilder.append(count);
            stringBuilder.append("、");
            stringBuilder.append(amount);
            stringBuilder.append("\n");
            textView.setText(stringBuilder.toString());
            SoundPoolPlay.with(getBaseContext()).play(amount, false, checkBox.isChecked());
            count++;
        }
    }

    @Override
    protected void onDestroy() {
        SoundPoolPlay.with(getBaseContext()).stop();
        super.onDestroy();
    }
}