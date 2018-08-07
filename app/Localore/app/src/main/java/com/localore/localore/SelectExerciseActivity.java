package com.localore.localore;

import android.app.Dialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.modelManipulation.ExerciseControl;
import com.localore.localore.modelManipulation.SessionControl;

import java.util.List;

/**
 * Activity for selecting exercise / choosing to create a new exercise.
 *
 * @pre Session-user set before starting activity.
 */
public class SelectExerciseActivity extends AppCompatActivity {

    private RecyclerView recyclerView_exerciseLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_exercise);
        setTitle(getString(R.string.select_exercise));

        this.recyclerView_exerciseLabels = findViewById(R.id.recyclerView_exerciseLabels);
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int dir) {
                int pos = viewHolder.getAdapterPosition();

                AppDatabase db = AppDatabase.getInstance(SelectExerciseActivity.this);
                Exercise delExercise = db.exerciseDao().loadWithDisplayIndex(pos);
                ExerciseControl.deleteExercise(delExercise, db);
                updateLayout();
            }
        }).attachToRecyclerView(recyclerView_exerciseLabels);

        updateLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLayout();
    }

    /**
     * Sets layout based on database-content.
     */
    private void updateLayout() {
        AppDatabase db = AppDatabase.getInstance(this);
        long userId = SessionControl.load(db).getUserId();
        List<Exercise> exercises = db.exerciseDao().loadWithUserOrderedByDisplayIndex(userId);
        List<Integer> exerciseProgresses = ExerciseControl.exerciseProgresses(userId, db);

        recyclerView_exerciseLabels.setLayoutManager(new LinearLayoutManager(this));
        recyclerView_exerciseLabels.setHasFixedSize(true); //todo: recyclerView fixed size?
        ExerciseLabelAdapter adapter = new ExerciseLabelAdapter(exercises, exerciseProgresses);
        recyclerView_exerciseLabels.setAdapter(adapter);
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
            return this.exercises.size();
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

            this.textView = (TextView)itemView.findViewById(R.id.textView_exerciseLabel);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AppDatabase db = AppDatabase.getInstance(SelectExerciseActivity.this);
                    SessionControl.setActiveExercise(exercise.getId(), db);
                    Intent intent = new Intent(SelectExerciseActivity.this, ExerciseActivity.class);
                    startActivity(intent);
                }
            });
        }

        public void bind(Exercise exercise, int progress) {
            this.exercise = exercise;
            this.progress = progress;
            this.textView.setText(String.format("%s : %s percent", exercise.getName(), progress));
        }
    }

    //endregion
}
