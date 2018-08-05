package com.localore.localore;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;

import java.util.List;

/**
 * Activity regarding an exercise.
 *
 * @pre Session-exercise set before starting activity.
 */
public class ExerciseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(db);
        int progress = ExerciseControl.progressOfExercise(exercise.getId(), db);
        int requiredNoExerciseReminders = SessionControl.loadExercise(db).getNoRequiredExerciseReminders();
        int noPassedLevels = ExerciseControl.loadPassedQuizzesInExercise(exercise.getId(), db).size();

        List<int[]> quizCategoriesData = ExerciseControl.loadQuizCategoriesData(exercise.getId(), db);

        setTitle(exercise.getName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
    }
}
