package com.localore.localore;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(this);
        int progress = ExerciseControl.progressOfExercise(exercise.getId(), db);
        int requiredNoExerciseReminders = SessionControl.loadExercise(this).getNoRequiredExerciseReminders();
        int noPassedLevels = ExerciseControl.loadPassedQuizzesInExercise(exercise.getId(), db).size();
        List<int[]> quizCategoriesData = ExerciseControl.loadQuizCategoriesData(exercise.getId(), db);

        setTitle(exercise.getName());

        RecyclerView recyclerView = findViewById(R.id.recyclerView_quizCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        QuizCategoryAdapter adapter = new QuizCategoryAdapter(quizCategoriesData);
        recyclerView.setAdapter(adapter);
    }

    //region options-menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exercise_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItem_selectExercise) {
            SelectExerciseActivity.start(ExerciseActivity.this);
            finish();
        }
        else if (item.getItemId() == R.id.menuItem_about) {
            AboutActivity.start(ExerciseActivity.this);
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region quiz-categories RecyclerView

    private class QuizCategoryAdapter extends RecyclerView.Adapter<QuizCategoryHolder> {
        private List<int[]> quizCategoriesData;

        public QuizCategoryAdapter(List<int[]> quizCategoriesData) {
            this.quizCategoriesData = quizCategoriesData;
        }

        @NonNull
        @Override
        public QuizCategoryHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(ExerciseActivity.this);
            return new QuizCategoryHolder(inflater, viewGroup);
        }

        @Override
        public void onBindViewHolder(@NonNull QuizCategoryHolder quizCategoryHolder, int i) {
            int[] quizCategoryData = this.quizCategoriesData.get(i);
            quizCategoryHolder.bind(quizCategoryData);
        }

        @Override
        public int getItemCount() {
            return this.quizCategoriesData.size();
        }
    }

    private class QuizCategoryHolder extends RecyclerView.ViewHolder {
        private int quizCategoryType;
        private int totalNoLevels;
        private int nextLevel;
        private int noRequiredReminders;

        private ImageView quizCategoryIcon;
        private TextView quizCategoryName;
        private TextView quizCategoryLevel;

        public QuizCategoryHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.listitem_quiz_category, parent, false));

            this.quizCategoryIcon = itemView.findViewById(R.id.imageView_quizCategoryIcon);
            this.quizCategoryName = itemView.findViewById(R.id.textView_quizCategoryName);
            this.quizCategoryLevel = itemView.findViewById(R.id.textView_quizCategoryLevel);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ExerciseActivity.this);
                    alertBuilder.setTitle(QuizCategory.DISPLAY_TYPES[quizCategoryType]);

                    CharSequence[] dialogOptions = {"Tapping", "Level quiz", "Reminder"};
                    alertBuilder.setItems(dialogOptions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (item == 0) {
                                TappingActivity.start(quizCategoryType, ExerciseActivity.this);
                            }
                            else if (item == 1) {
                                QuizActivity.freshStart(RunningQuiz.LEVEL_QUIZ, quizCategoryType, ExerciseActivity.this);
                                ExerciseActivity.this.finish();
                            }
                            else {
                                QuizActivity.freshStart(RunningQuiz.QUIZ_CATEGORY_REMINDER, quizCategoryType, ExerciseActivity.this);
                                ExerciseActivity.this.finish();
                            }
                        }
                    });

                    alertBuilder.create().show();
                }
            });
        }

        /**
         * @param quizCategoryData
         */
        public void bind(int[] quizCategoryData) {
            if (quizCategoryData.length != 4) {
                throw new RuntimeException("Bad quiz-category-data");
            }

            this.quizCategoryType = quizCategoryData[0];
            this.totalNoLevels = quizCategoryData[1];
            this.nextLevel = quizCategoryData[2];
            this.noRequiredReminders = quizCategoryData[3];

            this.quizCategoryIcon.setImageResource(R.drawable.mapbox_compass_icon);
            this.quizCategoryName.setText( getString(QuizCategory.DISPLAY_TYPES[quizCategoryType]) );

            String levelString = String.format("%s %s / %s",
                    getString(R.string.Level), nextLevel, totalNoLevels);

            this.quizCategoryLevel.setText(levelString);
        }

    }


    /**
     * Starts the activity.
     * @param oldActivity
     *
     * @pre exerciseId in session.
     */
    public static void start(long exerciseId, Activity oldActivity) {
        AppDatabase db = AppDatabase.getInstance(oldActivity);
        SessionControl.setActiveExercise(exerciseId, db);

        LocaUtils.fadeInActivity(ExerciseActivity.class, oldActivity);
    }

    /**
     * Quit app on second back-press.
     */
    @Override
    public void onBackPressed() {
        LocaUtils.quitSecondTime(this);
    }
}
