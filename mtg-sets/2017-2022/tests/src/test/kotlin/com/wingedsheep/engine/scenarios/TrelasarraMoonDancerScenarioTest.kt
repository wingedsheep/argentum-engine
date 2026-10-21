package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Trelasarra, Moon Dancer (AFR #236) — "Whenever you gain life, put a +1/+1 counter on Trelasarra
 * and scry 1."
 */
class TrelasarraMoonDancerScenarioTest : ScenarioTestBase() {

    init {
        test("gaining life puts a +1/+1 counter on her and scries 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Trelasarra, Moon Dancer")
                .withCardInHand(1, "Sacred Nectar")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val trelasarra = game.findPermanent("Trelasarra, Moon Dancer")!!

            game.castSpell(1, "Sacred Nectar").error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 1
            game.selectCards(decision.options)
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 24
            game.state.getEntity(trelasarra)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(trelasarra) shouldBe 3
            game.librarySize(1) shouldBe 2
        }
    }
}
