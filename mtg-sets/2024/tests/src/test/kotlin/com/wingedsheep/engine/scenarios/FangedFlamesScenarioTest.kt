package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fanged Flames (MH3 #118) — {1}{R} Sorcery, Devoid.
 * "Fanged Flames deals 4 damage to target creature or planeswalker. If that creature or
 *  planeswalker would die this turn, exile it instead."
 */
class FangedFlamesScenarioTest : ScenarioTestBase() {

    init {
        context("Fanged Flames") {

            test("a creature killed by the 4 damage is exiled instead of dying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withCardInHand(1, "Fanged Flames")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Fanged Flames", giant).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe false
            }

            test("a creature that survives the damage but is destroyed later this turn is still exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Enormous Baloth") // 7/7
                    .withCardInHand(1, "Fanged Flames")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val baloth = game.findPermanent("Enormous Baloth")!!
                game.castSpell(1, "Fanged Flames", baloth).error shouldBe null
                game.resolveStack()
                withClue("4 damage doesn't kill a 7/7") { game.isOnBattlefield("Enormous Baloth") shouldBe true }

                game.castSpell(1, "Murder", baloth).error shouldBe null
                game.resolveStack()

                withClue("destroyed by Murder the same turn, it is exiled instead") {
                    game.isInExile(2, "Enormous Baloth") shouldBe true
                    game.isInGraveyard(2, "Enormous Baloth") shouldBe false
                }
            }

            test("a planeswalker that falls to 0 loyalty is exiled instead") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Jace Beleren")
                    .withCardInHand(1, "Fanged Flames")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace Beleren")!!
                game.state = game.state.updateEntity(jace) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) }
                game.castSpell(1, "Fanged Flames", jace).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Jace Beleren") shouldBe true
                game.isInGraveyard(2, "Jace Beleren") shouldBe false
            }

            test("devoid makes the card colorless") {
                cardRegistry.getCard("Fanged Flames")!!.colors.isEmpty() shouldBe true
            }
        }
    }
}
