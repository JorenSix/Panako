package be.panako.tests;

import be.panako.strategy.Strategy;
import be.panako.strategy.panako.PanakoStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PanakoStrategyTest {

    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testPanakoStrategy(){
        Strategy s = new PanakoStrategy();
        //s.store();
    }
}