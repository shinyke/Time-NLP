/**
 * Copyright (c) 2016 21CN.COM . All rights reserved.<br>
 *
 * Description: calendarCommon<br>
 * 
 * Modified log:<br>
 * ------------------------------------------------------<br>
 * Ver.		Date		Author			Description<br>
 * ------------------------------------------------------<br>
 * 1.0		2016年4月25日		kexm		created.<br>
 */
package com.time.util;

/**
 * <p>
 * 字符串工具类
 * <p>
 * @author <a href="mailto:kexm@corp.21cn.com">kexm</a>
 * @version 
 * @since 2016年4月25日
 * 
 */
public class StringUtil {

	/**
	 * 字符串是否为空
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		return ((str == null) || (str.trim().length() == 0));
	}

}
