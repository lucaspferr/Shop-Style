package com.MS.history.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user")
public class User {

//    @Transient
//    public static final String SEQUENCE_NAME = "user_sequence";

    @Id
    private Long user_id;

    private String firstName;
    private String lastName;
    private String sex;
    private String cpf;
    private String birthdate;
    private String email;

    private Long history_id;

}
