package org.damazio.lockstrength;

import java.util.List;

import org.damazio.lockstrength.LockPatternView.Cell;

import android.util.FloatMath;
import android.util.Log;

/**
 * Class which estimates the strength of any given pattern.
 *
 * @author rdamazio
 */
public class LockStrengthMeter {
 
  /**
   * Enum with possible strength values.
   */
  public enum Strength {
    WEAK,
    AVERAGE,
    GOOD,
    EXCELLENT;
  }

  private static final String LOG_TAG = "LockStrength";

  private static final float MAX_WEAK_STRENGTH = 10.0f;
  private static final float MAX_AVG_STRENGTH = 15.0f;
  private static final float MAX_GOOD_STRENGTH = 50.0f;
  private static final float SCORE_PER_DISTANCE_UNIT = 0.5f;
  private static final float CROSSING_SCORE = 5.0f;
  private static final float ANGLE_SCORES[][] = new float[][] {
      { 0, 0, 2 },
      { 0, 2, 8 },
      { 2, 8, 4 }
  };
  private static final float ANGLE_CHANGE_SCORE_0 = 0;
  private static final float ANGLE_CHANGE_SCORE_45 = 2.0f;
  private static final float ANGLE_CHANGE_SCORE_90 = 1.0f;
  private static final float ANGLE_CHANGE_SCORE_OTHER = 4.0f;

  /**
   * Calculates and returns the strength of the given pattern.
   */
  public Strength calculateStrength(List<Cell> pattern) {
    float strength = calculateNumericStrength(pattern);
    if (strength <= MAX_WEAK_STRENGTH) {
      return Strength.WEAK;
    } else if (strength <= MAX_AVG_STRENGTH) {
      return Strength.AVERAGE;
    } else if (strength <= MAX_GOOD_STRENGTH) {
      return Strength.GOOD;
    } else {
      return Strength.EXCELLENT;
    }
  }

  /**
   * Calculates and returns a numerical strength value for the given pattern.
   * The number returned is an arbitrary strength measurement and should not
   * be assumed to be in any scale.
   */
  private float calculateNumericStrength(List<Cell> pattern) {
    int numCrossings = 0;
    float distanceScore = 0.0f,
          angleScore = 0.0f,
          angleChangeScore = 0.0f,
          crossingScore = 0.0f;

    for (int i = 1; i < pattern.size(); i++) {
      Cell cell = pattern.get(i);
      Cell previousCell = pattern.get(i - 1);
      int x1 = previousCell.getColumn();
      int y1 = previousCell.getRow();
      int x2 = cell.getColumn();
      int y2 = cell.getRow();

      // Score the distance
      int diffX = x2 - x1;
      int diffY = y2 - y1;
      float distance = FloatMath.sqrt(diffX * diffX + diffY * diffY);
      distanceScore += distance * SCORE_PER_DISTANCE_UNIT;

      // Score the angle of each segment
      int dx = Math.abs(x2 - x1);
      int dy = Math.abs(y2 - y1);
      angleScore += ANGLE_SCORES[dx][dy];

      // Score the change of angle
      if (i < pattern.size() - 1) {
        Cell nextCell = pattern.get(i + 1);
        int x3 = nextCell.getColumn();
        int y3 = nextCell.getRow();
        int diffX2 = x3 - x2;
        int diffY2 = y3 - y2;
        float distance2 = FloatMath.sqrt(diffX2 * diffX2 + diffY2 * diffY2);

        int dotProduct = (diffX * diffX2) + (diffY * diffY2);
        float cos = dotProduct / (distance * distance2);
        int angle = (int) Math.round(Math.acos(cos) * 180.0f / Math.PI);
        while (angle > 90) angle -= 90;
        Logv("Angle change: " + angle);

        switch (angle) {
          case 0:
            angleChangeScore += ANGLE_CHANGE_SCORE_0;
            break;
          case 45:
            angleChangeScore += ANGLE_CHANGE_SCORE_45;
            break;
          case 90:
            angleChangeScore += ANGLE_CHANGE_SCORE_90;
            break;
          default:
            angleChangeScore += ANGLE_CHANGE_SCORE_OTHER;
            break;
        }
      }

      // Score the line crossings (quadratic complexity, but the input is always small)
      for (int j = 1; j < i; j++) {
        Cell secondSegment1 = pattern.get(j);
        Cell secondSegment2 = pattern.get(j - 1);
        if (linesCross(cell, previousCell, secondSegment1, secondSegment2)) {
          Logv("Intersection: " + cell + "," + previousCell + "; " + secondSegment1 + "," + secondSegment2);
          crossingScore += CROSSING_SCORE;
          numCrossings++;
        }
      }
    }
    Logv(" Distance: " + distanceScore);
    Logv("    Angle: " + angleScore);
    Logv("  AngDiff: " + angleChangeScore);
    Logv(" Crossing: " + crossingScore + "(" + numCrossings + " crossings)");

    float score = distanceScore + angleScore + angleChangeScore + crossingScore;
    Log.d(LOG_TAG, "*Strength: " + score);
    return score;
  }

