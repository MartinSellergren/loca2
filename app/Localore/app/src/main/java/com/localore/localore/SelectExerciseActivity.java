package com.localore.localore;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.Session;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;

import java.util.List;

/**
 * Activity for selecting exercise / choosing to create a new exercise.
 *
 * @pre Session-user set before starting activity.
 */
public class SelectExerciseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppDatabase db = AppDatabase.getInstance(this);
        long userId = SessionControl.load(db).getUserId();
        List<Exercise> exercises = db.exerciseDao().loadWithUserOrderedByDisplayIndex(userId);
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(userId, db);

        RecyclerView recyclerView = findViewById(R.id.recyclerView_exerciseLabel);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //recyclerView.setHasFixedSize(true); //todo: recyclerView fixed size?
        ExerciseLabelAdapter adapter = new ExerciseLabelAdapter(exercises, exerciseProgresses);
        recyclerView.setAdapter(adapter);

        setTitle(getString(R.string.select_exercise));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_exercise);
    }

    /**
     * @param v Button that initiated the request.
     */
    public void startCreateExerciseActivity(View v) {
        Intent intent = new Intent(this, CreateExerciseActivity.class);
        startActivity(intent);
    }


    //region the list content: recycler-view adapter and view-holder

    /**
     * The Adapter for the exercise-list.
     */
    private class ExerciseLabelAdapter extends RecyclerView.Adapter<ExerciseLabelHolder> {
        private List<Exercise> exercises;
        private List<Integer> exerciseProgresses;

        public ExerciseLabelAdapter(List<Exercise> exercises, List<Integer> exerciseProgresses) {
            this.exercises = exercises;
            this.exerciseProgresses = exerciseProgresses;
        }

        @Override
        public ExerciseLabelHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(SelectExerciseActivity.this);
            return new ExerciseLabelHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ExerciseLabelHolder holder, int i) {
            Exercise exercise = this.exercises.get(i);
            int progress = this.exerciseProgresses.get(i);
            holder.bind(exercise, progress);
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }
    }

    /**
     * One exercise-label (i.e one list-item in the list of exercises).
     */
    private class ExerciseLabelHolder extends RecyclerView.ViewHolder {
        private Exercise exercise;
        private int progress;
        private TextView textView;

        public ExerciseLabelHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.listitem_exercise_label, parent, false));

            this.textView = (TextView) itemView.findViewById(R.id.textView_exerciseLabel);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AppDatabase db = AppDatabase.getInstance(SelectExerciseActivity.this);
                    SessionControl.enterExercise(exercise.getId(), db);
                    Intent intent = new Intent(SelectExerciseActivity.this, ExerciseActivity.class);
                    startActivity(intent);
                }
            });
        }

        public void bind(Exercise exercise, int progress) {
            this.exercise = exercise;
            this.progress = progress;
            textView.setText(String.format("%s : %s percent", exercise.getName(), progress));
        }
    }

    //endregion
}
