/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.codeStyle.autodetect;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

public class DetectedIndentNotificationTest extends LightPlatformCodeInsightFixtureTestCase {

  static {
    PlatformTestCase.initPlatformLangPrefix();
  }

  private CodeStyleSettings mySettings;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mySettings = CodeStyleSettingsManager.getInstance(getProject()).getCurrentSettings();
    mySettings.AUTODETECT_INDENTS = true;
    DetectableIndentOptionsProvider optionsProvider = DetectableIndentOptionsProvider.getInstance();
    if (optionsProvider != null) {
      optionsProvider.setEnabledInTest(true);
    }
  }

  @Override
  public void tearDown() throws Exception {
    mySettings.AUTODETECT_INDENTS = false;
    DetectableIndentOptionsProvider optionsProvider = DetectableIndentOptionsProvider.getInstance();
    if (optionsProvider != null) {
      optionsProvider.setEnabledInTest(false);
    }
    super.tearDown();
  }

  public void testNotificationShownOnEnter_WhenIndentDetected() throws Exception {
    myFixture.configureByText("Test.java",
                              "class Test {\n" +
                              "  public void main() {\n" +
                              "    int a;<caret>\n" +
                              "    int b;\n" +
                              "  }\n" +
                              "}");

    PsiFile file = myFixture.getFile();
    VirtualFile vFile = file.getVirtualFile();

    try {
      vFile.putUserData(DetectedIndentOptionsNotificationProvider.SHOW_NOTIFICATION_IN_TEST, Boolean.TRUE);
      assert !isNotificationShown(vFile);
      myFixture.type('\n');
      assert isNotificationShown(vFile);
    }
    finally {
      vFile.putUserData(DetectedIndentOptionsNotificationProvider.SHOW_NOTIFICATION_IN_TEST, null);
    }
  }

  public void testNoNotification_WhenNothingDetected() throws Exception {
    myFixture.configureByText("Test.java",
                              "class Test {\n" +
                              "    public void main() {\n" +
                              "        int a;<caret>\n" +
                              "        int b;\n" +
                              "    }\n" +
                              "}");

    PsiFile file = myFixture.getFile();
    VirtualFile vFile = file.getVirtualFile();

    try {
      vFile.putUserData(DetectedIndentOptionsNotificationProvider.SHOW_NOTIFICATION_IN_TEST, Boolean.TRUE);
      assert !isNotificationShown(vFile);
      myFixture.type('\n');
      assert !isNotificationShown(vFile);
    }
    finally {
      vFile.putUserData(DetectedIndentOptionsNotificationProvider.SHOW_NOTIFICATION_IN_TEST, null);
    }
  }

  private boolean isNotificationShown(@NotNull VirtualFile vFile) {
    return vFile.getUserData(DetectedIndentOptionsNotificationProvider.DETECT_INDENT_NOTIFICATION_SHOWN_KEY) != null;
  }
}
