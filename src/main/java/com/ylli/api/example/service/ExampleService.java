package com.ylli.api.example.service;

import com.google.gson.Gson;
import com.ylli.api.common.exception.GenericException;
import com.ylli.api.example.mapper.ExampleMapper;
import com.ylli.api.example.model.ExampleInfo;
import com.ylli.api.example.model.ExampleModel;
import io.mybatis.mapper.example.ExampleWrapper;
import io.mybatis.mapper.fn.Fn;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExampleService {

    ExampleMapper exampleMapper;

    ExampleService(ExampleMapper exampleMapper) {
        this.exampleMapper = exampleMapper;
    }

    /**
     * insert          插入all fields
     * insertSelective 插入给定字段，其余使用mysql default
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(ExampleModel model) {
        if (exampleMapper.wrapper().eq(ExampleModel::getUsername, model.username).count() != 0) {
            throw new GenericException(HttpStatus.BAD_REQUEST, "username already exists");
        }
        exampleMapper.insertSelective(model);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        exampleMapper.deleteByPrimaryKey(id);
    }

    /*
     * mysql 多值索引
     * https://dev.mysql.com/doc/refman/8.0/en/json-search-functions.html#function_json-overlaps
     * https://dev.mysql.com/doc/refman/8.0/en/create-index.html#create-index-multi-valued
     */
    public List<ExampleModel> get(Long id, String username, Long version, Boolean status, List<ExampleInfo> extras,
                                  Timestamp leftTime, Timestamp rightTime, Integer offset, Integer limit) {
        ExampleWrapper<ExampleModel, Long> exampleWrapper = exampleMapper.wrapper();
        if (extras != null) {
            //	JSON_OVERLAPS (extras -> '$[*].serialNo',CAST( '["342501199310231774"]' AS JSON ));
            exampleWrapper.anyCondition("JSON_OVERLAPS (extras ->> '$[*].serialNo', CAST( '" + new Gson().toJson(extras.stream().map(info -> info.serialNo).collect(Collectors.toList())) + "' AS JSON))");
        }
        if (id != null) {
            exampleWrapper.eq(ExampleModel::getId, id);
        }
        if (username != null) {
            exampleWrapper.startsWith(ExampleModel::getUsername, username);
        }
        if (version != null) {
            exampleWrapper.eq(ExampleModel::getVersion, version);
        }
        if (status != null) {
            exampleWrapper.eq(ExampleModel::getStatus, status);
        }
        if (leftTime != null) {
            exampleWrapper.ge(ExampleModel::getCreateTime, leftTime);
        }
        if (rightTime != null) {
            exampleWrapper.le(ExampleModel::getCreateTime, rightTime);
        }
        //查询指定字段
//        exampleWrapper.select(ExampleModel::getId, ExampleModel::getUsername, ExampleModel::getPassword);
        return exampleMapper.selectByExample(exampleWrapper.example(), new RowBounds(offset, limit));

//        return exampleMapper.selectByExample(exampleWrapper.example()
//                        .selectColumns(ExampleModel::getId, ExampleModel::getUsername, ExampleModel::getPassword),
//                new RowBounds(offset, limit));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ExampleModel source) {
        ExampleModel target = selectByPrimaryKey(source.id);
        copyPropertiesIgnoreNull(source, target);
        target.version = target.version + 1;
        target.updateTime = Timestamp.from(Instant.now());
        //更新非空字段
        exampleMapper.updateByPrimaryKeySelective(target);
    }

    public ExampleModel selectByPrimaryKey(Long id) {
        if (id == null) {
            throw new GenericException(HttpStatus.BAD_REQUEST, "id is required");
        }
        Optional<ExampleModel> target = exampleMapper.selectByPrimaryKey(id);
        return target.orElseThrow(() -> new GenericException(HttpStatus.NOT_FOUND, String.format("id %s not exists", id)));
    }


    /**
     * 强制更新null值,fields字段注意数据库非空限制
     * only extras support null
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateNull(ExampleModel source) {
        ExampleModel target = selectByPrimaryKey(source.id);
        copyPropertiesIgnoreNull(source, target);
        target.version = target.version + 1;
        target.updateTime = Timestamp.from(Instant.now());
        if (true) { //执行需要更新为null的逻辑
            target.extras = null;
        }
        //Fn.of(ExampleModel::getExtras, ExampleModel::getUpdateTime);
        exampleMapper.updateByPrimaryKeySelectiveWithForceFields(target, Fn.of(ExampleModel::getExtras));
    }

    public void copyPropertiesIgnoreNull(Object source, Object target) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");

        List<String> ignoreProperties = new ArrayList<>();
        Stream.of(source.getClass().getFields()).forEach(field -> {
            try {
                if (field.get(source) == null) {
                    ignoreProperties.add(field.getName());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        BeanUtils.copyProperties(source, target, ignoreProperties.toArray(new String[0]));
    }

    public void batchInsert(List<ExampleModel> models) {
        exampleMapper.insertList(models);
    }
}
