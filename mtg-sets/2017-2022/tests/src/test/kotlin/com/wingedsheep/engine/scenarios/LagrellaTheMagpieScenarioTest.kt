package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lagrella, the Magpie (SNC #196) — {G}{W}{U} Legendary Creature — Human Soldier, 2/3
 *
 *   When Lagrella enters, exile any number of other target creatures controlled by different
 *   players until Lagrella leaves the battlefield. When an exiled card enters under your control
 *   this way, put two +1/+1 counters on it.
 *
 * Proves the linked exile of several targets, the return under each owner's control, and that
 * only the card coming back under Lagrella's controller's control gets the two counters.
 */
class LagrellaTheMagpieScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Lagrella, the Magpie") {

            test("exiles a creature per player; on leaving, yours returns with two counters, theirs without") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lagrella, the Magpie")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Lagrella, the Magpie").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(bears, giant)).error shouldBe null
                game.resolveStack()

                withClue("both creatures are exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.state.getExile(game.player1Id).contains(bears) shouldBe true
                    game.state.getExile(game.player2Id).contains(giant) shouldBe true
                }

                val lagrella = game.findPermanent("Lagrella, the Magpie")!!
                game.castSpell(1, "Lightning Bolt", lagrella).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Lagrella, the Magpie") shouldBe false
                val returnedBears = game.findPermanent("Grizzly Bears")!!
                val returnedGiant = game.findPermanent("Hill Giant")!!
                withClue("each card returns under its owner's control") {
                    game.state.getEntity(returnedBears)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
                    game.state.getEntity(returnedGiant)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                }
                withClue("only the card entering under your control gets two +1/+1 counters") {
                    game.plusOnes(returnedBears) shouldBe 2
                    game.plusOnes(returnedGiant) shouldBe 0
                }
            }

            test("an opponent's creature alone returns without counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lagrella, the Magpie")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Lagrella, the Magpie").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Hill Giant") shouldBe false

                val lagrella = game.findPermanent("Lagrella, the Magpie")!!
                game.castSpell(1, "Lightning Bolt", lagrella).error shouldBe null
                game.resolveStack()

                val returnedGiant = game.findPermanent("Hill Giant")!!
                game.state.getEntity(returnedGiant)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                game.plusOnes(returnedGiant) shouldBe 0
                game.state.stack.isEmpty() shouldBe true
            }
        }
    }
}
