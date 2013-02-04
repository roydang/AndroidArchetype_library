package com.nhn.android.archetype.base.worker.listener;

import java.util.Date;

import com.nhn.android.archetype.base.object.BaseObj;

/**
 * 이 Listener를 사용하면 자동으로 http response를 캐싱한다.
 * 주의 : 캐시가 존재하지 않는 경우 onPreload의 값은 null이고
 * 캐시가 존재할 경우 onSuccess는 호출되지 않는다!
 * @author psbreeze
 *
 */
public abstract class PreloadJsonListener implements JsonListener {
	/**
	 * cache가 존재하지 않는 경우 null이 리턴된다
	 * @param response
	 * @param cachedDate
	 */
	public abstract void onPreload(BaseObj response, Date cachedDate);
	
	/**
	 * onSuccess메소드가 스킵된 경우 실행
	 */
	public void onSkipSuccess(BaseObj baseObj) {
	}
}
