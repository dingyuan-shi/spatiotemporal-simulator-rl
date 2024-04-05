package org.sdy;
import com.alibaba.fastjson.JSON;

public class Test {

    public static void main() {
        System.out.println(JSON.parseObject("[{\"abc:123\"}]"));

    }
    
}
