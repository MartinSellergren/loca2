package com.localore.localore;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.localore.localore.model.AppDatabase;
import com.localore.localore.model.Question;
import com.localore.localore.model.QuizCategory;
import com.localore.localore.model.RunningQuiz;
import com.localore.localore.modelManipulation.RunningQuizControl;
import com.localore.localore.modelManipulation.SessionControl;

public class QuizActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ViewFlipper flipper;
    private FloatingActionButton button_nextQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        this.progressBar = findViewById(R.id.progressBar_quiz);
        this.flipper = findViewById(R.id.viewFlipper_questions);
        this.button_nextQuestion = findViewById(R.id.button_nextQuestion);

        updateLayout();
    }

    /**
     * Sets layout based on current question in running-quiz.
     * Calls setContentView() with xml-layout based on question.
     */
    private void updateLayout() {
        AppDatabase db = AppDatabase.getInstance(this);
        RunningQuiz runningQuiz = RunningQuizControl.load(db);

        int currentQuestionIndex = runningQuiz.getCurrentQuestionIndex();
        int noQuestions = RunningQuizControl.noQuestions(db);
        updateProgressBar(currentQuestionIndex, noQuestions);

        Question question =
                db.questionDao().loadWithRunningQuizAndIndex(runningQuiz.getId(), currentQuestionIndex);

        switch (question.getType()) {
            case Question.NAME_IT:
                updateLayout_nameIt(question);
                break;
            case Question.PLACE_IT:
                updateLayout_placeIt(question);
                break;
            case Question.PAIR_IT:
                updateLayout_pairIt(question);
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        if (question.isAnsweredCorrectly())
            this.button_nextQuestion.show();
        else
            this.button_nextQuestion.hide();
    }

    /**
     * @param currentIndex [0, tot)
     * @param tot
     */
    private void updateProgressBar(int currentIndex, int tot) {

    }

    /**
     * @param question
     */
    private void updateLayout_nameIt(Question question) {
        this.flipper.setDisplayedChild(Question.NAME_IT);
    }

    /**
     *
     * @param question
     */
    private void updateLayout_placeIt(Question question) {
        this.flipper.setDisplayedChild(Question.PLACE_IT);
    }

    /**
     *
     * @param question
     */
    private void updateLayout_pairIt(Question question) {
        this.flipper.setDisplayedChild(Question.PAIR_IT);
    }

    /**
     * Called when user clicks next-question button.
     * Next question or done?
     *
     * @param view
     */
    public void onNextQuestion(View view) {

    }

    /**
     * Called when user clicks exit-button.
     * Only way out!
     * @param view
     */
    public void onExitQuiz(View view) {
        RunningQuizControl.deleteRunningQuiz(AppDatabase.getInstance(this));
        Intent intent = new Intent(this, ExerciseActivity.class);
        startActivity(intent);
    }


    /**
     * Quit app on back-press.
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Starts a new quiz by creating a running-quiz in db and starts the activity.
     * Loads the first question.
     *
     * @param quizType
     * @param quizCategory
     * @param context
     *
     * @pre Session-exercise set.
     * @pre If quizType=followup: A running-quiz in db.
     */
    public static void freshStart(int quizType, int quizCategory, Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        long exerciseId = SessionControl.load(db).getExerciseId();

        switch (quizType) {
            case RunningQuiz.LEVEL_QUIZ:
                RunningQuizControl.newLevelQuiz(exerciseId, quizCategory, db);
                break;
            case RunningQuiz.FOLLOW_UP_QUIZ:
                RunningQuizControl.newFollowUpQuiz(db);
                break;

            case RunningQuiz.QUIZ_CATEGORY_REMINDER:
                RunningQuizControl.newLevelReminder(exerciseId, quizCategory, db);
                break;
            case RunningQuiz.EXERCISE_REMINDER:
                RunningQuizControl.newExerciseReminder(exerciseId, db);
                break;
            default:
                throw new RuntimeException("Dead end");
        }

        RunningQuizControl.nextQuestion(db);

        Intent intent = new Intent(context, QuizActivity.class);
        context.startActivity(intent);
    }

    /**
     * Use when an exercise is running (i.e a running-quiz exists).
     * @param context
     */
    public static void resumedStart(Context context) {
        Intent intent = new Intent(context, QuizActivity.class);
        context.startActivity(intent);
    }
}
