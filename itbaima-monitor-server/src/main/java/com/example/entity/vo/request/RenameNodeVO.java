package com.example.entity.vo.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class RenameNodeVO {
    private int id;
    @Length(min = 1,max = 10)
    private String node;
    @Pattern(regexp = "(cn|jp|kr|us|de|sg|hk)")
    private String location;
}
