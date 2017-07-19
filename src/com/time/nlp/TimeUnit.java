package com.time.nlp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.time.enums.RangeTimeEnum;

/**
 * <p>
 * 时间语句分析
 * <p>
 *
 * @author <a href="mailto:kexm@corp.21cn.com">kexm</a>
 * @since 2016年5月4日
 */
public class TimeUnit {
	//有需要可使用
	//private static final Logger LOGGER = LoggerFactory.getLogger(TimeUnit.class);
    /**
     * 目标字符串
     */
    public String Time_Expression = null;
    public String Time_Norm = "";
    public int[] time_full;
    public int[] time_origin;
    private Date time;
    private Boolean isAllDayTime = true;
    private boolean isFirstTimeSolveContext = true;

    TimeNormalizer normalizer = null;
    public TimePoint _tp = new TimePoint();
    public TimePoint _tp_origin = new TimePoint();

    /**
     * 时间表达式单元构造方法
     * 该方法作为时间表达式单元的入口，将时间表达式字符串传入
     *
     * @param exp_time 时间表达式字符串
     * @param n
     */

    public TimeUnit(String exp_time, TimeNormalizer n) {
        Time_Expression = exp_time;
        normalizer = n;
        Time_Normalization();
    }

    /**
     * 时间表达式单元构造方法
     * 该方法作为时间表达式单元的入口，将时间表达式字符串传入
     *
     * @param exp_time  时间表达式字符串
     * @param n
     * @param contextTp 上下文时间
     */

    public TimeUnit(String exp_time, TimeNormalizer n, TimePoint contextTp) {
        Time_Expression = exp_time;
        normalizer = n;
        _tp_origin = contextTp;
        Time_Normalization();
    }

    /**
     * return the accurate time object
     */
    public Date getTime() {
        return time;
    }

