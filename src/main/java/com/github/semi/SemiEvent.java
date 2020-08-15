package com.github.semi;

import com.intellij.openapi.editor.Editor;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName EnterEvent
 * @description
 * @date 2020/5/10 15:24
 * @since JDK 1.8
 */
public class SemiEvent {

    CharSequence currentLine;

    CharSequence totalContent;

    int caretIndex;

    int currentLineNum;

    int currentLineStart;

    int currentLineEnd;

    int currentLineStartWsEndOffset;

    public SemiEvent(CharSequence currentLine) {
        this.currentLine = currentLine;
    }

    public CharSequence getCurrentLine() {
        return currentLine;
    }

    public void setCurrentLine(CharSequence currentLine) {
        this.currentLine = currentLine;
    }

    public CharSequence getTotalContent() {
        return totalContent;
    }

    public void setTotalContent(CharSequence totalContent) {
        this.totalContent = totalContent;
    }

    public int getCaretIndex() {
        return caretIndex;
    }

    public void setCaretIndex(int caretIndex) {
        this.caretIndex = caretIndex;
    }

    public int getCurrentLineNum() {
        return currentLineNum;
    }

    public void setCurrentLineNum(int currentLineNum) {
        this.currentLineNum = currentLineNum;
    }

    public int getCurrentLineStart() {
        return currentLineStart;
    }

    public void setCurrentLineStart(int currentLineStart) {
        this.currentLineStart = currentLineStart;
    }

    public int getCurrentLineEnd() {
        return currentLineEnd;
    }

    public void setCurrentLineEnd(int currentLineEnd) {
        this.currentLineEnd = currentLineEnd;
    }

    public int getCurrentLineStartWsEndOffset() {
        return currentLineStartWsEndOffset;
    }

    public void setCurrentLineStartWsEndOffset(int currentLineStartWsEndOffset) {
        this.currentLineStartWsEndOffset = currentLineStartWsEndOffset;
    }
}
