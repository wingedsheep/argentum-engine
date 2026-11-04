package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Crafty Cutpurse (RIX #33) — Flash. When this creature enters, each token that would be created
 * under an opponent's control this turn is created under your control instead.
 *
 * Flashed in response to an opponent's Raise the Alarm, it steals both Soldiers; the redirect
 * applies before the opponent's Anointed Procession, which then no longer doubles them (ruling).
 */
class CraftyCutpurseScenarioTest : ScenarioTestBase() {

    private fun TestGame.soldiersOf(playerId: EntityId) = state.getBattlefield().filter { id ->
        val container = state.getEntity(id) ?: return@filter false
        container.has<TokenComponent>() && state.projectedState.getController(id) == playerId
    }

    private fun TestGame.flashCutpurseInResponseToRaiseTheAlarm() {
        castSpell(2, "Raise the Alarm").error shouldBe null
        passPriority().error shouldBe null // Bob passes; Alice may respond
        castSpell(1, "Crafty Cutpurse").error shouldBe null
        resolveStack()
    }

    init {
        test("flashed in response, it takes the opponent's tokens — Alice controls and owns both Soldiers") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Crafty Cutpurse")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(2, "Raise the Alarm")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.flashCutpurseInResponseToRaiseTheAlarm()

            val soldiers = game.soldiersOf(game.player1Id)
            soldiers.size shouldBe 2
            soldiers.forEach { game.state.getEntity(it)!!.get<CardComponent>()!!.ownerId shouldBe game.player1Id }
            game.soldiersOf(game.player2Id).shouldBeEmpty()
        }

        test("killed in response to its enters trigger, it still takes the tokens — the grant outlives it") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Crafty Cutpurse")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(2, "Raise the Alarm")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Raise the Alarm").error shouldBe null
            game.passPriority().error shouldBe null // Bob passes; Alice responds
            game.castSpell(1, "Crafty Cutpurse").error shouldBe null
            game.passPriority().error shouldBe null // Alice
            game.passPriority().error shouldBe null // Bob — Cutpurse resolves, its trigger goes on the stack
            val cutpurse = game.findPermanent("Crafty Cutpurse")!!
            withClue("Raise the Alarm and the Cutpurse trigger are on the stack") { game.state.stack.size shouldBe 2 }

            // Bob kills the Cutpurse with its enters trigger still on the stack.
            game.castSpell(2, "Lightning Bolt", cutpurse).error shouldBe null
            game.resolveStack()

            withClue("the Cutpurse died") { game.findPermanent("Crafty Cutpurse") shouldBe null }
            game.soldiersOf(game.player1Id).size shouldBe 2
            game.soldiersOf(game.player2Id).shouldBeEmpty()
        }

        test("the opponent's Anointed Procession no longer applies to the stolen tokens") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Crafty Cutpurse")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(2, "Raise the Alarm")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withCardOnBattlefield(2, "Anointed Procession")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.flashCutpurseInResponseToRaiseTheAlarm()

            withClue("two Soldiers, not four") { game.soldiersOf(game.player1Id).size shouldBe 2 }
            game.soldiersOf(game.player2Id).shouldBeEmpty()
        }

        test("without the Cutpurse, the opponent keeps their tokens") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(2, "Raise the Alarm")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            game.soldiersOf(game.player2Id).size shouldBe 2
            game.soldiersOf(game.player1Id).shouldBeEmpty()
        }
    }
}
