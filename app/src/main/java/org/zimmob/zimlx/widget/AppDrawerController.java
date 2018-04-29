package org.zimmob.zimlx.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;

import net.gsantner.opoc.util.Callback;

import org.zimmob.zimlx.R;
import org.zimmob.zimlx.activity.Home;
import org.zimmob.zimlx.manager.Setup;
import org.zimmob.zimlx.util.Tool;

import io.codetail.widget.RevealFrameLayout;

public class AppDrawerController extends RevealFrameLayout {
    public AppDrawerPaged _drawerViewPaged;
    public AppDrawerVertical _drawerViewGrid;
    public int _drawerMode;
    public boolean _isOpen = false;
    private Callback.a2<Boolean, Boolean> _appDrawerCallback;
    private Animator _appDrawerAnimator;
    private Long _drawerAnimationTime = 500L;

    public AppDrawerController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppDrawerController(Context context) {
        super(context);
    }

    public AppDrawerController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setCallBack(Callback.a2<Boolean, Boolean> callBack) {
        _appDrawerCallback = callBack;
    }

    public View getDrawer() {
        return getChildAt(0);
    }

    public void open(int cx, int cy, int startRadius, int finalRadius) {
        if (_isOpen) return;
        _isOpen = true;
        _drawerAnimationTime = (long) (240 * Setup.appSettings().getOverallAnimationSpeedModifier());

        _appDrawerAnimator = io.codetail.animation.ViewAnimationUtils.createCircularReveal(getChildAt(0), cx, cy, startRadius, finalRadius);
        _appDrawerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        _appDrawerAnimator.setDuration(_drawerAnimationTime);
        _appDrawerAnimator.setStartDelay((int) (Setup.appSettings().getOverallAnimationSpeedModifier() * 200));
        _appDrawerCallback.callback(true, true);
        _appDrawerAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator p1) {
                getChildAt(0).setVisibility(View.VISIBLE);

                ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(getBackground(), PropertyValuesHolder.ofInt("alpha", 0, 255));
                animator.setDuration(_drawerAnimationTime);
                animator.start();

                switch (_drawerMode) {
                    case DrawerMode.HORIZONTAL_PAGED:
                        for (int i = 0; i < _drawerViewPaged._pages.size(); i++) {
                            _drawerViewPaged._pages.get(i).findViewById(R.id.group).setAlpha(1);
                        }
                        if (_drawerViewPaged._pages.size() > 0) {
                            View mGrid = _drawerViewPaged._pages.get(_drawerViewPaged.getCurrentItem()).findViewById(R.id.group);
                            mGrid.setAlpha(0);
                            mGrid.animate().alpha(1).setDuration(150L).setStartDelay(Math.max(_drawerAnimationTime - 50, 1)).setInterpolator(new AccelerateDecelerateInterpolator());
                        }
                        break;
                    case DrawerMode.VERTICAL:
                        _drawerViewGrid.recyclerView.setAlpha(0);
                        _drawerViewGrid.recyclerView.animate().alpha(1).setDuration(150L).setStartDelay(Math.max(_drawerAnimationTime - 50, 1)).setInterpolator(new AccelerateDecelerateInterpolator());
                        break;
                }
            }

            @Override
            public void onAnimationEnd(Animator p1) {
                _appDrawerCallback.callback(true, false);
            }

            @Override
            public void onAnimationCancel(Animator p1) {
            }

