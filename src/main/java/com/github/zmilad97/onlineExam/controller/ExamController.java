package com.github.zmilad97.onlineExam.controller;

import com.github.zmilad97.onlineExam.module.Exam;
import com.github.zmilad97.onlineExam.module.Question;
import com.github.zmilad97.onlineExam.security.SecurityUtil;
import com.github.zmilad97.onlineExam.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exam/")
public class ExamController {
    private final ExamService examService;
    private final CorrectionService correctionService;
    private final QuestionService questionService;
    private final UserService userService;
    private final ScoreService scoreService;

    @Autowired
    public ExamController(ExamService examService, CorrectionService correctionService, QuestionService questionService, UserService userService, ScoreService scoreService) {
        this.examService = examService;
        this.correctionService = correctionService;
        this.questionService = questionService;
        this.userService = userService;
        this.scoreService = scoreService;
    }

  /**
   * It's better to write documentation for you class or methods like this, standard javadoc :)
   * @param exam
   */
  //adding a exam
    @PostMapping("add")
    public void addExam(@RequestBody Exam exam) {
        exam.setMakerId(SecurityUtil.getCurrentUser().getId());
        exam.setActive(true);
        examService.save(exam);
    }

    //returns all creator's exams
    @GetMapping("myExam")
    public List<Exam> myExam() {
        return examService.findByMakerId(SecurityUtil.getCurrentUser().getId());
    }

    //returns all available exams to the student
    @GetMapping("available")
    public List<Exam> availableExams() {
     return examService.findByActiveTrueAndIdIn(
         SecurityUtil.getCurrentUser().getPermissionList()
             .stream().map(Long::parseLong).collect(Collectors.toList()));
    }

    @GetMapping("{examId}")
    public String getExamTitleById(@PathVariable String examId) {
        return examService.findById(Long.parseLong(examId)).getTitle();
    }

    @GetMapping("/{examId}/questions")
    public List<Question> getQuestionsByExamId(@PathVariable long examId) {
        //TODO : need to check if student participate or not
        if (scoreService.findByUserIdAndExamId(SecurityUtil.getCurrentUser().getId(), examId) == null)
            return questionService.findByExamId(examId);
        else
            return null;
    }


    //this method allows Master or Admin to choose students for taking exam
    @PostMapping("{examId}/add-to-exam")
    public void givePermissionToStudent(@RequestBody List<Long> studentsId, @PathVariable String examId) {

        if (examService.findById(Long.parseLong(examId)).getMakerId() == SecurityUtil.getCurrentUser().getId()
                || SecurityUtil.getCurrentUser().getRoles().equals("ADMIN")) {
            studentsId.forEach(id -> userService.findUserById(id).addPermission(examId));
            studentsId.forEach(id -> userService.save(userService.findUserById(id)));
        }
    }

    //this method allows MASTER or ADMIN to remove a student from exam
    @PostMapping("{examId}/remove-from-exam")
    public void takePermission(@RequestBody Long studentId, @PathVariable String examId) {
        //You get runtimeException if the ExamId is not number or it's corresponding exam entity is not found
        if (examService.findById(Long.parseLong(examId)).getMakerId() == SecurityUtil.getCurrentUser().getId()
                || SecurityUtil.getCurrentUser().getRoles().equals("ADMIN")) {

            List<String> permissionList = userService.findUserById(studentId).getPermissionList();
            permissionList.remove(examId);
            userService.findUserById(studentId).setPermissions(permissionList);
            userService.save(userService.findUserById(studentId));
        }

    }


    //TODO : add a method to get exam and answer


    //TODO : this method seems not right
    //this method gets the User's answers of a exam in a map structure that has a userId and a examId Key and value
    @PostMapping("answer")
    public void takeAnswers(@RequestBody Map<String, String> answer) {
        correctionService.correction(answer);
    }


    //this method return list of exam that in same category
    @GetMapping("category/{categoryName}")
    public List<Exam> getExamsByCategory(@PathVariable String categoryName) {
        return examService.findByCategory(categoryName);
    }

    //returns the exam in same grade
    @GetMapping("grade/{grade}")
    public List<Exam> getExamsByGrade(@PathVariable String grade) {
        return examService.findByGrade(grade);
    }

    //search the exam by title
    @GetMapping("search/{title}")
    public List<Exam> searchExam(@PathVariable String title) {
        return examService.findAllByTitleContains(title);
    }


}
