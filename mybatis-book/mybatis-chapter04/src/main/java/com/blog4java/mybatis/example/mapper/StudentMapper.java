package com.blog4java.mybatis.example.mapper;


import com.blog4java.mybatis.example.entity.StudentEntity;
import org.apache.ibatis.annotations.Param;

public interface StudentMapper {

	public StudentEntity getStudentById(int id);

	public int addStudent(StudentEntity student);

	public int updateStudentName(@Param("name") String name, @Param("id") int id);

	public StudentEntity getStudentByIdWithClassInfo(int id);
}
