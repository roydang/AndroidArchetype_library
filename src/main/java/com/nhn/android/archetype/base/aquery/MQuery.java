package com.nhn.android.archetype.base.aquery;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.androidquery.AbstractAQuery;

public class MQuery extends AbstractAQuery<MQuery>{
	public MQuery(Activity act) {
		super(act);
	}
	
	public MQuery(View view) {
		super(view);
	}
	
	public MQuery(Context context) {
		super(context);
	}
	
	public MQuery(Activity act, View root){
		super(act, root);
	}
}
