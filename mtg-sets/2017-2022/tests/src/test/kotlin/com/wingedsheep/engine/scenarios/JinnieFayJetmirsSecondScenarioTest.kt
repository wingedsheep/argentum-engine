package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jinnie Fay, Jetmir's Second (SNC #195) — {R/G}{G}{G/W} Legendary Creature — Elf Druid 3/3.
 *
 *   If you would create one or more tokens, you may instead create that many 2/2 green Cat
 *   creature tokens with haste or that many 3/1 green Dog creature tokens with vigilance.
 *
 * Covers: Cats for Raise the Alarm's two Soldiers (and they have haste), a Dog for Strike It
 * Rich's Treasure (any token, not just creatures), and declining keeps the original tokens.
 */
class JinnieFayJetmirsSecondScenarioTest : ScenarioTestBase() {

    init {
        context("Jinnie Fay, Jetmir's Second") {

            test("two Soldiers become two Cats with haste") {
                val game = jinnieGame()
                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                choose(game, 0)

                withClue("that many Cats instead of the Soldiers") {
                    game.findAllPermanents("Cat Token").size shouldBe 2
                    game.findAllPermanents("Soldier Token").size shouldBe 0
                }
                val cat = game.findAllPermanents("Cat Token").first()
                game.state.projectedState.getPower(cat) shouldBe 2
                game.state.projectedState.hasKeyword(cat, Keyword.HASTE) shouldBe true
            }

            test("a Treasure becomes a 3/1 Dog with vigilance") {
                val game = jinnieGame()
                game.castSpell(1, "Strike It Rich").error shouldBe null
                game.resolveStack()
                choose(game, 1)

                game.findAllPermanents("Treasure").size shouldBe 0
                val dog = game.findAllPermanents("Dog Token").single()
                game.state.projectedState.getPower(dog) shouldBe 3
                game.state.projectedState.getToughness(dog) shouldBe 1
                game.state.projectedState.hasKeyword(dog, Keyword.VIGILANCE) shouldBe true
            }

            test("declining keeps the original tokens") {
                val game = jinnieGame()
                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()
                choose(game, 2)

                game.findAllPermanents("Soldier Token").size shouldBe 2
                game.findAllPermanents("Cat Token").size shouldBe 0
            }
        }
    }

    private fun jinnieGame(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Jinnie Fay, Jetmir's Second")
        .withCardInHand(1, "Raise the Alarm")
        .withCardInHand(1, "Strike It Rich")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun choose(game: TestGame, option: Int) {
        val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("Jinnie's controller makes the choice") { decision.playerId shouldBe game.player1Id }
        game.submitDecision(OptionChosenResponse(decision.id, option)).error shouldBe null
        game.resolveStack()
    }
}
