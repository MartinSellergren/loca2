package com.localore.localore;

import com.localore.localore.modelManipulation.ExerciseControl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void ExerciseCreation_groupEquallySizedLevelsTest() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 14; i++) ids.add(i);

        List<List<Long>> groups = ExerciseControl.groupEquallySizedLevels(ids);
        System.out.println(groups);
    }
}