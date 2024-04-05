package Solvers.GMAPF.Graph;

import org.junit.Test;

public class SegmentIndexTest {
    @Test
    public void testIntersection() {
        Segment s1 = new Segment(0, 1, 3, 4);
        Segment s2 = new Segment(0, 2, 1, 3);
        Segment s3 = new Segment(2, 2, 3, 3);
        Segment s4 = new Segment(0, 5, 3, 2);
        Segment s5 = new Segment(0, 5, 0, 5);
        Segment s6 = new Segment(1, 5, 1, 5);
        Segment s7 = new Segment(4, 4, 6, 2);
        Segment[] segments = {s1, s2, s3, s4, s5, s6, s7};
        boolean[][] judgeMatrix = new boolean[segments.length][segments.length];
        for (int i = 0; i < segments.length; ++i) {
            for (int j = 0; j < segments.length; ++j) {
                judgeMatrix[i][j] = segments[i].intersection(segments[j]);
            }
        }
        for (int i = 0; i < segments.length; ++i) {
            for (int j = 0; j < segments.length; ++j) {
                System.out.print(judgeMatrix[i][j]?"1 ":"0 ");
            }
            System.out.println();
        }
    }
}

