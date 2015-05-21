package com.guerinet.materialtabs;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 * <p>
 * To use the component, simply add it to your view hierarchy. Then in your
 * {@link android.app.Activity} or {@link android.support.v4.app.Fragment} call
 * {@link #setViewPager(ViewPager)} providing it the ViewPager this layout is being used for.
 * <p>
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via {@link #setSelectedIndicatorColors(int...)}. The
 * alternative is via the {@link TabColorizer} interface which provides you complete control over
 * which color is used for any individual position.
 * <p>
 * The views used as tabs can be customized by calling {@link #setCustomTabView(int, int)},
 * providing the layout ID of your custom layout.
 */
public class TabLayout extends HorizontalScrollView {
	/**
	 * Allows complete control over the colors drawn in the tab layout. Set with
	 * {@link #setCustomTabColorizer(TabColorizer)}.
	 */
	public interface TabColorizer {

		/**
		 * @return return the color of the indicator used when {@code position} is selected.
		 */
		int getIndicatorColor(int position);

	}

	private static final int TITLE_OFFSET_DIPS = 24;
	private static final int TAB_VIEW_PADDING_DIPS = 16;
	private static final int TAB_VIEW_TEXT_SIZE_SP = 12;

	/**
	 * Keeps track of the current tab open to avoid calling methods when a user clicks on an
	 *  already open tab
	 */
	private int mCurrentPosition = -1;
	/* ICON VIEWS */
	/**
	 * True if the custom tab has an icon, false otherwise
	 */
	private boolean mHasIcon = false;
	/**
	 * The default selector Id, -1 if none set
	 */
	private int mDefaultSelectorId = -1;
	/**
	 * True if the custom tab should use the default selector, false otherwise
	 */
	private boolean mDefaultSelector;
	/**
	 * The Id of the ImageView for the icon
	 */
	private int mTabViewIconId;
	/**
	 * The array of Ids to use for the icon drawables
	 */
	private int[] mIconIds;


	private int mTitleOffset;

	private int mTabViewLayoutId;
	private int mTabViewTextViewId;
	private boolean mDistributeEvenly;

	private ViewPager mViewPager;
	private SparseArray<String> mContentDescriptions = new SparseArray<>();
	private ViewPager.OnPageChangeListener mViewPagerPageChangeListener;

	private final TabStrip mTabStrip;

	public TabLayout(Context context) {
		this(context, null);
	}

	public TabLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TabLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		// Disable the Scroll Bar
		setHorizontalScrollBarEnabled(false);
		// Make sure that the Tab Strips fills this View
		setFillViewport(true);

		mTitleOffset = (int) (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density);

		mTabStrip = new TabStrip(context);
		addView(mTabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	}

	/**
	 * Set the custom {@link TabColorizer} to be used.
	 *
	 * If you only require simple custmisation then you can use
	 * {@link #setSelectedIndicatorColors(int...)} to achieve
	 * similar effects.
	 */
	public void setCustomTabColorizer(TabColorizer tabColorizer) {
		mTabStrip.setCustomTabColorizer(tabColorizer);
	}

	public void setDistributeEvenly(boolean distributeEvenly) {
		mDistributeEvenly = distributeEvenly;
	}

	/**
	 * Sets the colors to be used for indicating the selected tab. These colors are treated as a
	 * circular array. Providing one color will mean that all tabs are indicated with the same color.
	 */
	public void setSelectedIndicatorColors(int... colors) {
		mTabStrip.setSelectedIndicatorColors(colors);
	}

	/**
	 * Set the {@link ViewPager.OnPageChangeListener}. When using {@link TabLayout} you are
	 * required to set any {@link ViewPager.OnPageChangeListener} through this method. This is so
	 * that the layout can update it's scroll position correctly.
	 *
	 * @see ViewPager#setOnPageChangeListener(ViewPager.OnPageChangeListener)
	 */
	public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
		mViewPagerPageChangeListener = listener;
	}

	/**
	 * Sets the default selector. By default, it will use the selectableItemBackground attribute,
	 *  but this will not work on API 10
	 * @param selectorId The selector Id
	 */
	public void setDefaultSelector(int selectorId){
		this.mDefaultSelectorId = selectorId;
	}

	/**
	 * @return The default background to use
	 */
	private int getDefaultBackground(){
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			throw new IllegalStateException("API 10 must have the default selector set");
		}
		else{
			TypedValue outValue = new TypedValue();
			//Use the custom default selector if it is set
			getContext().getTheme().resolveAttribute(mDefaultSelectorId != -1 ? mDefaultSelectorId :
					android.R.attr.selectableItemBackground, outValue, true);
			return outValue.resourceId;
		}
	}

	/**
	 * Set the custom layout to be inflated for the tab views.
	 *
	 * @param layoutResId Layout id to be inflated
	 * @param textViewId id of the {@link TextView} in the inflated view
	 */
	public void setCustomTabView(int layoutResId, int textViewId) {
		mTabViewLayoutId = layoutResId;
		mTabViewTextViewId = textViewId;
	}

	/**
	 * Sets the custom layout view that has an icon
	 *
	 * @param layoutResId     To Layout Id to be inflated
	 * @param textViewId      Id of the {@link TextView} in the inflated view
	 * @param imageViewId     Id of the {@link android.widget.ImageView} in the inflated view
	 * @param iconIds         The list of icon Ids to use for the icon
	 * @param defaultSelector True if we should use the default selector as the background, false
	 *                        otherwise. Note: This will override any set background
	 */
	public void setCustomTabView(int layoutResId, int textViewId, int imageViewId, int[] iconIds,
	                             boolean defaultSelector){
		setCustomTabView(layoutResId, textViewId);
		mDefaultSelector = defaultSelector;
		mHasIcon = true;
		mTabViewIconId = imageViewId;
		mIconIds = iconIds;
	}

	/**
	 * Sets the associated view pager. Note that the assumption here is that the pager content
	 * (number of tabs and tab titles) does not change after this call has been made.
	 */
	public void setViewPager(ViewPager viewPager) {
		mTabStrip.removeAllViews();

		mViewPager = viewPager;
		if (viewPager != null) {
			viewPager.setOnPageChangeListener(new InternalViewPagerListener());
			populateTabStrip();
		}
	}

	/**
	 * Create a default view to be used for tabs. This is called if a custom tab view is not set via
	 * {@link #setCustomTabView(int, int)}.
	 */
	protected TextView createDefaultTabView(Context context) {
		TextView textView = new TextView(context);
		textView.setGravity(Gravity.CENTER);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
		textView.setTypeface(Typeface.DEFAULT_BOLD);
		textView.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		textView.setBackgroundResource(getDefaultBackground());
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			textView.setAllCaps(true);
		}

		int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
		textView.setPadding(padding, padding, padding, padding);

		return textView;
	}

	private void populateTabStrip() {
		final PagerAdapter adapter = mViewPager.getAdapter();
		final View.OnClickListener tabClickListener = new TabViewPagerClickListener();

		for (int i = 0; i < adapter.getCount(); i++) {
			View tabView = null;
			TextView tabTitleView = null;

			if (mTabViewLayoutId != 0) {
				// If there is a custom tab view layout id set, try and inflate it
				tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip,
						false);
				tabTitleView = (TextView) tabView.findViewById(mTabViewTextViewId);
				//Set up the icon if needed
				if(mHasIcon){
					ImageView iconView = (ImageView)tabView.findViewById(mTabViewIconId);
					iconView.setImageResource(mIconIds[i]);
				}
				//Set the default selector if needed
				if(mDefaultSelector){
					tabView.setBackgroundResource(getDefaultBackground());
				}
			}

			if (tabView == null) {
				tabView = createDefaultTabView(getContext());
			}

			if (tabTitleView == null && TextView.class.isInstance(tabView)) {
				tabTitleView = (TextView) tabView;
			}

			if (mDistributeEvenly) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
				lp.width = 0;
				lp.weight = 1;
			}

			tabTitleView.setText(adapter.getPageTitle(i));
			tabView.setOnClickListener(tabClickListener);
			String desc = mContentDescriptions.get(i, null);
			if (desc != null) {
				tabView.setContentDescription(desc);
			}

			mTabStrip.addView(tabView);
			if (i == mViewPager.getCurrentItem()) {
				tabView.setSelected(true);
				mCurrentPosition = i;
			}
		}
	}

	public void setContentDescription(int i, String desc) {
		mContentDescriptions.put(i, desc);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (mViewPager != null) {
			scrollToTab(mViewPager.getCurrentItem(), 0);
		}
	}

	private void scrollToTab(int tabIndex, int positionOffset) {
		final int tabStripChildCount = mTabStrip.getChildCount();
		if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
			return;
		}

		View selectedChild = mTabStrip.getChildAt(tabIndex);
		if (selectedChild != null) {
			int targetScrollX = selectedChild.getLeft() + positionOffset;

			if (tabIndex > 0 || positionOffset > 0) {
				// If we're not at the first child and are mid-scroll, make sure we obey the offset
				targetScrollX -= mTitleOffset;
			}

			scrollTo(targetScrollX, 0);
		}
	}

	private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
		private int mScrollState;

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			int tabStripChildCount = mTabStrip.getChildCount();
			if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
				return;
			}

			mTabStrip.onViewPagerPageChanged(position, positionOffset);

			View selectedTitle = mTabStrip.getChildAt(position);
			int extraOffset = (selectedTitle != null)
					? (int) (positionOffset * selectedTitle.getWidth())
					: 0;
			scrollToTab(position, extraOffset);

			if (mViewPagerPageChangeListener != null) {
				mViewPagerPageChangeListener.onPageScrolled(position, positionOffset,
						positionOffsetPixels);
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			mScrollState = state;

			if (mViewPagerPageChangeListener != null) {
				mViewPagerPageChangeListener.onPageScrollStateChanged(state);
			}
		}

		@Override
		public void onPageSelected(int position) {
			if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
				mTabStrip.onViewPagerPageChanged(position, 0f);
				scrollToTab(position, 0);
			}
			for (int i = 0; i < mTabStrip.getChildCount(); i++) {
				mTabStrip.getChildAt(i).setSelected(position == i);
			}
			if (mViewPagerPageChangeListener != null) {
				mViewPagerPageChangeListener.onPageSelected(position);
			}
		}

	}

	private class TabViewPagerClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			for (int i = 0; i < mTabStrip.getChildCount(); i++) {
				if (v == mTabStrip.getChildAt(i)){
					//If this tab is already open, do nothing
					if(i == mCurrentPosition){
						return;
					}
					//Set the new position
					mCurrentPosition = i;
					mViewPager.setCurrentItem(i);
					return;
				}
			}
		}
	}

	/**
	 * Clears the tabs
	 */
	public void clear(){
		mTabStrip.removeAllViews();
	}

	/**
	 * @param position The position of the desired tab
	 * @return The tab view
	 */
	public View getTabView(int position){
		//Make sure that the position is within bounds
		if(position < 0 || position >= mTabStrip.getChildCount()){
			return null;
		}
		return mTabStrip.getChildAt(position);
	}

	/**
	 * Forces the OnClick of the currently opened tab
	 */
	public void clickCurrentTab(){
		int currentPosition = mCurrentPosition;
		//Set this to -1 to force the click action
		mCurrentPosition = -1;

		getTabView(currentPosition).performClick();
	}

	/* NON-VIEWPAGER TABS */

	/**
	 * Adds the tabs based on a list of Strings to use as tab titles
	 *
	 * @param callback   The {@link Callback} to call when a tab is clicked
	 * @param initialTab The initial tab to show
	 * @param titles     The titles for the tabs
	 */
	public void addTabs(Callback callback, int initialTab, List<String> titles){
		//Create a new listener based on the given callback
		TabClickListener listener = new TabClickListener(callback);
		View firsTab = null;

		//Go through the titles
		for(int i = 0; i < titles.size(); i ++){
			String title = titles.get(i);

			View tabView;
			TextView tabTitleView = null;

			//If there is a custom tab view layout id set, try and inflate it
			if(mTabViewLayoutId != 0){
				tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip,
						false);
				tabTitleView = (TextView) tabView.findViewById(mTabViewTextViewId);
				//Set up the icon if needed
				if(mHasIcon){
					ImageView iconView = (ImageView)tabView.findViewById(mTabViewIconId);
					iconView.setImageResource(mIconIds[i]);
				}
				//Set the default selector if needed
				if(mDefaultSelector){
					TypedValue outValue = new TypedValue();
					getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
							outValue, true);
					tabView.setBackgroundResource(outValue.resourceId);
				}
			}
			else {
				tabView = createDefaultTabView(getContext());
			}

			if(tabTitleView == null && TextView.class.isInstance(tabView)){
				tabTitleView = (TextView) tabView;
			}

			if (mDistributeEvenly) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
				lp.width = 0;
				lp.weight = 1;
			}

			tabTitleView.setText(title);
			tabView.setOnClickListener(listener);

			mTabStrip.addView(tabView);

			if(i == initialTab){
				firsTab = tabView;
			}
		}

		if(firsTab != null){
			//Click on the first tab if there is one
			firsTab.performClick();
			//Set the current position
			mCurrentPosition = initialTab;
		}
	}

	/**
	 * Adds the tabs based on the given titles
	 *
	 * @param callback   The {@link Callback} to use when a tab is selected
	 * @param initialTab The initial tab selected
	 * @param titles     The variable list of titles
	 */
	public void addTabs(Callback callback, int initialTab, String... titles){
		List<String> tabTitles = new ArrayList<>();
		Collections.addAll(tabTitles, titles);

		addTabs(callback, initialTab, tabTitles);
	}

	/**
	 * Adds the tabs based on a list of Strings to use as tab titles.
	 *  Assumes that the first tab is the selected one but will not call the callback
	 *
	 * @param callback The {@link Callback} to call when a tab is clicked
	 * @param titles   The titles for the tabs
	 */
	public void addTabs(Callback callback, List<String> titles){
		addTabs(callback, -1, titles);
	}

	/**
	 * Adds the tabs based on the given titles.
	 *  Assumes that the first tab is the selected one but will not call the callback
	 *
	 * @param callback The {@link Callback} to call when a tab is clicked
	 * @param titles   The variable list of titles
	 */
	public void addTabs(Callback callback, String... titles){
		addTabs(callback, -1, titles);
	}

	/**
	 * The OnClickListener used when we are using tabs without a ViewPager
	 */
	private class TabClickListener implements OnClickListener{
		/**
		 * The callback to call when a tab is selected
		 */
		private Callback mCallback;
		/**
		 * The ViewPagerListener instance to update the UI
		 */
		private InternalViewPagerListener listener;

		/**
		 * Default Constructor
		 *
		 * @param callback The callback
		 */
		public TabClickListener(Callback callback){
			//Create a new InternalViewPagerListener to update the UI
			this.listener = new InternalViewPagerListener();
			this.mCallback = callback;
		}

		@Override
		public void onClick(View v){
			for (int i = 0; i < mTabStrip.getChildCount(); i++) {
				if (v == mTabStrip.getChildAt(i)){
					//If this tab is already open, do nothing
					if(i == mCurrentPosition){
						return;
					}
					//Set the new position
					mCurrentPosition = i;

					//Call the appropriate listeners/callbacks
					mCallback.onTabSelected(i);
					listener.onPageSelected(i);
					return;
				}
			}
		}
	}

	/**
	 * Callback to implement when a tab is clicked on
	 */
	public static abstract class Callback {
		/**
		 * Called when a tab is selected
		 *
		 * @param position The tab position
		 */
		public abstract void onTabSelected(int position);
	}
}