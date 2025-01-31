package com.collabit.community.domain.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GetCommentResponseDTO {
    int code;
    int postCode;
    String userNickname;
    String content;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    int parentCommentCode;
    boolean isDeleted;
}
