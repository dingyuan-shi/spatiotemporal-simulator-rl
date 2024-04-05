package org.sdy;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class JarEnv {
    private int cnt;
    
    public JarEnv(int cnt) {
        this.cnt = cnt;
    }

    // public int step(int decision) {
    //     cnt += decision;
    //     return cnt;
    // }

    // public String step(String decision) {
    //     return decision + "###";
    // }

    public void step(Object decision) {

        System.out.println(decision.getClass().toString());
        // Map<String, Integer> res = new HashMap<>();
        // for (String k: decision.keySet()) {
        //     res.put(k, decision.get(k) + 10);
        //     System.out.print(k + ": " + decision.get(k));
        // }
        // return res;
    }

}
