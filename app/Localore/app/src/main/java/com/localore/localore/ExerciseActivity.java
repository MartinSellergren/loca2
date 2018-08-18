package com.localore.localore;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Telephony;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity regarding an exercise.
 *
 * @pre Session-exercise set before starting activity.
 */
public class ExerciseActivity extends AppCompatActivity {

    private FrameLayout overlay;
    private Button exerciseReminderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);

        this.overlay = findViewById(R.id.overlay_exercise);
        this.exerciseReminderButton = findViewById(R.id.button_exerciseReminder);

        updateLayout();
    }

    //region options-menu

    /**
     * Set progress in options-bar.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exercise_actions, menu);

        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(this);
        int progress = ExerciseControl.progressOfExercise(exercise.getId(), db);

        MenuItem progressItem = menu.findItem(R.id.menuItem_exerciseProgress);
        progressItem.setTitle(progress + "%");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItem_selectExercise) {
            SelectExerciseActivity.start(ExerciseActivity.this);
        }
        else if (item.getItemId() == R.id.menuItem_about) {
            AboutActivity.start(ExerciseActivity.this);
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    /**
     * Updates layout based on database.
     */
    private void updateLayout() {
        Exercise exercise = SessionControl.loadExercise(this);
        setTitle(exercise.getName());

        updateQuizCategories();
        updateExerciseReminder();

        exerciseReminderButton.setOnClickListener(view -> {
            QuizActivity.freshStart(RunningQuiz.EXERCISE_REMINDER, -1, this);
        });
    }

    /**
     * Updates the quiz-categories layout and events.
     */
    private void updateQuizCategories() {
        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(this);
        List<int[]> quizCategoriesData = ExerciseControl.loadQuizCategoriesData(exercise.getId(), db);

        RecyclerView recyclerView = findViewById(R.id.recyclerView_quizCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        QuizCategoryAdapter adapter = new QuizCategoryAdapter(quizCategoriesData);
        recyclerView.setAdapter(adapter);
    }

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

            itemView.setOnClickListener(view -> {
                buildDialog().show();
            });
        }

        /**
         * Dialog that displays tapping-quiz-reminder. Only possible options appear.
         * Reminder-entry changes in appearance based on state.
         * @return The dialog.
         */
        private AlertDialog buildDialog() {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(ExerciseActivity.this);
            alertBuilder.setTitle(QuizCategory.DISPLAY_TYPES[quizCategoryType]);

            List<CharSequence> optionsList = new ArrayList<>();
            optionsList.add("Tapping");

            if (noRequiredReminders == 0 && nextLevel < totalNoLevels)
                optionsList.add("Level quiz");

            if (nextLevel > 0) {
                String reminderEntry = "Reminder";
                if (noRequiredReminders > 0) reminderEntry += String.format(" (%s)", noRequiredReminders);
                optionsList.add(reminderEntry);
            }

            CharSequence[] dialogOptions = optionsList.toArray(new CharSequence[optionsList.size()]);
            alertBuilder.setItems(dialogOptions, (dialog, item) -> {
                if (item == 0) {
                    TappingActivity.start(quizCategoryType, ExerciseActivity.this);
                }
                else if (item == 1) {
                    QuizActivity.freshStart(RunningQuiz.LEVEL_QUIZ, quizCategoryType, ExerciseActivity.this);
                }
                else {
                    QuizActivity.freshStart(RunningQuiz.QUIZ_CATEGORY_REMINDER, quizCategoryType, ExerciseActivity.this);
                }
            });

            return alertBuilder.create();
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

    //endregion

    private void updateExerciseReminder() {
        int requiredNoExerciseReminders = SessionControl.loadExercise(this).getNoRequiredExerciseReminders();

        if (requiredNoExerciseReminders > 0) {
            exerciseReminderButton.setText(String.format("%s (%s)",
                    getString(R.string.reminder),
                    requiredNoExerciseReminders));

            overlay.setAlpha(0.5f);
            overlay.setClickable(true);
        }
    }


    /**
     * Starts the activity and sets exercise in session.
     * @param oldActivity
     */
    public static void start(long exerciseId, Activity oldActivity) {
        AppDatabase db = AppDatabase.getInstance(oldActivity);
        SessionControl.setActiveExercise(exerciseId, db);
        LocaUtils.fadeInActivity(ExerciseActivity.class, oldActivity);
    }

    /**
     * Start with exercise already in session.
     * @param oldActivity
     */
    public static void start(Activity oldActivity) {
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
