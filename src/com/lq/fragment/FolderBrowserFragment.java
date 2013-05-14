package com.lq.fragment;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.lq.activity.MainContentActivity;
import com.lq.activity.R;
import com.lq.adapter.FolderAdapter;
import com.lq.entity.FolderInfo;
import com.lq.loader.FolderInfoRetreiveLoader;
import com.lq.util.CharHelper;
import com.lq.util.GlobalConstant;

public class FolderBrowserFragment extends Fragment implements
		LoaderCallbacks<List<FolderInfo>> {
	private static final String TAG = FolderBrowserFragment.class
			.getSimpleName();
	private final int FOLDER_RETRIEVE_LOADER = 0;

	private ImageView mView_MenuNavigation = null;
	private ImageView mView_MoreFunctions = null;
	private ImageView mView_GoToPlayer = null;
	private TextView mView_Title = null;
	private ListView mView_ListView = null;
	private PopupMenu mOverflowPopupMenu = null;

	private FolderAdapter mAdapter = null;
	private MainContentActivity mActivity = null;
	private String mSortOrder = null;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = (MainContentActivity) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");

		View rootView = inflater
				.inflate(R.layout.list_folder, container, false);
		mView_ListView = (ListView) rootView.findViewById(R.id.listview_folder);
		mView_MenuNavigation = (ImageView) rootView
				.findViewById(R.id.menu_navigation);
		mView_Title = (TextView) rootView.findViewById(R.id.title);
		mView_MoreFunctions = (ImageView) rootView
				.findViewById(R.id.more_functions);
		mView_GoToPlayer = (ImageView) rootView
				.findViewById(R.id.switch_to_player);
		mOverflowPopupMenu = new PopupMenu(getActivity(), mView_MoreFunctions);
		mOverflowPopupMenu.getMenuInflater().inflate(R.menu.popup_folder_list,
				mOverflowPopupMenu.getMenu());

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		initViewsSetting();

		getLoaderManager().initLoader(FOLDER_RETRIEVE_LOADER, null, this);

	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivity = null;
	}

	private void initViewsSetting() {
		mAdapter = new FolderAdapter(getActivity());
		mView_ListView.setAdapter(mAdapter);
		mView_ListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (getParentFragment() instanceof LocalMusicFragment) {
					Bundle data = new Bundle();
					data.putParcelable(FolderInfo.class.getSimpleName(),
							mAdapter.getData().get(position));
					data.putString(GlobalConstant.PARENT,
							FolderBrowserFragment.class.getSimpleName());
					getFragmentManager()
							.beginTransaction()
							.replace(
									R.id.frame_of_local_music,
									Fragment.instantiate(getActivity(),
											TrackBrowserFragment.class
													.getName(), data))
							.addToBackStack(null).commit();
				}
			}
		});
		mView_GoToPlayer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.switchToPlayer();
			}
		});
		mView_Title.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getFragmentManager().popBackStackImmediate();
			}
		});

		mOverflowPopupMenu
				.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.sort_by_folder_name:
							Collections.sort(mAdapter.getData(),
									mFolderNameComparator);
							mAdapter.notifyDataSetChanged();
							break;
						case R.id.sort_by_folder_music_count:
							Collections.sort(mAdapter.getData(),
									mFolderSongCountComparator);
							mAdapter.notifyDataSetChanged();
							break;

						default:
							break;
						}
						return true;
					}
				});

		mView_MenuNavigation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.getSlidingMenu().showMenu();
			}
		});
		mView_MoreFunctions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mOverflowPopupMenu.show();
			}
		});
		
		if(getParentFragment() instanceof LocalMusicFragment){
			setTitleLeftDrawable();
		}
	}

	@Override
	public Loader<List<FolderInfo>> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");

		// 创建并返回一个Loader
		return new FolderInfoRetreiveLoader(getActivity(), mSortOrder);
	}

	@Override
	public void onLoadFinished(Loader<List<FolderInfo>> loader,
			List<FolderInfo> data) {
		Log.i(TAG, "onLoadFinished");

		// TODO SD卡拔出时，没有处理

		// 载入完成，更新列表数据
		Collections.sort(data, mFolderSongCountComparator);
		mAdapter.setData(data);

		// 在标题栏上显示艺术家数目
		if (data != null && data.size() != 0) {
			mView_Title.setText(getResources().getString(
					R.string.classify_by_folder)
					+ "(" + data.size() + ")");
		}

	}

	@Override
	public void onLoaderReset(Loader<List<FolderInfo>> loader) {
		Log.i(TAG, "onLoaderReset");
		mAdapter.setData(null);
	}

	// 按歌曲数量倒序排序
	private Comparator<FolderInfo> mFolderSongCountComparator = new Comparator<FolderInfo>() {
		@Override
		public int compare(FolderInfo lhs, FolderInfo rhs) {
			if (lhs.getNumOfTracks() > rhs.getNumOfTracks()) {
				return -1;
			} else if (lhs.getNumOfTracks() < rhs.getNumOfTracks()) {
				return 1;
			} else {
				return 0;
			}
		}
	};

	private void setTitleLeftDrawable() {
		mView_Title.setClickable(true);
		Drawable title_drawable = getResources().getDrawable(
				R.drawable.btn_titile_back);
		title_drawable.setBounds(0, 0, title_drawable.getIntrinsicWidth(),
				title_drawable.getIntrinsicHeight());
		mView_Title.setCompoundDrawables(title_drawable, null, null, null);
		mView_Title.setBackgroundResource(R.drawable.button_backround_light);
	}

	
	// 按歌曲名称顺序排序
	private Comparator<FolderInfo> mFolderNameComparator = new Comparator<FolderInfo>() {
		char first_l, first_r;

		@Override
		public int compare(FolderInfo lhs, FolderInfo rhs) {
			first_l = lhs.getFolderName().charAt(0);
			first_r = rhs.getFolderName().charAt(0);
			if (CharHelper.checkType(first_l) == CharHelper.CharType.CHINESE) {
				first_l = CharHelper.getPinyinFirstLetter(first_l);
			}
			if (CharHelper.checkType(first_r) == CharHelper.CharType.CHINESE) {
				first_r = CharHelper.getPinyinFirstLetter(first_r);
			}
			if (first_l > first_r) {
				return 1;
			} else if (first_l < first_r) {
				return -1;
			} else {
				return 0;
			}
		}
	};
}
