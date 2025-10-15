package com.blogapp.dtos;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PostParamFilterDto {
  private List<String> authorNames;
  private List<Long> tagIds;
  private String search;
  private String oldSearch;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate fromDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate toDate;

  private int page;
  private int size;
  private String sort;
  private String direction;
}
