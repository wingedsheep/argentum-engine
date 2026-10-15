package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ledger Shredder (SNC #46) — "Whenever a player casts their second spell each turn, this creature
 * connives."
 *
 * Pins "a player": an opponent's second spell of the turn makes your Shredder connive, and their
 * first spell does not.
 */
class LedgerShredderScenarioTest : ScenarioTestBase() {

    init {
        test("an opponent's second spell in a turn makes Ledger Shredder connive") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ledger Shredder")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardsInHand(2, "Giant Growth", 2)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val shredder = game.findPermanent("Ledger Shredder")!!
            val opposingBears = game.findPermanent("Grizzly Bears")!!
            val myBears = game.findCardsInHand(1, "Grizzly Bears").single()

            game.castSpell(2, "Giant Growth", opposingBears).error shouldBe null
            game.state.stack.size shouldBe 1 // first spell: no trigger

            game.castSpell(2, "Giant Growth", opposingBears).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(myBears))
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Island") shouldBe true
            game.state.getEntity(shredder)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
