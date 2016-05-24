# Time-NLP
#中文语句中的时间语义识别

author：shinyke


本工具是由复旦NLP中的时间分析功能修改而来，做了很多细节和功能的优化，具体如下：

1. **泛指时间**的支持，如：早上、晚上、中午、傍晚等。
2. 时间**未来倾向**。 如：在周五输入“周一早上开会”，则识别到下周一早上的时间；在下午17点输入：“9点送牛奶给隔壁的汉子”则识别到第二天上午9点。
3. **多个时间的识别，及多个时间之间上下文关系处理**。如："下月1号下午3点至5点到图书馆还书"，识别到开始时间为下月1号下午三点。同时，结束时间也**继承上文时间**，识别到下月1号下午5点。
4. 可**自定义基准时间**：指定基准时间为“2016-05-20-09-00-00-00”，则一切分析以此时间为基准。
5. **修复了各种各样的BUG**。

<font color=#0099ff>简而言之，这是一个输入一句话，能识别出话里的时间的工具。╮(╯▽╰)╭</fo>

使用方法详见测试类：
``` java
/**
 * <p>
 * 测试类
 * <p>
 * @author <a href="mailto:xinmeike@163.com">kexm</a>
 * @version 1.0
 * @since 2016年5月4日
 * 
 */
public class TimeAnalyseTest {
	
	@Test
	public void test(){
		String path = TimeNormalizer.class.getResource("").getPath();
		String classPath = path.substring(0, path.indexOf("/com/time"));
		System.out.println(classPath+"/TimeExp.m");
		TimeNormalizer normalizer = new TimeNormalizer(classPath+"/TimeExp.m");

		
		normalizer.parse("Hi，all.下周一下午三点开会");// 抽取时间
		TimeUnit[] unit = normalizer.getTimeUnit();
		System.out.println("Hi，all.下周一下午三点开会");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime()); 
		
		normalizer.parse("早上六点起床");// 注意此处识别到6天在今天已经过去，自动识别为明早六点（未来倾向，可通过开关关闭：new TimeNormalizer(classPath+"/TimeExp.m", false)）
		unit = normalizer.getTimeUnit();
		System.out.println("早上六点起床");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		
		normalizer.parse("周一开会");// 如果本周已经是周二，识别为下周周一。同理处理各级时间。（未来倾向）
		unit = normalizer.getTimeUnit();
		System.out.println("周一开会");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		
		normalizer.parse("下下周一开会");//对于上/下的识别
		unit = normalizer.getTimeUnit();
		System.out.println("下下周一开会");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		
		normalizer.parse("6:30 起床");// 严格时间格式的识别
		unit = normalizer.getTimeUnit();
		System.out.println("6:30 起床");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		
		normalizer.parse("6-3 春游");// 严格时间格式的识别
		unit = normalizer.getTimeUnit();
		System.out.println("6-3 春游");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		
		normalizer.parse("6月3  春游");// 残缺时间的识别 （打字输入时可便捷用户）
		unit = normalizer.getTimeUnit();
		System.out.println("6月3  春游");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		
		normalizer.parse("明天早上跑步");// 模糊时间范围识别（可在RangeTimeEnum中修改
		unit = normalizer.getTimeUnit();
		System.out.println("明天早上跑步");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		
		normalizer.parse("本周日到下周日出差");// 多时间识别
		unit = normalizer.getTimeUnit();
		System.out.println("本周日到下周日出差");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		System.out.println(DateUtil.formatDateDefault(unit[1].getTime()) + "-" + unit[1].getIsAllDayTime());
		
		normalizer.parse("周四下午三点到五点开会");// 多时间识别，注意第二个时间点用了第一个时间的上文
		unit = normalizer.getTimeUnit();
		System.out.println("周四下午三点到五点开会");
		System.out.println(DateUtil.formatDateDefault(unit[0].getTime()) + "-" + unit[0].getIsAllDayTime());
		System.out.println(DateUtil.formatDateDefault(unit[1].getTime()) + "-" + unit[1].getIsAllDayTime());
		
	}
	
	/**
	 * 修改TimeExp.m文件的内容
	 */
	@Test
	public void editTimeExp(){
		String path = TimeNormalizer.class.getResource("").getPath();
		String classPath = path.substring(0, path.indexOf("/com/time"));
		System.out.println(classPath+"/TimeExp.m");
		/**写TimeExp*/
		Pattern p = Pattern.compile("your-regex");
		try {
			TimeNormalizer.writeModel(p, classPath+"/TimeExp.m");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
```
<br>
在`2016年5月24日11:47`測試，结果如下：<br>
Hi，all。下周一下午三点开会<br>
2016-05-30 15:00:00-false<br>
早上六点起床<br>
2016-05-25 06:00:00-false<br>
周一开会<br>
2016-05-30 00:00:00-true<br>
下下周一开会<br>
2016-06-06 00:00:00-true<br>
6:30 起床<br>
2016-05-25 06:30:00-false<br>
6-3 春游<br>
2016-06-03 00:00:00-true<br>
6月3  春游<br>
2016-06-03 00:00:00-true<br>
明天早上跑步<br>
2016-05-25 08:00:00-false<br>
本周日到下周日出差<br>
2016-05-29 00:00:00-true<br>
2016-06-05 00:00:00-true<br>
周四下午三点到五点开会<br>
2016-06-02 15:00:00-false<br>
2016-06-02 17:00:00-false<br>
<br>
Enjoy。** - shinyke**

