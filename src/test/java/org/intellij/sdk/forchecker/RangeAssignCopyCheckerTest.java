package org.intellij.sdk.forchecker;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.refactoring.suggested.LightJavaCodeInsightFixtureTestCaseWithUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

public class RangeAssignCopyCheckerTest extends LightJavaCodeInsightFixtureTestCaseWithUtils {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBuildGoVisitor_handlesEmptyCode() {
        myFixture.configureByFile("rangeassign.empty.go");
        myFixture.enableInspections(new RangeAssignCopyChecker());
        List<HighlightInfo> highlightInfos = myFixture.doHighlighting();
        assertEmpty(filterRangeAssignCopyCheckerHighlights(highlightInfos));
    }

    public void testBuildGoVisitor_raisesPointerUsageHighlights() {
        myFixture.configureByFile("rangeassign.reference.go");
        myFixture.enableInspections(new RangeAssignCopyChecker());
        List<HighlightInfo> highlightInfos = myFixture.doHighlighting();
        assertNotEmpty(highlightInfos);

        List<HighlightInfo> passingReferenceHighlights = filterRangeAssignCopyCheckerHighlights(highlightInfos);
        assertNotEmpty(passingReferenceHighlights);
        assertSize(2, passingReferenceHighlights);
        HighlightInfo first = passingReferenceHighlights.get(0);
        assertEqualOffset(153, 156, first);
        HighlightInfo second = passingReferenceHighlights.get(1);
        assertEqualOffset(186, 189, second);
    }

    public void testBuildGoVisitor_raisesEditingReceiverHighlights() {
        myFixture.configureByFiles("rangeassign.receiver.go", "editable.go");
        myFixture.enableInspections(new RangeAssignCopyChecker());
        List<HighlightInfo> highlightInfos = myFixture.doHighlighting();
        assertNotEmpty(highlightInfos);

        List<HighlightInfo> passingReferenceHighlights = filterRangeAssignCopyCheckerHighlights(highlightInfos);
        assertNotEmpty(passingReferenceHighlights);
        assertSize(2, passingReferenceHighlights);
        HighlightInfo first = passingReferenceHighlights.get(0);
        assertEqualOffset(157, 160, first);
        HighlightInfo second = passingReferenceHighlights.get(1);
        assertEqualOffset(179, 182, second);
    }

    private void assertEqualOffset(int startOffset, int endOffset, HighlightInfo highlightInfo) {
        assertEquals(startOffset, highlightInfo.getStartOffset());
        assertEquals(endOffset, highlightInfo.getEndOffset());
    }

    private @NotNull List<HighlightInfo> filterRangeAssignCopyCheckerHighlights(List<HighlightInfo> highlightInfos) {
        ArrayList<HighlightInfo> retObjects = new ArrayList<>();
        for (HighlightInfo highlightInfo : highlightInfos) {
            if (highlightInfo.getInspectionToolId() != null && highlightInfo.getInspectionToolId().contentEquals("RangeAssignCopyChecker")) {
                retObjects.add(highlightInfo);
            }
        }
        return retObjects;
    }
}