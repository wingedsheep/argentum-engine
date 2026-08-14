package com.wingedsheep.ai.puzzles

import com.wingedsheep.ai.puzzles.categories.ActivatedAbilityPuzzles
import com.wingedsheep.ai.puzzles.categories.BlockingPuzzles
import com.wingedsheep.ai.puzzles.categories.BoardWipeTimingPuzzles
import com.wingedsheep.ai.puzzles.categories.CombatKeywordPuzzles
import com.wingedsheep.ai.puzzles.categories.HoldingInstantsPuzzles
import com.wingedsheep.ai.puzzles.categories.LastChancePuzzles
import com.wingedsheep.ai.puzzles.categories.LethalDetectionPuzzles
import com.wingedsheep.ai.puzzles.categories.NonCreatureValuationPuzzles
import com.wingedsheep.ai.puzzles.categories.PriorityTimingPuzzles
import com.wingedsheep.ai.puzzles.categories.RaceMathPuzzles
import com.wingedsheep.ai.puzzles.categories.RemovalTargetingPuzzles
import com.wingedsheep.ai.puzzles.categories.SequencingPuzzles
import com.wingedsheep.ai.puzzles.categories.StackResponsePuzzles

/**
 * Every puzzle, in category order. Thirteen categories, at least six positions each.
 *
 * Adding a puzzle is: write it in its category file, run `just arena-puzzles`, and add its id to
 * [PuzzleSuiteTest.KNOWN_FAILURES] if the AI does not solve it yet.
 */
object PuzzleCatalog {

    val all: List<AiPuzzle> = listOf(
        LethalDetectionPuzzles.all(),
        BlockingPuzzles.all(),
        RemovalTargetingPuzzles.all(),
        HoldingInstantsPuzzles.all(),
        SequencingPuzzles.all(),
        BoardWipeTimingPuzzles.all(),
        RaceMathPuzzles.all(),
        NonCreatureValuationPuzzles.all(),
        // Phase 2b.
        StackResponsePuzzles.all(),
        ActivatedAbilityPuzzles.all(),
        CombatKeywordPuzzles.all(),
        // Phase 2c — timing.
        PriorityTimingPuzzles.all(),
        LastChancePuzzles.all(),
    ).flatten()

    fun byCategory(category: PuzzleCategory): List<AiPuzzle> = all.filter { it.category == category }
}
