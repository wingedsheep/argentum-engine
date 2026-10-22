package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.numberChoice
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sigarda's Splendor — notes its controller's life total as it enters, draws at each of their
 * upkeeps if their life total is at least the last noted one, then notes it again; and gains 1
 * life whenever they cast a white spell.
 */
class SigardasSplendorScenarioTest : ScenarioTestBase() {

    init {
        /** Pass through the opponent's turn to the start of our next upkeep, its trigger resolved. */
        fun TestGame.toMyUpkeepResolved() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            resolveStack()
        }

        fun TestGame.notedLife(): Int? =
            state.getEntity(findPermanent("Sigarda's Splendor")!!)?.numberChoice(ChoiceSlot.NOTED_NUMBER)

        fun TestGame.castSplendor() {
            castSpell(1, "Sigarda's Splendor").error shouldBe null
            resolveStack()
        }

        fun builder() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Sigarda's Splendor")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains").withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains").withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        context("Sigarda's Splendor") {
            test("notes your life total as it enters") {
                val game = builder().withLifeTotal(1, 14).build()
                game.castSplendor()
                game.notedLife() shouldBe 14
            }

            test("draws at upkeep when life is equal to the noted total, and notes it again") {
                val game = builder().withLifeTotal(1, 14).build()
                game.castSplendor()
                game.handSize(1) shouldBe 0

                game.toMyUpkeepResolved()
                withClue("14 >= 14: the upkeep trigger draws") { game.handSize(1) shouldBe 1 }
                game.notedLife() shouldBe 14
            }

            test("no draw after losing life, but the lower total is noted so the next upkeep draws") {
                val game = builder()
                    .withLifeTotal(1, 14)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()
                game.castSplendor()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 11
                game.handSize(1) shouldBe 0

                game.toMyUpkeepResolved()
                withClue("11 < 14: no draw") { game.handSize(1) shouldBe 0 }
                withClue("the note is taken even without a draw") { game.notedLife() shouldBe 11 }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.toMyUpkeepResolved()
                withClue("draw step card + the upkeep draw (11 >= 11)") { game.handSize(1) shouldBe 2 }
            }

            test("destroyed in response to its upkeep trigger, it compares against its last-known note") {
                val game = builder()
                    .withLifeTotal(1, 14)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()
                game.castSplendor()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 11

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.stack.size shouldBe 1
                game.castSpell(1, "Disenchant", game.findPermanent("Sigarda's Splendor")!!).error shouldBe null
                game.resolveStack()

                game.findPermanent("Sigarda's Splendor") shouldBe null
                withClue("11 < last noted 14: no draw, even with the enchantment gone") { game.handSize(1) shouldBe 0 }
            }

            test("gaining life since the last note still draws") {
                val game = builder()
                    .withLifeTotal(1, 14)
                    .withCardInHand(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .build()
                game.castSplendor()
                game.castSpell(1, "Savannah Lions").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 15

                game.toMyUpkeepResolved()
                withClue("15 > 14: draw") { game.handSize(1) shouldBe 1 }
                game.notedLife() shouldBe 15
            }

            test("casting a white spell gains 1 life; a nonwhite spell and its own cast don't") {
                val game = builder()
                    .withLifeTotal(1, 14)
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()
                game.castSplendor()
                withClue("Splendor isn't on the battlefield when it is cast") { game.getLifeTotal(1) shouldBe 14 }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 14

                game.castSpell(1, "Savannah Lions").error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 15
            }
        }
    }
}
