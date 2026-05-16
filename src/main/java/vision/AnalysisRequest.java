package vision;

import java.nio.file.Path;


class AnalysisRequest {
    private final String kind;
    private final String value;

    private AnalysisRequest(String kind, String value) {
      this.kind = kind;
      this.value = value;
    }

    static AnalysisRequest forUrl(String url) {
      return new AnalysisRequest("url", url);
    }

    static AnalysisRequest forFile(Path file) {
      return new AnalysisRequest("file", file.toString());
    }

    boolean isUrl() {
      return "url".equals(kind);
    }

    String value() {
      return value;
    }

    String describe() {
      return kind + ":" + value;
    }
  }