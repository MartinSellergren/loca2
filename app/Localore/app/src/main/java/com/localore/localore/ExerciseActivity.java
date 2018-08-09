package com.localore.localore;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DialogTitle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.QuizCategory;
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

        AppDatabase db = AppDatabase.getInstance(this);
        Exercise exercise = SessionControl.loadExercise(db);
        int progress = ExerciseControl.progressOfExercise(exercise.getId(), db);
        int requiredNoExerciseReminders = SessionControl.loadExercise(db).getNoRequiredExerciseReminders();
        int noPassedLevels = ExerciseControl.loadPassedQuizzesInExercise(exercise.getId(), db).size();
        List<int[]> quizCategoriesData = ExerciseControl.loadQuizCategoriesData(exercise.getId(), db);

        setTitle(exercise.getName());

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
        private int type;
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
                    alertBuilder.setTitle(QuizCategory.DISPLAY_TYPES[type]);

                    CharSequence[] dialogOptions = {"Tapping", "Level quiz", "Reminder"};
                    alertBuilder.setItems(dialogOptions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (item == 0) {
                                TappingActivity.start(type, ExerciseActivity.this);
                            }
                            else if (item == 1) {
                                Intent intent = new Intent(ExerciseActivity.this, QuizActivity.class);
                                startActivity(intent);
                            }
                            else {
                                Intent intent = new Intent(ExerciseActivity.this, QuizActivity.class);
                                startActivity(intent);
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

            this.type = quizCategoryData[0];
            this.totalNoLevels = quizCategoryData[1];
            this.nextLevel = quizCategoryData[2];
            this.noRequiredReminders = quizCategoryData[3];

            this.quizCategoryIcon.setImageResource(R.drawable.mapbox_compass_icon);
            this.quizCategoryName.setText( getString(QuizCategory.DISPLAY_TYPES[type]) );

            String levelString = String.format("%s %s / %s",
                    getString(R.string.Level), nextLevel, totalNoLevels);

            this.quizCategoryLevel.setText(levelString);
        }

    }

//    /**
//     * Dialog shown when user clicks quiz-category.
//     * For starting 1) tapping, 2) exercise-quiz, 3) quiz-category-reminder
//     */
//    public static class QuizCategoryDialog extends DialogFragment {
//
//        @NonNull
//        @Override
//        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//
//            builder.setNegativeButton("Tapping", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    TappingActivity.start(0, getActivity());
//                }
//            });
//
//            builder.setNeutralButton("Exercise quiz", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    Intent intent = new Intent(getActivity(), QuizActivity.class);
//                    startActivity(intent);
//                }
//            });
//
//            builder.setPositiveButton("Reminder", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    Intent intent = new Intent(getActivity(), QuizActivity.class);
//                    startActivity(intent);
//                }
//            });
//
//            return builder.create();
//        }
//
//    }

    //endregion

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, SelectExerciseActivity.class);
        startActivity(intent);
    }
}
