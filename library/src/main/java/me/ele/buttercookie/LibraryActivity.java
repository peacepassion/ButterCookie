package me.ele.buttercookie;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.library.R;

public class LibraryActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_library);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.btn4) public void onClickTestChinese() {
    Toast.makeText(LibraryActivity.this, "请选择颜色", Toast.LENGTH_SHORT).show();
  }
}
