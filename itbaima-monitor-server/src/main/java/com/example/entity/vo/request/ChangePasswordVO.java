package com.example.entity.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Valid
public class ChangePasswordVO {
    @Length(min = 6,max=18)
    private String old_password;
    @Length(min=6,max=18)
    private String new_password;
}