    /**
     * 年-规范化方法
     * <p>
     * 该方法识别时间表达式单元的年字段
     */
    public void norm_setyear() {
        /**假如只有两位数来表示年份*/
        String rule = "[0-9]{2}(?=年)";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[0] = Integer.parseInt(match.group());
            if (_tp.tunit[0] >= 0 && _tp.tunit[0] < 100) {
                if (_tp.tunit[0] < 30) /**30以下表示2000年以后的年份*/
                    _tp.tunit[0] += 2000;
                else/**否则表示1900年以后的年份*/
                    _tp.tunit[0] += 1900;
            }

        }
        /**不仅局限于支持1XXX年和2XXX年的识别，可识别三位数和四位数表示的年份*/
        rule = "[0-9]?[0-9]{3}(?=年)";

        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find())/**如果有3位数和4位数的年份，则覆盖原来2位数识别出的年份*/ {
            _tp.tunit[0] = Integer.parseInt(match.group());
        }
    }

    /**
     * 月-规范化方法
     * <p>
     * 该方法识别时间表达式单元的月字段
     */
    public void norm_setmonth() {
        String rule = "((10)|(11)|(12)|([1-9]))(?=月)";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[1] = Integer.parseInt(match.group());

            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(1);
        }
    }

    /**
     * 月-日 兼容模糊写法
     * <p>
     * 该方法识别时间表达式单元的月、日字段
     * <p>
     * add by kexm
     */
    public void norm_setmonth_fuzzyday() {
        String rule = "((10)|(11)|(12)|([1-9]))(月|\\.|\\-)([0-3][0-9]|[1-9])";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            String matchStr = match.group();
            Pattern p = Pattern.compile("(月|\\.|\\-)");
            Matcher m = p.matcher(matchStr);
            if (m.find()) {
                int splitIndex = m.start();
                String month = matchStr.substring(0, splitIndex);
                String date = matchStr.substring(splitIndex + 1);

                _tp.tunit[1] = Integer.parseInt(month);
                _tp.tunit[2] = Integer.parseInt(date);

                /**处理倾向于未来时间的情况  @author kexm*/
                preferFuture(1);
            }
        }
    }

    /**
     * 日-规范化方法
     * <p>
     * 该方法识别时间表达式单元的日字段
     */
    public void norm_setday() {
        String rule = "((?<!\\d))([0-3][0-9]|[1-9])(?=(日|号))";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[2] = Integer.parseInt(match.group());

            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(2);
        }
    }

    /**
     * 时-规范化方法
     * <p>
     * 该方法识别时间表达式单元的时字段
     */
    public void norm_sethour() {
        String rule = "(?<!(周|星期))([0-2]?[0-9])(?=(点|时))";

        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[3] = Integer.parseInt(match.group());
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }
        /*
         * 对关键字：早（包含早上/早晨/早间），上午，中午,午间,下午,午后,晚上,傍晚,晚间,晚,pm,PM的正确时间计算
		 * 规约：
		 * 1.中午/午间0-10点视为12-22点
		 * 2.下午/午后0-11点视为12-23点
		 * 3.晚上/傍晚/晚间/晚1-11点视为13-23点，12点视为0点
		 * 4.0-11点pm/PM视为12-23点
		 * 
		 * add by kexm
		 */
        rule = "凌晨";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“凌晨”这种情况的处理 @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.day_break.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }

        rule = "早上|早晨|早间|晨间|今早|明早";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“早上/早晨/早间”这种情况的处理 @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.early_morning.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }

        rule = "上午";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“上午”这种情况的处理 @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.morning.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }

        rule = "(中午)|(午间)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] >= 0 && _tp.tunit[3] <= 10)
                _tp.tunit[3] += 12;
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“中午/午间”这种情况的处理 @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.noon.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }

        rule = "(下午)|(午后)|(pm)|(PM)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] >= 0 && _tp.tunit[3] <= 11)
                _tp.tunit[3] += 12;
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“下午|午后”这种情况的处理  @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.afternoon.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }

        rule = "晚上|夜间|夜里|今晚|明晚";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] >= 1 && _tp.tunit[3] <= 11)
                _tp.tunit[3] += 12;
            else if (_tp.tunit[3] == 12)
                _tp.tunit[3] = 0;
            else if (_tp.tunit[3] == -1)
                _tp.tunit[3] = RangeTimeEnum.night.getHourTime();

            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }

    }

    /**
     * 分-规范化方法
     * <p>
     * 该方法识别时间表达式单元的分字段
     */
    public void norm_setminute() {
        String rule = "([0-5]?[0-9](?=分(?!钟)))|((?<=((?<!小)[点时]))[0-5]?[0-9](?!刻))";

        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (!match.group().equals("")) {
                _tp.tunit[4] = Integer.parseInt(match.group());
                /**处理倾向于未来时间的情况  @author kexm*/
                preferFuture(4);
                isAllDayTime = false;
            }
        }
        /** 加对一刻，半，3刻的正确识别（1刻为15分，半为30分，3刻为45分）*/
        rule = "(?<=[点时])[1一]刻(?!钟)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[4] = 15;
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(4);
            isAllDayTime = false;
        }

        rule = "(?<=[点时])半";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[4] = 30;
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(4);
            isAllDayTime = false;
        }

        rule = "(?<=[点时])[3三]刻(?!钟)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[4] = 45;
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(4);
            isAllDayTime = false;
        }
    }

    /**
     * 秒-规范化方法
     * <p>
     * 该方法识别时间表达式单元的秒字段
     */
    public void norm_setsecond() {
		/*
		 * 添加了省略“分”说法的时间
		 * 如17点15分32
		 * modified by 曹零
		 */
        String rule = "([0-5]?[0-9](?=秒))|((?<=分)[0-5]?[0-9])";

        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            _tp.tunit[5] = Integer.parseInt(match.group());
            isAllDayTime = false;
        }
    }

    /**
     * 特殊形式的规范化方法
     * <p>
     * 该方法识别特殊形式的时间表达式单元的各个字段
     */
    public void norm_setTotal() {
        String rule;
        Pattern pattern;
        Matcher match;
        String[] tmp_parser;
        String tmp_target;

        rule = "(?<!(周|星期))([0-2]?[0-9]):[0-5]?[0-9]:[0-5]?[0-9]";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            tmp_parser = new String[3];
            tmp_target = match.group();
            tmp_parser = tmp_target.split(":");
            _tp.tunit[3] = Integer.parseInt(tmp_parser[0]);
            _tp.tunit[4] = Integer.parseInt(tmp_parser[1]);
            _tp.tunit[5] = Integer.parseInt(tmp_parser[2]);
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        } else {
            rule = "(?<!(周|星期))([0-2]?[0-9]):[0-5]?[0-9]";
            pattern = Pattern.compile(rule);
            match = pattern.matcher(Time_Expression);
            if (match.find()) {
                tmp_parser = new String[2];
                tmp_target = match.group();
                tmp_parser = tmp_target.split(":");
                _tp.tunit[3] = Integer.parseInt(tmp_parser[0]);
                _tp.tunit[4] = Integer.parseInt(tmp_parser[1]);
                /**处理倾向于未来时间的情况  @author kexm*/
                preferFuture(3);
                isAllDayTime = false;
            }
        }
		/*
		 * 增加了:固定形式时间表达式的
		 * 中午,午间,下午,午后,晚上,傍晚,晚间,晚,pm,PM
		 * 的正确时间计算，规约同上
		 */
        rule = "(中午)|(午间)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] >= 0 && _tp.tunit[3] <= 10)
                _tp.tunit[3] += 12;
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“中午/午间”这种情况的处理 @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.noon.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;

        }

        rule = "(下午)|(午后)|(pm)|(PM)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] >= 0 && _tp.tunit[3] <= 11)
                _tp.tunit[3] += 12;
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“中午/午间”这种情况的处理 @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.afternoon.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }

        rule = "晚";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            if (_tp.tunit[3] >= 1 && _tp.tunit[3] <= 11)
                _tp.tunit[3] += 12;
            else if (_tp.tunit[3] == 12)
                _tp.tunit[3] = 0;
            if (_tp.tunit[3] == -1) /**增加对没有明确时间点，只写了“中午/午间”这种情况的处理 @author kexm*/
                _tp.tunit[3] = RangeTimeEnum.night.getHourTime();
            /**处理倾向于未来时间的情况  @author kexm*/
            preferFuture(3);
            isAllDayTime = false;
        }


        rule = "[0-9]?[0-9]?[0-9]{2}-((10)|(11)|(12)|([1-9]))-((?<!\\d))([0-3][0-9]|[1-9])";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            tmp_parser = new String[3];
            tmp_target = match.group();
            tmp_parser = tmp_target.split("-");
            _tp.tunit[0] = Integer.parseInt(tmp_parser[0]);
            _tp.tunit[1] = Integer.parseInt(tmp_parser[1]);
            _tp.tunit[2] = Integer.parseInt(tmp_parser[2]);
        }

        rule = "((10)|(11)|(12)|([1-9]))/((?<!\\d))([0-3][0-9]|[1-9])/[0-9]?[0-9]?[0-9]{2}";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            tmp_parser = new String[3];
            tmp_target = match.group();
            tmp_parser = tmp_target.split("/");
            _tp.tunit[1] = Integer.parseInt(tmp_parser[0]);
            _tp.tunit[2] = Integer.parseInt(tmp_parser[1]);
            _tp.tunit[0] = Integer.parseInt(tmp_parser[2]);
        }
		
		/*
		 * 增加了:固定形式时间表达式 年.月.日 的正确识别
		 * add by 曹零
		 */
        rule = "[0-9]?[0-9]?[0-9]{2}\\.((10)|(11)|(12)|([1-9]))\\.((?<!\\d))([0-3][0-9]|[1-9])";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            tmp_parser = new String[3];
            tmp_target = match.group();
            tmp_parser = tmp_target.split("\\.");
            _tp.tunit[0] = Integer.parseInt(tmp_parser[0]);
            _tp.tunit[1] = Integer.parseInt(tmp_parser[1]);
            _tp.tunit[2] = Integer.parseInt(tmp_parser[2]);
        }
    }

    /**
     * 设置以上文时间为基准的时间偏移计算
     */
    public void norm_setBaseRelated() {
        String[] time_grid = new String[6];
        time_grid = normalizer.getTimeBase().split("-");
        int[] ini = new int[6];
        for (int i = 0; i < 6; i++)
            ini[i] = Integer.parseInt(time_grid[i]);

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(ini[0], ini[1] - 1, ini[2], ini[3], ini[4], ini[5]);
        calendar.getTime();

        boolean[] flag = {false, false, false};//观察时间表达式是否因当前相关时间表达式而改变时间


        String rule = "\\d+(?=天[以之]?前)";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            int day = Integer.parseInt(match.group());
            calendar.add(Calendar.DATE, -day);
        }

        rule = "\\d+(?=天[以之]?后)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            int day = Integer.parseInt(match.group());
            calendar.add(Calendar.DATE, day);
        }

        rule = "\\d+(?=(个)?月[以之]?前)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[1] = true;
            int month = Integer.parseInt(match.group());
            calendar.add(Calendar.MONTH, -month);
        }

        rule = "\\d+(?=(个)?月[以之]?后)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[1] = true;
            int month = Integer.parseInt(match.group());
            calendar.add(Calendar.MONTH, month);
        }

        rule = "\\d+(?=年[以之]?前)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[0] = true;
            int year = Integer.parseInt(match.group());
            calendar.add(Calendar.YEAR, -year);
        }

        rule = "\\d+(?=年[以之]?后)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[0] = true;
            int year = Integer.parseInt(match.group());
            calendar.add(Calendar.YEAR, year);
        }

        String s = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(calendar.getTime());
        String[] time_fin = s.split("-");
        if (flag[0] || flag[1] || flag[2]) {
            _tp.tunit[0] = Integer.parseInt(time_fin[0]);
        }
        if (flag[1] || flag[2])
            _tp.tunit[1] = Integer.parseInt(time_fin[1]);
        if (flag[2])
            _tp.tunit[2] = Integer.parseInt(time_fin[2]);
    }

    /**
     * 设置当前时间相关的时间表达式
     */
    public void norm_setCurRelated() {
        String[] time_grid = new String[6];
        time_grid = normalizer.getOldTimeBase().split("-");
        int[] ini = new int[6];
        for (int i = 0; i < 6; i++)
            ini[i] = Integer.parseInt(time_grid[i]);

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(ini[0], ini[1] - 1, ini[2], ini[3], ini[4], ini[5]);
        calendar.getTime();

        boolean[] flag = {false, false, false};//观察时间表达式是否因当前相关时间表达式而改变时间

        String rule = "前年";
        Pattern pattern = Pattern.compile(rule);
        Matcher match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[0] = true;
            calendar.add(Calendar.YEAR, -2);
        }

        rule = "去年";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[0] = true;
            calendar.add(Calendar.YEAR, -1);
        }

        rule = "今年";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[0] = true;
            calendar.add(Calendar.YEAR, 0);
        }

        rule = "明年";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[0] = true;
            calendar.add(Calendar.YEAR, 1);
        }

        rule = "后年";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[0] = true;
            calendar.add(Calendar.YEAR, 2);
        }

        rule = "上(个)?月";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[1] = true;
            calendar.add(Calendar.MONTH, -1);

        }

        rule = "(本|这个)月";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[1] = true;
            calendar.add(Calendar.MONTH, 0);
        }

        rule = "下(个)?月";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[1] = true;
            calendar.add(Calendar.MONTH, 1);
        }

        rule = "大前天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            calendar.add(Calendar.DATE, -3);
        }

        rule = "(?<!大)前天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            calendar.add(Calendar.DATE, -2);
        }

        rule = "昨";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            calendar.add(Calendar.DATE, -1);
        }

        rule = "今(?!年)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            calendar.add(Calendar.DATE, 0);
        }

        rule = "明(?!年)";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            calendar.add(Calendar.DATE, 1);
        }

        rule = "(?<!大)后天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            calendar.add(Calendar.DATE, 2);
        }

        rule = "大后天";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            calendar.add(Calendar.DATE, 3);
        }

        rule = "(?<=(上上(周|星期)))[1-7]?";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7)
                week = 1;
            else
                week++;
            calendar.add(Calendar.WEEK_OF_MONTH, -2);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule = "(?<=((?<!上)上(周|星期)))[1-7]?";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7)
                week = 1;
            else
                week++;
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule = "(?<=((?<!下)下(周|星期)))[1-7]?";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7)
                week = 1;
            else
                week++;
            calendar.add(Calendar.WEEK_OF_MONTH, 1);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule = "(?<=(下下(周|星期)))[1-7]?";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7)
                week = 1;
            else
                week++;
            calendar.add(Calendar.WEEK_OF_MONTH, 2);
            calendar.set(Calendar.DAY_OF_WEEK, week);
        }

        rule = "(?<=((?<!(上|下))(周|星期)))[1-7]?";
        pattern = Pattern.compile(rule);
        match = pattern.matcher(Time_Expression);
        if (match.find()) {
            flag[2] = true;
            int week;
            try {
                week = Integer.parseInt(match.group());
            } catch (NumberFormatException e) {
                week = 1;
            }
            if (week == 7)
                week = 1;
            else
                week++;
            calendar.add(Calendar.WEEK_OF_MONTH, 0);
            calendar.set(Calendar.DAY_OF_WEEK, week);
            /**处理未来时间倾向 @author kexm*/
            preferFutureWeek(week, calendar);
        }

        String s = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(calendar.getTime());
        String[] time_fin = s.split("-");
        if (flag[0] || flag[1] || flag[2]) {
            _tp.tunit[0] = Integer.parseInt(time_fin[0]);
        }
        if (flag[1] || flag[2])
            _tp.tunit[1] = Integer.parseInt(time_fin[1]);
        if (flag[2])
            _tp.tunit[2] = Integer.parseInt(time_fin[2]);

    }

    /**
     * 该方法用于更新timeBase使之具有上下文关联性
     */
    public void modifyTimeBase() {
        String[] time_grid = new String[6];
        time_grid = normalizer.getTimeBase().split("-");

        String s = "";
        if (_tp.tunit[0] != -1)
            s += Integer.toString(_tp.tunit[0]);
        else
            s += time_grid[0];
        for (int i = 1; i < 6; i++) {
            s += "-";
            if (_tp.tunit[i] != -1)
                s += Integer.toString(_tp.tunit[i]);
            else
                s += time_grid[i];
        }
        normalizer.setTimeBase(s);
    }

    /**
     * 时间表达式规范化的入口
     * <p>
     * 时间表达式识别后，通过此入口进入规范化阶段，
     * 具体识别每个字段的值
     */
    public void Time_Normalization() {
        norm_setyear();
        norm_setmonth();
        norm_setday();
        norm_setmonth_fuzzyday();/**add by kexm*/
        norm_setBaseRelated();
        norm_setCurRelated();
        norm_sethour();
        norm_setminute();
        norm_setsecond();
        norm_setTotal();
        modifyTimeBase();

        _tp_origin.tunit = _tp.tunit.clone();

        String[] time_grid = new String[6];
        time_grid = normalizer.getTimeBase().split("-");

        int tunitpointer = 5;
        while (tunitpointer >= 0 && _tp.tunit[tunitpointer] < 0) {
            tunitpointer--;
        }
        for (int i = 0; i < tunitpointer; i++) {
            if (_tp.tunit[i] < 0)
                _tp.tunit[i] = Integer.parseInt(time_grid[i]);
        }
        String[] _result_tmp = new String[6];
        _result_tmp[0] = String.valueOf(_tp.tunit[0]);
        if (_tp.tunit[0] >= 10 && _tp.tunit[0] < 100) {
            _result_tmp[0] = "19" + String.valueOf(_tp.tunit[0]);
        }
        if (_tp.tunit[0] > 0 && _tp.tunit[0] < 10) {
            _result_tmp[0] = "200" + String.valueOf(_tp.tunit[0]);
        }

        for (int i = 1; i < 6; i++) {
            _result_tmp[i] = String.valueOf(_tp.tunit[i]);
        }

        Calendar cale = Calendar.getInstance();            //leverage a calendar object to figure out the final time
        cale.clear();
        if (Integer.parseInt(_result_tmp[0]) != -1) {
            Time_Norm += _result_tmp[0] + "年";
            cale.set(Calendar.YEAR, Integer.valueOf(_result_tmp[0]));
            if (Integer.parseInt(_result_tmp[1]) != -1) {
                Time_Norm += _result_tmp[1] + "月";
                cale.set(Calendar.MONTH, Integer.valueOf(_result_tmp[1]) - 1);
                if (Integer.parseInt(_result_tmp[2]) != -1) {
                    Time_Norm += _result_tmp[2] + "日";
                    cale.set(Calendar.DAY_OF_MONTH, Integer.valueOf(_result_tmp[2]));
                    if (Integer.parseInt(_result_tmp[3]) != -1) {
                        Time_Norm += _result_tmp[3] + "时";
                        cale.set(Calendar.HOUR_OF_DAY, Integer.valueOf(_result_tmp[3]));
                        if (Integer.parseInt(_result_tmp[4]) != -1) {
                            Time_Norm += _result_tmp[4] + "分";
                            cale.set(Calendar.MINUTE, Integer.valueOf(_result_tmp[4]));
                            if (Integer.parseInt(_result_tmp[5]) != -1) {
                                Time_Norm += _result_tmp[5] + "秒";
                                cale.set(Calendar.SECOND, Integer.valueOf(_result_tmp[5]));
                            }
                        }
                    }
                }
            }
        }
        time = cale.getTime();

        time_full = _tp.tunit.clone();
//		time_origin = _tp_origin.tunit.clone(); comment by kexm
    }

    public Boolean getIsAllDayTime() {
        return isAllDayTime;
    }

    public void setIsAllDayTime(Boolean isAllDayTime) {
        this.isAllDayTime = isAllDayTime;
    }

    public String toString() {
        return Time_Expression + " ---> " + Time_Norm;
    }

    /**
     * 如果用户选项是倾向于未来时间，检查checkTimeIndex所指的时间是否是过去的时间，如果是的话，将大一级的时间设为当前时间的+1。
     * <p>
     * 如在晚上说“早上8点看书”，则识别为明天早上;
     * 12月31日说“3号买菜”，则识别为明年1月的3号。
     *
     * @param checkTimeIndex _tp.tunit时间数组的下标
     */
    private void preferFuture(int checkTimeIndex) {
        /**1. 检查被检查的时间级别之前，是否没有更高级的已经确定的时间，如果有，则不进行处理.*/
        for (int i = 0; i < checkTimeIndex; i++) {
            if (_tp.tunit[i] != -1) return;
        }
        /**2. 根据上下文补充时间*/
        checkContextTime(checkTimeIndex);
        /**3. 根据上下文补充时间后再次检查被检查的时间级别之前，是否没有更高级的已经确定的时间，如果有，则不进行倾向处理.*/
        for (int i = 0; i < checkTimeIndex; i++) {
            if (_tp.tunit[i] != -1) return;
        }
        /**4. 确认用户选项*/
        if (!normalizer.isPreferFuture()) {
            return;
        }
        /**5. 获取当前时间，如果识别到的时间小于当前时间，则将其上的所有级别时间设置为当前时间，并且其上一级的时间步长+1*/
        Calendar c = Calendar.getInstance();
        if (this.normalizer.getTimeBase() != null) {
            String[] ini = this.normalizer.getTimeBase().split("-");
            c.set(Integer.valueOf(ini[0]).intValue(), Integer.valueOf(ini[1]).intValue() - 1, Integer.valueOf(ini[2]).intValue()
                    , Integer.valueOf(ini[3]).intValue(), Integer.valueOf(ini[4]).intValue(), Integer.valueOf(ini[5]).intValue());
//            LOGGER.debug(DateUtil.formatDateDefault(c.getTime()));
        }

        int curTime = c.get(TUNIT_MAP.get(checkTimeIndex));
        if (curTime < _tp.tunit[checkTimeIndex]) {
            return;
        }
        //准备增加的时间单位是被检查的时间的上一级，将上一级时间+1
        int addTimeUnit = TUNIT_MAP.get(checkTimeIndex - 1);
        c.add(addTimeUnit, 1);

//		_tp.tunit[checkTimeIndex - 1] = c.get(TUNIT_MAP.get(checkTimeIndex - 1));
        for (int i = 0; i < checkTimeIndex; i++) {
            _tp.tunit[i] = c.get(TUNIT_MAP.get(i));
            if (TUNIT_MAP.get(i) == Calendar.MONTH) {
                ++_tp.tunit[i];
            }
        }

    }

    /**
     * 如果用户选项是倾向于未来时间，检查所指的day_of_week是否是过去的时间，如果是的话，设为下周。
     * <p>
     * 如在周五说：周一开会，识别为下周一开会
     *
     * @param weekday 识别出是周几（范围1-7）
     */
    private void preferFutureWeek(int weekday, Calendar c) {
        /**1. 确认用户选项*/
        if (!normalizer.isPreferFuture()) {
            return;
        }
        /**2. 检查被检查的时间级别之前，是否没有更高级的已经确定的时间，如果有，则不进行倾向处理.*/
        int checkTimeIndex = 2;
        for (int i = 0; i < checkTimeIndex; i++) {
            if (_tp.tunit[i] != -1) return;
        }
        /**获取当前是在周几，如果识别到的时间小于当前时间，则识别时间为下一周*/
        Calendar curC = Calendar.getInstance();
        if (this.normalizer.getTimeBase() != null) {
            String[] ini = this.normalizer.getTimeBase().split("-");
            curC.set(Integer.valueOf(ini[0]).intValue(), Integer.valueOf(ini[1]).intValue() - 1, Integer.valueOf(ini[2]).intValue()
                    , Integer.valueOf(ini[3]).intValue(), Integer.valueOf(ini[4]).intValue(), Integer.valueOf(ini[5]).intValue());
        }
        int curWeekday = curC.get(Calendar.DAY_OF_WEEK);
        if (weekday == 1) {
            weekday = 7;
        }
        if (curWeekday < weekday) {
            return;
        }
        //准备增加的时间单位是被检查的时间的上一级，将上一级时间+1
        c.add(Calendar.WEEK_OF_YEAR, 1);
    }

    /**
     * 根据上下文时间补充时间信息
     */
    private void checkContextTime(int checkTimeIndex) {
        for (int i = 0; i < checkTimeIndex; i++) {
            if (_tp.tunit[i] == -1 && _tp_origin.tunit[i] != -1) {
                _tp.tunit[i] = _tp_origin.tunit[i];
            }
        }
        /**在处理小时这个级别时，如果上文时间是下午的且下文没有主动声明小时级别以上的时间，则也把下文时间设为下午*/
        if (isFirstTimeSolveContext == true && checkTimeIndex == 3 && _tp_origin.tunit[checkTimeIndex] >= 12 && _tp.tunit[checkTimeIndex] < 12) {
            _tp.tunit[checkTimeIndex] += 12;
        }
        isFirstTimeSolveContext = false;
    }

    private static Map<Integer, Integer> TUNIT_MAP = new HashMap<>();

    static {
        TUNIT_MAP.put(0, Calendar.YEAR);
        TUNIT_MAP.put(1, Calendar.MONTH);
        TUNIT_MAP.put(2, Calendar.DAY_OF_MONTH);
        TUNIT_MAP.put(3, Calendar.HOUR_OF_DAY);
        TUNIT_MAP.put(4, Calendar.MINUTE);
        TUNIT_MAP.put(5, Calendar.SECOND);
    }
}
