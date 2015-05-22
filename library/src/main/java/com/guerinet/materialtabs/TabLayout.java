/*
 * Copyright 2015 Julien Guerinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guerinet.materialtabs;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
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
	/**
	 * Dimensions used throughout the class
	 */
	private static final int TITLE_OFFSET_DIPS = 24;
	private static final int TAB_VIEW_PADDING_DIPS = 16;
	private static final int TAB_VIEW_TEXT_SIZE_SP = 12;
	/**
	 * The title offset
	 */
	private int mTitleOffset;
	/**
	 * Keeps track of the current tab open to avoid calling methods when a user clicks on an
	 *  already open tab
	 */
	private int mCurrentPosition = -1;
	/**
	 * The content descriptions to use for the tabs
	 */
	private SparseArray<String> mContentDescriptions = new SparseArray<>();
	/* VIEWS */
	/**
	 * Layout Id to use for a custom layout
	 */
	private int mTabViewLayoutId;
	/**
	 * TextView Id for the title of a custom layout
	 */
	private int mTabViewTextViewId;
	/**
	 * ImageView Id for the eventual icon of a custom layout
	 */
	private int mTabViewIconId;
	/**
	 * True if the custom tab has an icon, false otherwise
	 */
	private boolean mHasIcon = false;
	/**
	 * The default selector Id, null if none set
	 */
	private Integer mDefaultSelectorId = null;
	/**
	 * True if the custom tab should use the default selector, false otherwise
	 */
	private boolean mDefaultSelector;
	/**
	 * The text color to use for the tabs, null if none set
	 */
	private Integer mDefaultTextColorId = null;
	/**
	 * The array of Ids to use for the icon drawables
	 */
	private int[] mIconIds;
	/**
	 * True if the tabs should be distributed evenly, false otherwise
	 */
	private boolean mDistributeEvenly;
	/* VIEWPAGER STUFF */
	/**
	 * The {@link ViewPager} instance if the tabs are associated to a ViewPager
	 */
	private ViewPager mViewPager;
	/**
	 * The {@link ViewPager.OnPageChangeListener} to update the selector view
	 */
	private ViewPager.OnPageChangeListener mViewPagerPageChangeListener;
	/**
	 * The tab strip containing the list of tabs
	 */
	private final TabStrip mTabStrip;

	/**
	 * Default Constructor
	 *
	 * @param context The app context
	 */
	public TabLayout(Context context) {
		this(context, null);
	}

	/**
	 * Default Constructor with an attribute set
	 *
	 * @param context The app context
	 * @param attrs   The list of attributes
	 */
	public TabLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Default Constructor with an attribute set and the default style resource
	 *
	 * @param context  The app context
	 * @param attrs    The list of attributes
	 * @param defStyle The default style resource
	 */
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

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		//Scroll to the current ViewPager position if there is one
		if (mViewPager != null) {
			scrollToTab(mViewPager.getCurrentItem(), 0);
		}
	}

	/* GETTERS */

	/**
	 * @return The default background to use
	 */
	private int getTabBackground(){
		TypedValue outValue = new TypedValue();

		if(mDefaultSelectorId == null){
			//If we are in API 10 and a selector has not been set, throw an exception
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
				throw new IllegalStateException("API 10 must have the default selector set");
			}
			else{
				getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
						outValue, true);
			}
		}
		else{
			//Use the set selector Id
			getContext().getTheme().resolveAttribute(mDefaultSelectorId, outValue, true);
		}
		return outValue.resourceId;
	}

	/**
	 * @param position The position of the desired tab
	 * @return The tab view
	 */
	public View getTabView(int position){
		if(position < 0 || position >= mTabStrip.getChildCount()){
			return null;
		}

		return mTabStrip.getChildAt(position);
	}

	/**
	 * @return The tab currently opened
	 */
	public int getCurrentTab(){
		return mCurrentPosition;
	}

	/**
	 * @return Te view of the current tab
	 */
	public View getCurrentTabView(){
		return getTabView(mCurrentPosition);
	}

	/* SETTERS */

	/**
	 * Set the custom {@link TabColorizer} to be used.
	 *
	 * If you only require simple customisation then you can use
	 * {@link #setSelectedIndicatorColors(int...)} to achieve
	 * similar effects.
	 */
	public void setCustomTabColorizer(TabColorizer tabColorizer) {
		mTabStrip.setCustomTabColorizer(tabColorizer);
	}

	/**
	 * Gives the tabs equal weights if enabled
	 *
	 * @param distributeEvenly True if the tabs should be evenly distributed, false otherwise
	 */
	public void setDistributeEvenly(boolean distributeEvenly) {
		mDistributeEvenly = distributeEvenly;
	}

	/**
	 * Sets the default tab text color
	 *
	 * @param textColorId The Id of the text color to use
	 */
	public void setDefaultTextColor(int textColorId){
		this.mDefaultTextColorId = textColorId;
	}

	/**
	 * Sets the default selector. By default, it will use the selectableItemBackground attribute,
	 *  but this will not work on API 10
	 *
	 * @param selectorId The selector Id
	 */
	public void setDefaultSelector(int selectorId){
		this.mDefaultSelectorId = selectorId;
	}

	/**
	 * @param i    The tab number
	 * @param desc The tab's content description
	 */
	public void setContentDescription(int i, String desc) {
		mContentDescriptions.put(i, desc);
	}

	/**
	 * Sets the colors to be used for indicating the selected tab. These colors are treated as a
	 * circular array. Providing one color will mean that all tabs are indicated with the same
	 * color.
	 *
	 * @param colors The list of indicator colors
	 */
	public void setSelectedIndicatorColors(int... colors) {
		mTabStrip.setSelectedIndicatorColors(colors);
	}

	/**
	 * Sets all of the needed colors
	 *
	 * @param selectorId        The selector Id
	 * @param textColorId       The text color Id
	 * @param indicatorColorIds The indicator color Id(s)
	 */
	public void setColors(int selectorId, int textColorId, int... indicatorColorIds){
		setDefaultSelector(selectorId);
		setDefaultTextColor(textColorId);

		//Change the indicator color Ids to actual colors
		for(int i = 0; i < indicatorColorIds.length; i++){
			indicatorColorIds[i] = getResources().getColor(indicatorColorIds[i]);
		}
		setSelectedIndicatorColors(indicatorColorIds);
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
	 * Sets the custom layout to be inflated for the tab views.
	 *
	 * @param layoutResId     Layout id to be inflated
	 * @param textViewId      Id of the {@link TextView} in the inflated view
	 * @param defaultSelector True if the default selector should be used, false otherwise
	 */
	public void setCustomTabView(int layoutResId, int textViewId, boolean defaultSelector) {
		mTabViewLayoutId = layoutResId;
		mTabViewTextViewId = textViewId;
		mDefaultSelector = defaultSelector;
	}

	/**
	 * Sets the custom layout to be inflated for the tab views. Uses the default background selector
	 *
	 * @param layoutResId Layout Id to be inflated
	 * @param textViewId  Id of the {@link TextView} in the inflated view
	 */
	public void setCustomTabView(int layoutResId, int textViewId){
		setCustomTabView(layoutResId, textViewId, true);
	}

	/**
	 * Sets the custom layout view that has an icon
	 *
	 * @param layoutResId     Layout Id to be inflated
	 * @param textViewId      Id of the {@link TextView} in the inflated view
	 * @param imageViewId     Id of the {@link ImageView} in the inflated view
	 * @param defaultSelector True if the default selector should be used, false otherwise
	 * @param iconIds         Ids of the drawables to use for the icons
	 */
	public void setCustomTabView(int layoutResId, int textViewId, int imageViewId,
	                             boolean defaultSelector, int... iconIds){
		setCustomTabView(layoutResId, textViewId, defaultSelector);
		mTabViewIconId = imageViewId;
		mIconIds = iconIds;
	}

	/**
	 * Sets the custom layout view that has an icon. Uses the default background selector
	 *
	 * @param layoutResId Layout Id to be inflated
	 * @param textViewId  Id of the {@link TextView} in the inflated view
	 * @param imageViewId Id of the {@link ImageView} in the inflated view
	 * @param iconIds     Id(s) of the drawables to use for the icons
	 */
	public void setCustomTabView(int layoutResId, int textViewId, int imageViewId, int... iconIds){
		setCustomTabView(layoutResId, textViewId, imageViewId, true, iconIds);
	}

	/**
	 * Sets the associated view pager. Note that the assumption here is that the pager content
	 * (number of tabs and tab titles) does not change after this call has been made.
	 *
	 * @param viewPager The {@link ViewPager}
	 */
	public void setViewPager(ViewPager viewPager) {
		//Remove all existing views
		clear();

		mViewPager = viewPager;
		if (viewPager != null) {
			viewPager.setOnPageChangeListener(new InternalViewPagerListener());
			populateTabStrip();
		}
	}

	/* HELPERS */

	/**
	 * Clears the tabs
	 */
	public void clear(){
		mTabStrip.removeAllViews();
	}

	/**
	 * Scrolls to the specified tab
	 *
	 * @param tabIndex       The index of the tab to scroll to
	 * @param positionOffset The position offset
	 */
	private void scrollToTab(int tabIndex, int positionOffset) {
		View selectedChild = getTabView(tabIndex);

		//No need to continue if the tab doesn't exist
		if(selectedChild != null){
			int targetScrollX = selectedChild.getLeft() + positionOffset;

			if (tabIndex > 0 || positionOffset > 0) {
				// If we're not at the first child and are mid-scroll, make sure we obey the offset
				targetScrollX -= mTitleOffset;
			}

			scrollTo(targetScrollX, 0);
		}
	}

	/**
	 * Sets up the title {@link TextView} as per the material guidelines
	 *
	 * @param textView The TextView
	 */
	private void prepareTextView(TextView textView){
		//Keep it to 1 line or the selectors won't work
		textView.setSingleLine();

		//Set the text to all caps if we are in 14+
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			textView.setAllCaps(true);
		}

		//Tabs are bold
		textView.setTypeface(Typeface.DEFAULT_BOLD);
	}

	/**
	 * Creates a default view to be used for tabs. This is called if a custom tab view is not set
	 * via {@link #setCustomTabView(int, int)}.
	 *
	 * @param context The app context
	 * @return The default view to use
	 */
	protected TextView createDefaultTabView(Context context){
		TextView textView = new TextView(context);
		prepareTextView(textView);
		textView.setGravity(Gravity.CENTER);
		textView.setEllipsize(TextUtils.TruncateAt.END);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
		//Set the text color if there is one
		if(this.mDefaultTextColorId != null){
			textView.setTextColor(getResources().getColor(mDefaultTextColorId));
		}
		textView.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		textView.setBackgroundResource(getTabBackground());

		//Padding
		int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
		textView.setPadding(padding, padding, padding, padding);

		return textView;
	}

	private void populateTabStrip() {
		final PagerAdapter adapter = mViewPager.getAdapter();
		final View.OnClickListener tabClickListener = new TabClickListener();

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
					tabView.setBackgroundResource(getTabBackground());
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

	/**
	 * Forces the OnClick of the currently opened tab
	 */
	public void clickCurrentTab(){
		int currentPosition = getCurrentTab();
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
	 * {@link ViewPager.OnPageChangeListener} to use to update the selector
	 */
	private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
		/**
		 * The current scroll state. By default idle so that the indicator is scrolled to a tab
		 *  when not using a ViewPager.
		 */
		private int mScrollState = ViewPager.SCROLL_STATE_IDLE;

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
			mTabStrip.onViewPagerPageChanged(position, positionOffset);

			View selectedTitle = getTabView(position);
			int extraOffset = (selectedTitle != null) ?
					(int) (positionOffset * selectedTitle.getWidth()) : 0;
			scrollToTab(position, extraOffset);

			//Call the page listener if there's an associated one
			if(mViewPagerPageChangeListener != null){
				mViewPagerPageChangeListener.onPageScrolled(position, positionOffset,
						positionOffsetPixels);
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			mScrollState = state;

			//Call the page listener if there's an associated one
			if(mViewPagerPageChangeListener != null){
				mViewPagerPageChangeListener.onPageScrollStateChanged(state);
			}
		}

		@Override
		public void onPageSelected(int position) {
			//Update the current position (only useful when using this with a ViewPager)
			mCurrentPosition = position;
			if(mScrollState == ViewPager.SCROLL_STATE_IDLE){
				mTabStrip.onViewPagerPageChanged(position, 0f);
				scrollToTab(position, 0);
			}

			//Go through the children and set their selected state depending on the selected page
			for(int i = 0; i < mTabStrip.getChildCount(); i++){
				mTabStrip.getChildAt(i).setSelected(position == i);
			}

			//Call the page listener if there's an associated one
			if(mViewPagerPageChangeListener != null){
				mViewPagerPageChangeListener.onPageSelected(position);
			}
		}
	}

	/**
	 * {@link View.OnClickListener} used for the tabs
	 */
	private class TabClickListener implements OnClickListener{
		/**
		 * The callback to call when a tab is selected for when using the tabs without a ViewPager
		 */
		private Callback mCallback = null;
		/**
		 * The listener to use when using the tabs without a ViewPager
		 */
		private InternalViewPagerListener mListener = null;

		/**
		 * Constructor to use when not using a ViewPager
		 *
		 * @param callback The callback
		 */
		public TabClickListener(Callback callback){
			//Create a new InternalViewPagerListener to update the UI
			mListener = new InternalViewPagerListener();
			mCallback = callback;
		}

		/**
		 * Constructor to use when using a ViewPager
		 */
		public TabClickListener(){}

		@Override
		public void onClick(View v){
			//Go through the tabs
			for (int i = 0; i < mTabStrip.getChildCount(); i++) {
				if (v == mTabStrip.getChildAt(i)){
					//If this tab is already open, do nothing
					if(i == mCurrentPosition){
						return;
					}
					//Set the new position
					mCurrentPosition = i;

					//Is using the ViewPager, set the new item
					if(mListener == null){
						mViewPager.setCurrentItem(i);
					}
					//If not, call the appropriate listeners/callbacks
					else{
						mListener.onPageSelected(i);
						mCallback.onTabSelected(i);
					}
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