package com.ylli.api.example.model;

import com.ylli.api.common.uid.RandomLongGenerator;
import io.mybatis.provider.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity.Table("t_example_item")
@Data
@NoArgsConstructor
public class ExampleItem {

    @Entity.Column(id = true, updatable = false, genId = RandomLongGenerator.class)
    public Long id;

    public Long exampleId;

    public Timestamp createTime;

    public Timestamp updateTime;

    public ExampleItem(Long exampleId) {
        this.exampleId = exampleId;
    }
}
