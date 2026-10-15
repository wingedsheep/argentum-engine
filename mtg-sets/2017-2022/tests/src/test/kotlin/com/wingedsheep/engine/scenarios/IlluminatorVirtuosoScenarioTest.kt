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
 * Illuminator Virtuoso (SNC #17) — "Double strike. Whenever this creature becomes the target of a
 * spell you control, it connives."
 *
 * Pins the trigger (your own spell targeting it) and that the connive counter lands on the
 * Virtuoso itself.
 */
class IlluminatorVirtuosoScenarioTest : ScenarioTestBase() {

    init {
        test("your spell targeting it makes it connive; a nonland discard grows it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Illuminator Virtuoso")
                .withCardInHand(1, "Giant Growth")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val virtuoso = game.findPermanent("Illuminator Virtuoso")!!
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()

            game.castSpell(1, "Giant Growth", virtuoso).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bears))
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Island") shouldBe true
            game.state.getEntity(virtuoso)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
    }
}
