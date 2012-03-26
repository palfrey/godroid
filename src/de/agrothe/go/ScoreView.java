package de.agrothe.go;

import android.content.Context;
import android.widget.TableLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.GestureDetector;

public
class ScoreView
	extends TableLayout 
{
GestureDetector _gestureDetector;

public
ScoreView (
	final Context pContext,
	final AttributeSet pAttributeSet
	)
{
	super (pContext, pAttributeSet);
}

public
boolean onTouchEvent (
	final MotionEvent pEvent
	)
{
	_gestureDetector.onTouchEvent (pEvent);
	return true;
}
}
