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
		
		//新闻随机抽取长句识别（2016年6月7日新闻,均以当日0点为基准时间计算）
		//例1
		normalizer.parse("昨天上午，第八轮中美战略与经济对话气候变化问题特别联合会议召开。中国气候变化事务特别代表解振华表示，今年中美两国在应对气候变化多边进程中政策对话的重点任务，是推动《巴黎协定》尽早生效。", "2016-06-07-00-00-00");
		unit = normalizer.getTimeUnit();
		System.out.println("昨天上午，第八轮中美战略与经济对话气候变化问题特别联合会议召开。中国气候变化事务特别代表解振华表示，今年中美两国在应对气候变化多边进程中政策对话的重点任务，是推动《巴黎协定》尽早生效。");
		for(int i = 0; i < unit.length; i++){
		    System.out.println("时间文本:"+unit[i].Time_Expression +",对应时间:"+ DateUtil.formatDateDefault(unit[i].getTime()));
		}
		 
		//例2
		normalizer.parse("《辽宁日报》今日报道，6月3日辽宁召开省委常委扩大会，会议从下午两点半开到六点半，主要议题为：落实中央巡视整改要求。", "2016-06-07-00-00-00");
		unit = normalizer.getTimeUnit();
		System.out.println("《辽宁日报》今日报道，6月3日辽宁召开省委常委扩大会，会议从下午两点半开到六点半，主要议题为：落实中央巡视整改要求。");
		for(int i = 0; i < unit.length; i++){
		    System.out.println("时间文本:"+unit[i].Time_Expression +",对应时间:"+ DateUtil.formatDateDefault(unit[i].getTime()));
		}
		 
		//例3
		normalizer.parse("去年11月起正式实施的刑法修正案（九）中明确，在法律规定的国家考试中，组织作弊的将入刑定罪，最高可处七年有期徒刑。另外，本月刚刚开始实施的新版《教育法》中也明确...", "2016-06-07-00-00-00");
		unit = normalizer.getTimeUnit();
		System.out.println("去年11月起正式实施的刑法修正案（九）中明确，在法律规定的国家考试中，组织作弊的将入刑定罪，最高可处七年有期徒刑。另外，本月刚刚开始实施的新版《教育法》中也明确...");
		for(int i = 0; i < unit.length; i++){
		    System.out.println("时间文本:"+unit[i].Time_Expression +",对应时间:"+ DateUtil.formatDateDefault(unit[i].getTime()));
		}
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

在`2016年6月7日9:44`执行測試，结果如下：


Hi，all。下周一下午三点开会

2016-06-13 15:00:00-false

周一开会

2016-06-13 00:00:00-true

下下周一开会

2016-06-20 00:00:00-true

6:30 起床

2016-06-08 06:30:00-false

6-3 春游

2016-06-03 00:00:00-true

6月3日  春游

2016-06-03 00:00:00-true

明天早上跑步

2016-06-08 08:00:00-false

本周日到下周日出差

2016-06-12 00:00:00-true

2016-06-19 00:00:00-true

周四下午三点到五点开会

2016-06-16 15:00:00-false

2016-06-16 17:00:00-false

昨天上午，第八轮中美战略与经济对话气候变化问题特别联合会议召开。中国气候变化事务特别代表解振华表示，今年中美两国在应对气候变化多边进程中政策对话的重点任务，是推动《巴黎协定》尽早生效。

时间文本:昨天上午,  对应时间:2016-06-06 10:00:00

时间文本:今年,  对应时间:2016-01-01 00:00:00

《辽宁日报》今日报道，6月3日辽宁召开省委常委扩大会，会议从下午两点半开到六点半，主要议题为：落实中央巡视整改要求。

时间文本:今日,  对应时间:2016-06-07 00:00:00

时间文本:6月3日,  对应时间:2016-06-03 00:00:00

时间文本:下午2点半,  对应时间:2016-06-03 14:30:00

时间文本:6点半,  对应时间:2016-06-03 18:30:00


去年11月起正式实施的刑法修正案（九）中明确，在法律规定的国家考试中，组织作弊的将入刑定罪，最高可处七年有期徒刑。另外，本月刚刚开始实施的新版《教育法》中也明确...

时间文本:去年11月,  对应时间:2015-11-01 00:00:00

时间文本:本月,  对应时间:2016-06-01 00:00:00



<br/>
<br/>
如果您使用并有意见和建议，欢迎在Issue和我交流。若觉得好用，你的star是对作者最好的支持。<br/>

Enjoy ** - shinyke**

