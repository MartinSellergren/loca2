package com.localore.localore;

import android.app.Activity;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Exercise;
import com.localore.localore.model.Question;
import com.localore.localore.model.Quiz;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.modelManipulation.RunningQuizControl;

import java.util.List;

public class QuizResultActivity extends AppCompatActivity {

    /**
     * Feedback based on success-rate of quiz. Level cleared: index=0.
     */
    public static final int[] FEEDBACK = {
            R.string.excellent_result_feedback,
            R.string.good_result_feedback,
            R.string.poor_result_feedback};
    public static final int EXCELLENT_RESULT = 0;
    public static final int GOOD_RESULT = 1;
    public static final int POOR_RESULT = 2;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuiz runningQuiz = RunningQuizControl.load(this);
        List<Question> questions = db.questionDao().loadWithRunningQuiz(runningQuiz.getId());
        double successRate = RunningQuizControl.successRate(questions);

        int feedbackIndex = feedback(successRate);
        String feedbackStr = getString(FEEDBACK[feedbackIndex]);
        if (runningQuiz.getType() == RunningQuiz.LEVEL_QUIZ && feedbackIndex == EXCELLENT_RESULT) {
            feedbackStr += "\n\n " + getString(R.string.level_cleared);
        }

        this.textView = findViewById(R.id.textView_quizResult);
        textView.setText(feedbackStr);

        ConstraintLayout layout = findViewById(R.id.layout_quizResult);
        layout.setOnClickListener(view -> doneOrFollowUp());
    }

    /**
     * Starts a follow-up if relevant, else quiz done.
     */
    private void doneOrFollowUp() {
        try {
            QuizActivity.freshStart(RunningQuiz.FOLLOW_UP_QUIZ, -1, this);
        }
        catch (RunningQuizControl.QuizConstructionException e) {
            ExerciseActivity.start(this);
        }
    }

    /**
     * @param successRate
     * @return Brief feedback.
     */
    private static int feedback(double successRate) {
        if (successRate >= RunningQuizControl.ACCEPTABLE_QUIZ_SUCCESS_RATE)
            return EXCELLENT_RESULT;
        else if (successRate >= 0.75)
            return GOOD_RESULT;
        else
            return POOR_RESULT;
    }


    /**
     * @param oldActivity
     */
    public static void start(Activity oldActivity) {
        LocaUtils.fadeInActivity(QuizResultActivity.class, oldActivity);
    }
}
