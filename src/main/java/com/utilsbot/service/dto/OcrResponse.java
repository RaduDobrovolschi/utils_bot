package com.utilsbot.service.dto;

import java.util.List;

public record OcrResponse(List<ParsedResultItem> ParsedResults, String OCRExitCode,
                          boolean IsErroredOnProcessing, String ErrorMessage, String ErrorDetails,
                          String SearchablePDFURL, long ProcessingTimeInMilliseconds) {

    public record ParsedResultItem(TextOverlay TextOverlay, String FileParseExitCode, String ParsedText,
                                   String ErrorMessage, String ErrorDetails, String TextOrientation) {

        public record TextOverlay(List<TextOverlayLine> Lines, boolean HasOverlay, String Message) {

            public record TextOverlayLine(List<TextOverlayWord> Words, int MaxHeight, int MinTop) {

                public record TextOverlayWord(String WordText, int Left, int Top, int Height, int Width) {
                }
            }
        }
    }
}