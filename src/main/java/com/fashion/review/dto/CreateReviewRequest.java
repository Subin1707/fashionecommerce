package com.fashion.review.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {
    private Integer rating;
    private String comment;
    private Long orderItemId;
    private java.util.List<String> media;

    public CreateReviewRequest(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public CreateReviewRequest() {
    }
    public CreateReviewRequest(final Integer rating, final String comment, final Long orderItemId, final java.util.List<String> media) {
        this.rating = rating;
        this.comment = comment;
        this.orderItemId = orderItemId;
        this.media = media;
    }
}
