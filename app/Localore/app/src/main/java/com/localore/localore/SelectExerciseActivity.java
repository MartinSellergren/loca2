package com.localore.localore;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
        invalidateOptionsMenu();

        View container = findViewById(R.id.layout_selectExercise);
        Animation loadAnimation = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
        loadAnimation.setDuration(1000);
        container.startAnimation(loadAnimation);

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

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(SelectExerciseActivity.this);
                alertBuilder.setTitle(getString(R.string.delete_confirmation_request) + delExercise.getName() + "?");

                CharSequence[] dialogOptions = {getString(R.string.Yes), getString(R.string.No)};
                alertBuilder.setItems(dialogOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) ExerciseControl.deleteExercise(delExercise, db);
                    }
                });

                alertBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        updateLayout();
                    }
                });


                AlertDialog functionDialog = alertBuilder.create();
                functionDialog.show();
            }
        }).attachToRecyclerView(recyclerView_exerciseLabels);

//        updateLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLayout();
    }

    //region options-menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exercise_actions, menu);

        menu.findItem(R.id.menuItem_exerciseProgress).setVisible(false);
        menu.findItem(R.id.menuItem_selectExercise).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItem_about) {
            AboutActivity.start(SelectExerciseActivity.this);
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion


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
                    ExerciseActivity.start(exercise.getId(), SelectExerciseActivity.this);
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

    /**
     * Start the select-exercise activity.
     * @param v
     */
    public void onCreateExercise(View v) {
        CreateExerciseActivity.start(this);
    }

    /**
     * Starts the activity.
     */
    public static void start(Context context) {
        LocaUtils.startActivity(SelectExerciseActivity.class, context);
    }

    /**
     * Quit app on second back-press.
     */
    @Override
    public void onBackPressed() {
        LocaUtils.quitSecondTime(this);
    }
}
