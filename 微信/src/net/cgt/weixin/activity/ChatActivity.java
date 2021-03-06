package net.cgt.weixin.activity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.cgt.weixin.R;
import net.cgt.weixin.domain.ChatEmoji;
import net.cgt.weixin.domain.User;
import net.cgt.weixin.utils.AppToast;
import net.cgt.weixin.utils.DensityUtil;
import net.cgt.weixin.utils.FaceConversionUtil;
import net.cgt.weixin.utils.HandlerTypeUtils;
import net.cgt.weixin.utils.L;
import net.cgt.weixin.utils.LogUtil;
import net.cgt.weixin.view.adapter.EmojiAdapter;
import net.cgt.weixin.view.manager.XmppManager;
import net.cgt.weixin.view.scrollView.MyScrollView;
import net.cgt.weixin.view.scrollView.MyScrollView.IPageChangedListener;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 聊天
 * 
 * @author lijian
 * @date 2014-12-04
 */
public class ChatActivity extends BaseActivity implements OnClickListener, OnItemClickListener {

	private static final String LOGTAG = LogUtil.makeLogTag(ChatActivity.class);

	protected static final int FLAG_RECEIVER = 100;

	/**
	 * 聊天用户
	 */
	private User user;
	/**
	 * 内容显示容器的外置滚动
	 */
	private ScrollView mSv_chat_showBoxScrollView;
	/**
	 * 内容显示容器
	 */
	private LinearLayout mLl_chat_showBox;
	/**
	 * 语音按钮
	 */
	private Button mBtn_chat_speech;
	/**
	 * 键盘按钮
	 */
	private Button mBtn_chat_keyboard;
	/**
	 * 输入框外置容器
	 */
	private LinearLayout mLl_chat_inputBox;
	/**
	 * 笑脸按钮
	 */
	private Button mBtn_chat_smilingface;
	/**
	 * 按住说话按钮
	 */
	private Button mBtn_chat_pressToTalk;
	/**
	 * 加号
	 */
	private Button mBtn_chat_plus;
	/**
	 * 文本输入框
	 */
	private EditText mEt_chat_input;
	/**
	 * 表情外边框
	 */
	private LinearLayout mLl_chat_smilingface_box;
	/**
	 * 表情内容体
	 */
	private LinearLayout mLl_chat_smilingface_body;
	/**
	 * 表情导航tab
	 */
	private RadioGroup mRg_chat_smilingface_tab;
	/**
	 * 发送按钮
	 */
	private Button mBtn_chat_send;
	/**
	 * 自定义ScrollView
	 */
	private MyScrollView mV_myScrollView;
	/**
	 * 表情集合
	 */
	private List<List<ChatEmoji>> mList_emoji;
	/**
	 * 表情页界面集合
	 */
	//	private ArrayList<View> pageViews;
	/**
	 * 表情数据填充器
	 */
	private List<EmojiAdapter> mList_emojiAdapter;
	/**
	 * 当前表情页
	 */
	private int current = 0;
	/**
	 * 表情页的监听事件
	 */
	private OnCorpusSelectedListener mListener;
	/**
	 * 震动传感器
	 */
	private Vibrator vibrator;
	/**
	 * 输入框中的文本
	 */
	private String msg = "";
	/**
	 * 发送按钮show动画集
	 */
	private AnimationSet animationSet;

	/**
	 * 表情选择监听
	 * 
	 * @author naibo-liao
	 * @时间： 2013-1-15下午04:32:54
	 */
	public interface OnCorpusSelectedListener {

		void onCorpusSelected(ChatEmoji emoji);

		void onCorpusDeleted();
	}

