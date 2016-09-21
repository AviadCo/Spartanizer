package il.org.spartan.spartanizer.wrings;

import static il.org.spartan.spartanizer.wrings.TrimmerTestsUtils.*;

import org.junit.*;

/**  
* @author TODO: ALex? Dan?
 * @year 2016 */
public class Issue239Test {
  @Test public void a$01() {
    trimmingOf("private void testInteger(final boolean testTransients) {\n" + "final Integer i1 = Integer.valueOf(12345);\n"
        + "final Integer i2 = Integer.valueOf(12345);\n" + "assertEqualsAndHashCodeContract(i1, i2, testTransients);\n" + "}")
            .gives("private void testInteger(final boolean testTransients) {\n"
                + "assertEqualsAndHashCodeContract(Integer.valueOf(12345), Integer.valueOf(12345), testTransients);\n" + "}")
            .stays();
  }

  @Test public void a$02() {
    trimmingOf("private void testInteger(final boolean testTransients) {\n" + "final Integer i1 = Integer.valueOf(12345);\n"
        + "final Integer i2 = Integer.valueOf(12345);\n" + "assertEqualsAndHashCodeContract(i1, i2, testTransients);\n" + "}").stays();
  }
}
