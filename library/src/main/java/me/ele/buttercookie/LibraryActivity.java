package me.ele.buttercookie;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.library.R;

public class LibraryActivity extends AppCompatActivity {

  @BindView(R.id.btn1)
  public Button button;

  @BindString(R.string.app_name)
  public String str;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_library);
    ButterKnife.bind(this);

    Log.e("test", "button--->" + button);
    Log.e("test", "str--->" + str);
  }

  @OnClick(value = { R.id.textview1, R.id.btn1 }) public void click() {
    Toast.makeText(LibraryActivity.this, "haha", Toast.LENGTH_SHORT).show();
  }
}