            @Override
            public void onAnimationRepeat(Animator p1) {
            }
        });

        _appDrawerAnimator.start();
    }

    public void close(int cx, int cy, int startRadius, int finalRadius) {
        if (!_isOpen) return;
        _isOpen = false;

        if (_appDrawerAnimator == null || _appDrawerAnimator.isRunning())
            return;

        _appDrawerAnimator = io.codetail.animation.ViewAnimationUtils.createCircularReveal(getChildAt(0), cx, cy, finalRadius, startRadius);
        _appDrawerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        _appDrawerAnimator.setDuration(_drawerAnimationTime);
        _appDrawerAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator p1) {
                //_closeCallBack.onStart();

                ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(getBackground(), PropertyValuesHolder.ofInt("alpha", 255, 0));
                animator.setDuration(_drawerAnimationTime);
                animator.start();
            }

            @Override
            public void onAnimationEnd(Animator p1) {
                _appDrawerCallback.callback(true, false);
            }

            @Override
            public void onAnimationCancel(Animator p1) {
            }

            @Override
            public void onAnimationRepeat(Animator p1) {
            }
        });

        switch (_drawerMode) {
            case DrawerMode.HORIZONTAL_PAGED:
                if (_drawerViewPaged._pages.size() > 0) {
                    View mGrid = _drawerViewPaged._pages.get(_drawerViewPaged.getCurrentItem()).findViewById(R.id.group);
                    mGrid.animate().setStartDelay(0).alpha(0).setDuration(60L).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                _appDrawerAnimator.start();
                            } catch (NullPointerException ignored) {
                            }
                        }
                    });
                }
                break;
            case DrawerMode.VERTICAL:
                _drawerViewGrid.recyclerView.animate().setStartDelay(0).alpha(0).setDuration(60L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            _appDrawerAnimator.start();
                        } catch (NullPointerException ignored) {
                        }
                    }
                });
                break;
        }
    }

    public void init() {
        if (isInEditMode()) return;
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        _drawerMode = Setup.appSettings().getDrawerStyle();
        switch (_drawerMode) {
            case DrawerMode.HORIZONTAL_PAGED:
                _drawerViewPaged = (AppDrawerPaged) layoutInflater.inflate(R.layout.view_app_drawer_paged, this, false);
                addView(_drawerViewPaged);
                layoutInflater.inflate(R.layout.view_drawer_indicator, this, true);
                break;
            case DrawerMode.VERTICAL:
                _drawerViewGrid = (AppDrawerVertical) layoutInflater.inflate(R.layout.view_app_drawer_vertical, this, false);
                int marginHorizontal = Tool.dp2px(Setup.appSettings().getVerticalDrawerHorizontalMargin(), getContext());
                int marginVertical = Tool.dp2px(Setup.appSettings().getVerticalDrawerVerticalMargin(), getContext());
                RevealFrameLayout.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                lp.leftMargin = marginHorizontal;
                lp.rightMargin = marginHorizontal;
                lp.topMargin = marginVertical;
                lp.bottomMargin = marginVertical;
                addView(_drawerViewGrid, lp);
                break;
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        }
        return insets;
    }

    public void reloadDrawerCardTheme() {
        switch (_drawerMode) {
            case DrawerMode.HORIZONTAL_PAGED:
                _drawerViewPaged.resetAdapter();
                break;
            case DrawerMode.VERTICAL:
                if (!Setup.appSettings().isDrawerShowCardView()) {
                    _drawerViewGrid.setCardBackgroundColor(Color.TRANSPARENT);
                    _drawerViewGrid.setCardElevation(0);
                } else {
                    _drawerViewGrid.setCardBackgroundColor(Setup.appSettings().getDrawerCardColor());
                    _drawerViewGrid.setCardElevation(Tool.dp2px(4, getContext()));
                }
                if (_drawerViewGrid.gridDrawerAdapter != null) {
                    _drawerViewGrid.gridDrawerAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    public void scrollToStart() {
        switch (_drawerMode) {
            case DrawerMode.HORIZONTAL_PAGED:
                _drawerViewPaged.setCurrentItem(0, false);
                break;
            case DrawerMode.VERTICAL:
                _drawerViewGrid.recyclerView.scrollToPosition(0);
                break;
        }
    }

    public void setHome(Home home) {
        switch (_drawerMode) {
            case DrawerMode.HORIZONTAL_PAGED:
                _drawerViewPaged.withHome(home, (PagerIndicator) findViewById(R.id.appDrawerIndicator));
                break;
            case DrawerMode.VERTICAL:
                break;
        }
    }

    public static class DrawerMode {
        public static final int HORIZONTAL_PAGED = 0;
        public static final int VERTICAL = 1;
    }
}