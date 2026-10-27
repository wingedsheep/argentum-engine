package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Battle for Bretagard (KHM #203).
 *
 *   I — Create a 1/1 white Human Warrior creature token.
 *   II — Create a 1/1 green Elf Warrior creature token.
 *   III — Choose any number of artifact tokens and/or creature tokens you control with different
 *         names. For each of them, create a token that's a copy of it.
 *
 * Plays the Saga through all three chapters: the Human Warrior Token and Elf Warrior Token have
 * different names, so both may be copied; a nontoken creature is never offered; choosing none
 * copies nothing.
 */
class BattleForBretagardScenarioTest : ScenarioTestBase() {

    init {
        fun setup() = scenario()
            .withPlayers("Bretagard", "Opponent")
            .withCardInHand(1, "Battle for Bretagard")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(8) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
            .build()

        /** Cast the Saga and advance to the start of chapter III's resolution. */
        fun TestGame.playToChapterThree() {
            castSpell(1, "Battle for Bretagard").error shouldBe null
            resolveStack()
            withClue("chapter I made a Human Warrior") { findAllPermanents("Human Warrior Token").size shouldBe 1 }

            // Through the opponent's turn to our next precombat main, where the lore counter lands.
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
            withClue("chapter II made an Elf Warrior") { findAllPermanents("Elf Warrior Token").size shouldBe 1 }

            // Through the opponent's turn to our next precombat main, where the lore counter lands.
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
        }

        context("chapter III") {
            test("copies each chosen token with a different name, never a nontoken creature") {
                val game = setup()
                game.playToChapterThree()

                val human = game.findAllPermanents("Human Warrior Token").single()
                val elf = game.findAllPermanents("Elf Warrior Token").single()
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the two tokens are offered, held to different names") {
                    decision.options shouldContainExactlyInAnyOrder listOf(human, elf)
                    decision.onePerCardName shouldBe true
                }
                game.selectCards(listOf(human, elf)).error shouldBe null
                game.resolveStack()

                val humans = game.findAllPermanents("Human Warrior Token")
                val elves = game.findAllPermanents("Elf Warrior Token")
                humans.size shouldBe 2
                elves.size shouldBe 2
                (humans + elves).all { game.state.getEntity(it)!!.has<TokenComponent>() } shouldBe true
                withClue("the Saga is sacrificed after chapter III") {
                    game.findPermanent("Battle for Bretagard") shouldBe null
                }
            }

            test("choosing none copies nothing") {
                val game = setup()
                game.playToChapterThree()

                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.findAllPermanents("Human Warrior Token").size shouldBe 1
                game.findAllPermanents("Elf Warrior Token").size shouldBe 1
            }
        }
    }
}
