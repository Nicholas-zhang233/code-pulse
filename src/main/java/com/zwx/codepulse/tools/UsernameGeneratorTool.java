package com.zwx.codepulse.tools;

import cn.hutool.core.util.RandomUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 随机中文用户名生成器
 * 提供两种风格：昵称风格（形容词+名词+数字）、人名风格（姓氏+名字+数字）
 * 并支持唯一性校验与重试
 */
public class UsernameGeneratorTool {

    /* ========== 词库，可按需扩充 ========== */

    // 形容词词库（昵称风格）
    private static final String[] ADJECTIVES = {
            "快乐", "勇敢", "聪明", "安静", "温暖",
            "自由", "清新", "明亮", "温柔", "潇洒",
            "可爱", "活泼", "沉稳", "优雅", "阳光",
            "神秘", "浪漫", "淡定", "从容", "灵动"
    };

    // 名词词库（昵称风格）
    private static final String[] NOUNS = {
            "小猫", "海豚", "星辰", "森林", "微风",
            "山谷", "海洋", "月光", "飞鸟", "溪流",
            "云朵", "彩虹", "极光", "流星", "鲸鱼",
            "松鼠", "蝴蝶", "萤火", "竹林", "雪松"
    };

    // 姓氏词库（人名风格）
    private static final String[] SURNAMES = {
            "李", "王", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗"
    };

    // 名字常用字（人名风格）
    private static final String NAME_CHARS =
            "子涵雨欣梓萱宇轩浩然思远佳怡俊杰雅静诗琪明辉嘉懿";

    /* ========== 生成方法 ========== */

    /**
     * 生成昵称风格的用户名：形容词 + 名词 + 4位随机数字
     * 例如：温暖海豚5831
     */
    public static String generateNicknameStyle() {
        String adjective = RandomUtil.randomEle(ADJECTIVES);
        String noun = RandomUtil.randomEle(NOUNS);
        String suffix = RandomUtil.randomNumbers(4);
        return adjective + noun + suffix;
    }

    /**
     * 生成人名风格的用户名：姓氏 + 名字两字 + 4位随机数字
     * 例如：李梓涵7291
     */
    public static String generateRealNameStyle() {
        String surname = RandomUtil.randomEle(SURNAMES);
        String givenName = RandomUtil.randomString(NAME_CHARS, 2);
        String suffix = RandomUtil.randomNumbers(4);
        return surname + givenName + suffix;
    }

    /**
     * 生成唯一用户名（默认使用昵称风格）
     * 最多重试 10 次，若仍冲突则使用时间戳兜底
     *
     * @param existsChecker 检查用户名是否已存在的函数，返回 true 表示已存在
     * @return 唯一的用户名
     */
    public static String generateUniqueUsername(Predicate<String> existsChecker) {
        // 尝试 10 次生成不冲突的用户名
        for (int i = 0; i < 10; i++) {
            String username = generateNicknameStyle();
            if (!existsChecker.test(username)) {
                return username;
            }
        }
        // 兜底方案：时间戳 + 随机数，几乎不可能重复
        String fallback = "用户" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
        // 理论上还应再检查一次，但为了简洁，直接返回。极端情况下可再加循环或抛异常。
        return fallback;
    }

    /**
     * 生成唯一用户名（可指定生成风格）
     *
     * @param style         生成风格：true=人名风格，false=昵称风格
     * @param existsChecker 检查用户名是否已存在的函数
     * @return 唯一用户名
     */
    public static String generateUniqueUsername(boolean style, Predicate<String> existsChecker) {
        for (int i = 0; i < 10; i++) {
            String username = style ? generateRealNameStyle() : generateNicknameStyle();
            if (!existsChecker.test(username)) {
                return username;
            }
        }
        return "用户" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }

    /* ========== 测试入口 ========== */

    public static void main(String[] args) {
        // 模拟数据库中已存在的用户名集合
        Set<String> existingUsernames = new HashSet<>();
        existingUsernames.add("温暖海豚5831"); // 故意添加一个，测试重试逻辑

        // 使用 Predicate 模拟数据库查询
        Predicate<String> existsChecker = existingUsernames::contains;

        // 生成唯一用户名
        String uniqueUsername = generateUniqueUsername(existsChecker);
        System.out.println("生成的唯一用户名: " + uniqueUsername);

        // 直接生成随机用户名（不检查唯一性）
        System.out.println("随机昵称风格: " + generateNicknameStyle());
        System.out.println("随机人名风格: " + generateRealNameStyle());

        // 生成人名风格的唯一用户名
        String uniqueRealName = generateUniqueUsername(true, existsChecker);
        System.out.println("唯一人名风格: " + uniqueRealName);
    }
}