  /**
   * Checks whether the two line segments formed by the given points cross.
   * Crossing at the extremity (same endpoint) is not considered.
   *
   * @param firstSegment1 the first point of the first segment
   * @param firstSegment2 the second point of the first segment
   * @param secondSegment1 the first point of the second segment
   * @param secondSegment2 the second point of the second segment
   * @return true if they cross, false otherwise
   */
  private boolean linesCross(Cell firstSegment1, Cell firstSegment2,
                             Cell secondSegment1, Cell secondSegment2) {
    return linesCross(firstSegment1.getColumn(), firstSegment1.getRow(),
                      firstSegment2.getColumn(), firstSegment2.getRow(),
                      secondSegment1.getColumn(), secondSegment1.getRow(),
                      secondSegment2.getColumn(), secondSegment2.getRow());
  }

  /**
   * Checks and returns whether the two line segments with the given points cross.
   */
  private boolean linesCross(int x11, int y11, int x12, int y12,
                             int x21, int y21, int x22, int y22) {
    Logv("===== cross (" + x11 + "," + y11 + ")-(" + x12 + "," + y12 + "),("
                         + x21 + "," + y21 + ")-(" + x22 + "," + y22 + ") =====");

    int diffX1 = x12 - x11;
    int diffY1 = y12 - y11;
    int diffX2 = x22 - x21;
    int diffY2 = y22 - y21;

    // Check if either line is vertical
    if (diffX1 == 0 || diffX2 == 0) {
      if (diffX1 == diffX2) {
        // Both are vertical, intersection if bounding box intersects on Y
        // and they have the same X
        Logv("both vertical");
        return (x11 == x21) && boundsIntersect(y11, y12, y21, y22);
      }

      // Perpendicular, no intersection possible
      // (can't have both diffX and diffY == 0 for the same line)
      if (diffY1 == 0 || diffY2 == 0) {
        Logv("perpendicular");
        return false;
      }

      // Only one of them is axis-aligned, solve for flipped coordinates
      Logv("flipping");
      return linesCross(y11, x11, y12, x12, y21, x21, y22, x22);
    }

    // y = slope * x + a
    float slope1 = (float) diffY1 / (float) diffX1;
    float slope2 = (float) diffY2 / (float) diffX2;
    float a1 = y11 - slope1 * x11;
    float a2 = y21 - slope2 * x21;
    Logv("y1=" + slope1 + "x + " + a1);
    Logv("y2=" + slope2 + "x + " + a2);

    if (slope1 == slope2) {
      // Either parallel or colinear

      if (a1 != a2) {
        // Parallel
        Logv("parallel");
        return false;
      }

      // Colinear, intersection if bounding box intersects on either axis
      Logv("colinear");
      return boundsIntersect(y11, y12, y21, y22) ||
             boundsIntersect(x11, x12, x21, x22);
    }

    float interX = (a2 - a1) / (slope1 - slope2);

    int maxX1 = Math.max(x11, x12);
    int maxX2 = Math.max(x21, x22);
    int minX1 = Math.min(x11, x12);
    int minX2 = Math.min(x21, x22);

    // Check that the intersection is within both segments
    Logv("else, interx=" + interX);
    return (inRange(minX1, maxX1, interX) &&
            inRange(minX2, maxX2, interX));
  }

  /**
   * Checks and returns whether there's a positive-size intersection of the two
   * given ranges.
   *
   * @param v11 the first value of the first range
   * @param v12 the second value of the first range
   * @param v21 the first value of the second range
   * @param v22 the second value of the second range
   * @return true if they intersect, false otherwise
   */
  private boolean boundsIntersect(int v11, int v12, int v21, int v22) {
    int max1 = Math.max(v11, v12);
    int max2 = Math.max(v21, v22);
    int min1 = Math.min(v11, v12);
    int min2 = Math.min(v21, v22);

    int minMax = Math.min(max1, max2);
    int maxMin = Math.max(min1, min2);
    Logv("minMax=" + minMax + "; maxMin=" + maxMin);
    return (minMax > maxMin);
  }

  /**
   * Checks and returns whether val is between min and max.
   */
  private boolean inRange(int min, int max, float val) {
    return (val > min && val < max);
  }

  private void Logv(String msg) {
//    Log.v(LOG_TAG, msg);
  }
}
