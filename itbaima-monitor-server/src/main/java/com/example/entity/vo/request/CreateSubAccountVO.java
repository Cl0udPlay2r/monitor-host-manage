package com.example.entity.vo.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class CreateSubAccountVO {
    @Length(min=1,max=20)
    private String username;
    @Length(min=6,max=18)
    private String password;
    private String email;
    private List<Integer> clients;
}