	/** 代码注册一个广播接收者(临时的) **/
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String from = intent.getStringExtra("from");
			String body = intent.getStringExtra("body");
			L.i(LOGTAG, "receiver--->from=" + from + "---body=" + body);
			Message message = handler.obtainMessage();
			message.what = FLAG_RECEIVER;
			Bundle bundle = new Bundle();
			bundle.putString("from", from);
			bundle.putString("body", body);
			message.setData(bundle);
			message.sendToTarget();
		}
	};

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HandlerTypeUtils.WX_HANDLER_TYPE_LOAD_DATA_SUCCESS:
				setChatDataMe();
				break;
			case HandlerTypeUtils.WX_HANDLER_TYPE_LOAD_DATA_FAIL:
				AppToast.getToast().show("消息发送失败");
				break;
			case FLAG_RECEIVER:// 收到广播
				setChatDataOtherParty(msg);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	/**
	 * 设置对方聊天数据
	 * 
	 * @param msg
	 */
	private void setChatDataOtherParty(Message msg) {
		Bundle bundle = msg.getData();
		String body = bundle.getString("body");

		View v = View.inflate(this, R.layout.cgt_layout_chat_dialogbox_otherparty, null);
		TextView mTv_chat_dialogBox = (TextView) v.findViewById(R.id.cgt_tv_chat_dialogBox_otherparty);
		ImageView mIv_chat_userImg = (ImageView) v.findViewById(R.id.cgt_iv_chat_userImg_otherparty);

		mIv_chat_userImg.setImageResource(Integer.parseInt(user.getUserPhote()));
		SpannableString spannableString = FaceConversionUtil.getInstace().getExpressionString(this, body);
		mTv_chat_dialogBox.setText(spannableString);
		mLl_chat_showBox.addView(v);

		setChatWindowToDown();
	}

	/**
	 * 设置聊天窗口滚动到最底下
	 */
	private void setChatWindowToDown() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				mSv_chat_showBoxScrollView.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

	/**
	 * 设置我的聊天数据
	 */
	private void setChatDataMe() {
		mEt_chat_input.setText("");

		View v = View.inflate(this, R.layout.cgt_layout_chat_dialogbox_me, null);
		TextView mTv_chat_dialogBox = (TextView) v.findViewById(R.id.cgt_tv_chat_dialogBox_me);
		ImageView mIv_chat_userImg = (ImageView) v.findViewById(R.id.cgt_iv_chat_userImg_me);

		mIv_chat_userImg.setImageResource(R.drawable.user_picture);
		SpannableString spannableString = FaceConversionUtil.getInstace().getExpressionString(this, msg);
		mTv_chat_dialogBox.setText(spannableString);
		mLl_chat_showBox.addView(v);
		setChatWindowToDown();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cgt_activity_chat);
		//隐藏软体键盘
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		Intent intent = this.getIntent();
		user = intent.getParcelableExtra("user");
		init();
	}

	private void init() {
		initView();
		initData();
	}

	private void initView() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(user.getUserAccount());
		setOverflowShowingAlways();

		mSv_chat_showBoxScrollView = (ScrollView) findViewById(R.id.cgt_sv_chat_showBoxScrollView);
		mLl_chat_showBox = (LinearLayout) findViewById(R.id.cgt_ll_chat_showBox);
		mBtn_chat_speech = (Button) findViewById(R.id.cgt_btn_chat_speech);
		mBtn_chat_keyboard = (Button) findViewById(R.id.cgt_btn_chat_keyboard);
		mLl_chat_inputBox = (LinearLayout) findViewById(R.id.cgt_ll_chat_input_box);
		mEt_chat_input = (EditText) findViewById(R.id.cgt_et_chat_input);
		mBtn_chat_smilingface = (Button) findViewById(R.id.cgt_btn_chat_smilingface);
		mBtn_chat_pressToTalk = (Button) findViewById(R.id.cgt_btn_chat_pressToTalk);
		mBtn_chat_plus = (Button) findViewById(R.id.cgt_btn_chat_plus);
		mBtn_chat_send = (Button) findViewById(R.id.cgt_btn_chat_send);

		mLl_chat_smilingface_box = (LinearLayout) findViewById(R.id.cgt_ll_chat_smilingface_box);
		mLl_chat_smilingface_body = (LinearLayout) findViewById(R.id.cgt_ll_chat_smilingface_body);
		mRg_chat_smilingface_tab = (RadioGroup) findViewById(R.id.cgt_rg_chat_smilingface_tab);

		mBtn_chat_speech.setOnClickListener(this);
		mBtn_chat_keyboard.setOnClickListener(this);
		mBtn_chat_smilingface.setOnClickListener(this);
		mBtn_chat_pressToTalk.setOnClickListener(this);
		mBtn_chat_plus.setOnClickListener(this);
		mBtn_chat_send.setOnClickListener(this);
	}

	private void initData() {
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		IntentFilter filter = new IntentFilter("net.cgt.weixin.chat");
		registerReceiver(receiver, filter);// 注册一个广播接收者

		Animation sa = new ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		Animation aa = new AlphaAnimation(0.1f, 1.0f);
		animationSet = new AnimationSet(true);
		animationSet.addAnimation(aa);
		animationSet.addAnimation(sa);
		animationSet.setDuration(100);
		mEt_chat_input.addTextChangedListener(watcher);

		setSmilingfaceData();
	}

	/**
	 * 设置笑脸被点击后的表情数据
	 */
	private void setSmilingfaceData() {
		mV_myScrollView = new MyScrollView(this);
		mList_emoji = FaceConversionUtil.getInstace().emojiLists;

		// 添加表情页
		mList_emojiAdapter = new ArrayList<EmojiAdapter>();
		mV_myScrollView.removeAllViews();
		for (int i = 0; i < mList_emoji.size(); i++) {
			//			GridView的一些特殊属性：
			//
			//			1.android:numColumns=”auto_fit”   //GridView的列数设置为自动
			//			2.android:columnWidth=”90dp "       //每列的宽度，也就是Item的宽度
			//			3.android:stretchMode=”columnWidth"//缩放与列宽大小同步
			//			4.android:verticalSpacing=”10dp”          //两行之间的边距
			//			5.android:horizontalSpacing=”10dp”      //两列之间的边距 
			//			6.android:cacheColorHint="#00000000" //去除拖动时默认的黑色背景
			//			7.android:listSelector="#00000000"        //去除选中时的黄色底色
			//			8.android:scrollbars="none"                   //隐藏GridView的滚动条
			//			9.android:fadeScrollbars="true"             //设置为true就可以实现滚动条的自动隐藏和显示
			//			10.android:fastScrollEnabled="true"      //GridView出现快速滚动的按钮(至少滚动4页才会显示)
			//			11.android:fadingEdge="none"                //GridView衰落(褪去)边缘颜色为空，缺省值是vertical。(可以理解为上下边缘的提示色)
			//			12.android:fadingEdgeLength="10dip"   //定义的衰落(褪去)边缘的长度
			//			13.android:stackFromBottom="true"       //设置为true时，你做好的列表就会显示你列表的最下面
			//			14.android:transcriptMode="alwaysScroll" //当你动态添加数据时，列表将自动往下滚动最新的条目可以自动滚动到可视范围内
			//			15.android:drawSelectorOnTop="false"  //点击某条记录不放，颜色会在记录的后面成为背景色,内容的文字可见(缺省为false)
			//			
			GridView view = new GridView(this);
			EmojiAdapter adapter = new EmojiAdapter(this, mList_emoji.get(i));
			view.setAdapter(adapter);
			mList_emojiAdapter.add(adapter);
			view.setOnItemClickListener(this);
			view.setNumColumns(7);
			view.setBackgroundColor(Color.TRANSPARENT);
			//			view.setHorizontalSpacing(1); //两列之间的边距
			//			view.setVerticalSpacing(10);//两行之间的边距
			view.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);//缩放与列宽大小同步
			view.setCacheColorHint(0);//去除拖动时默认的黑色背景
			//						view.setPadding(5, 5, 5, 5);
			view.setSelector(new ColorDrawable(Color.TRANSPARENT));
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			view.setLayoutParams(params);
			view.setGravity(Gravity.CENTER);
			mV_myScrollView.addView(view);
		}

		mLl_chat_smilingface_body.removeAllViews();
		mLl_chat_smilingface_body.addView(mV_myScrollView);//将MyScrollView添加到内容显示区

		RadioGroup.LayoutParams params_rb = new RadioGroup.LayoutParams(DensityUtil.dip2px(this, 8), DensityUtil.dip2px(this, 8));
		int marginValue = DensityUtil.dip2px(this, 3);
		params_rb.setMargins(marginValue, 0, marginValue, 0);
		for (int i = 0; i < mV_myScrollView.getChildCount(); i++) {
			RadioButton rbtn = new RadioButton(this);
			rbtn.setButtonDrawable(R.drawable.cgt_selector_chat_radiobtn_bg);
			rbtn.setId(i);
			mRg_chat_smilingface_tab.addView(rbtn, params_rb);
			if (i == 0) {
				rbtn.setChecked(true);
			}
		}
		/**
		 * 监听单选按钮是否被选中,
		 */
		mRg_chat_smilingface_tab.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				current = checkedId;
				mV_myScrollView.moveToDest(checkedId);
			}
		});

		/**
		 * 
		 */
		mV_myScrollView.setChangedListener(new IPageChangedListener() {

			@Override
			public void changedTo(int pageId) {
				current = pageId;
				((RadioButton) mRg_chat_smilingface_tab.getChildAt(pageId)).setChecked(true);
			}
		});
	}

	/**
	 * 输入框文本改变监听器
	 */
	TextWatcher watcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			L.d(LOGTAG, "onTextChanged--->s=" + s + "---start=" + start + "---before=" + before + "---count=" + count);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// TODO s-->在输入文本前,文本框中已有的文本内容
			// start-->当前输入的文本的角标
			// count-->
			// after-->输入的文本长度
			L.d(LOGTAG, "beforeTextChanged--->s=" + s + "---start=" + start + "---count=" + count + "---after=" + after);
			if (s.toString().isEmpty()) {
				mBtn_chat_plus.setVisibility(View.GONE);
				mBtn_chat_send.setVisibility(View.VISIBLE);
				mBtn_chat_send.startAnimation(animationSet);
			}
		}

		@Override
		public void afterTextChanged(Editable s) {
			// TODO s-->刚刚输入的文本
			L.d(LOGTAG, "afterTextChanged--->s.toString()=" + s.toString());
			if (s.toString().isEmpty()) {
				mBtn_chat_plus.setVisibility(View.VISIBLE);
				mBtn_chat_send.setVisibility(View.GONE);
				mBtn_chat_plus.startAnimation(animationSet);
			}
		}
	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cgt_btn_chat_speech:// 语音按钮
			mBtn_chat_speech.setVisibility(View.GONE);
			mBtn_chat_keyboard.setVisibility(View.VISIBLE);
			mBtn_chat_pressToTalk.setVisibility(View.VISIBLE);
			mLl_chat_inputBox.setVisibility(View.GONE);
			break;
		case R.id.cgt_btn_chat_keyboard:// 键盘按钮
			mBtn_chat_speech.setVisibility(View.VISIBLE);
			mBtn_chat_keyboard.setVisibility(View.GONE);
			mBtn_chat_pressToTalk.setVisibility(View.GONE);
			mLl_chat_inputBox.setVisibility(View.VISIBLE);
			break;
		case R.id.cgt_btn_chat_smilingface:// 笑脸按钮
			if (mLl_chat_smilingface_box.getVisibility() == View.VISIBLE) {
				mLl_chat_smilingface_box.setVisibility(View.GONE);
			} else {
				mLl_chat_smilingface_box.setVisibility(View.VISIBLE);
			}
			break;
		case R.id.cgt_btn_chat_pressToTalk:// 按住说话

			break;
		case R.id.cgt_btn_chat_plus:// 加号

			break;
		case R.id.cgt_btn_chat_send://发送按钮
			if (checkValidity()) {
				sendMsg();
			}
			break;

		default:
			break;
		}
	}

	/**
	 * 检查合法性
	 * 
	 * @return
	 */
	private boolean checkValidity() {
		msg = mEt_chat_input.getText().toString().trim();
		if (TextUtils.isEmpty(msg)) {
			mEt_chat_input.requestFocus();
			AppToast.getToast().show("发送消息不能为空");
			Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
			mEt_chat_input.startAnimation(shake);
			vibrator.vibrate(300);
			return false;
		}
		return true;
	}

	/**
	 * 发送消息
	 */
	private void sendMsg() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Chat chat = XmppManager.getInstance().getFriendChat(user.getUserAccount(), null);

				//	friend为好友名，不是JID；listener 监听器可以传null，利用聊天窗口对象调用sendMessage发送消息
				//	这里sendMessage我传的是一个JSON字符串，以便更灵活的控制，发送消息完成~
				try {
					// String msgjson = "{\"messageType\":\""+messageType+"\",\"chanId\":\""+chanId+"\",\"chanName\":\""+chanName+"\"}";
					// chat.sendMessage(msgjson);

					chat.sendMessage(msg);
					handler.sendEmptyMessage(HandlerTypeUtils.WX_HANDLER_TYPE_LOAD_DATA_SUCCESS);
				} catch (XMPPException e) {
					handler.sendEmptyMessage(HandlerTypeUtils.WX_HANDLER_TYPE_LOAD_DATA_FAIL);
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.cgt_menu_chat, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home://返回上一菜单页
			AppToast.getToast().show("返回上一页");
			Intent upIntent = NavUtils.getParentActivityIntent(this);
			if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
				TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
			} else {
				upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				NavUtils.navigateUpTo(this, upIntent);
			}
			break;
		case R.id.menu_chat_chatInfo:
			AppToast.getToast().show(R.string.text_menu_chatInfo);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 通过反射得到Android的有无物理Menu键<br>
	 * setOverflowShowingAlways()方法则是屏蔽掉物理Menu键，不然在有物理Menu键的手机上，overflow按钮会显示不出来
	 */
	private void setOverflowShowingAlways() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			menuKeyField.setAccessible(true);
			menuKeyField.setBoolean(config, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ChatEmoji emoji = (ChatEmoji) mList_emojiAdapter.get(current).getItem(position);
		if (emoji.getId() == R.drawable.cgt_selector_chat_emoji_del_bg) {//如果是删除按钮
			int selection = mEt_chat_input.getSelectionStart();//获取光标所在位置
			//			String text = mEt_chat_input.getText().toString();//获取输入框中的文本
			String text = mEt_chat_input.getText().toString().substring(0, selection);//获取光标之前的所有文本
			if (selection > 0) {//如果光标位置大于零,表示有输入文字
				String text2 = text.substring(selection - 1);//获取光标位置的最后一个字符
				if ("]".equals(text2)) {//判断该字符是否为"]"
					int start = text.lastIndexOf("[");//获取最后
					int end = selection;
					mEt_chat_input.getText().delete(start, end);
					return;
				}
				mEt_chat_input.getText().delete(selection - 1, selection);
			}
		}
		if (!TextUtils.isEmpty(emoji.getDescription())) {
			if (mListener != null)
				mListener.onCorpusSelected(emoji);
			SpannableString spannableString = FaceConversionUtil.getInstace().addFace(this, emoji.getId(), emoji.getDescription());
			mEt_chat_input.append(spannableString);
		}
	}

	public void setOnCorpusSelectedListener(OnCorpusSelectedListener listener) {
		mListener = listener;
	}
}
