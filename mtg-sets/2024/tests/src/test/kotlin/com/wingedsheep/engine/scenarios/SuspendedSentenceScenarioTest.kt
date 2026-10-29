package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SuspendedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Suspended Sentence (DSC 25).
 *
 * "Destroy target creature an opponent controls. That player loses 3 life. Exile Suspended
 *  Sentence with three time counters on it.
 *  Suspend 3—{1}{B}"
 */
class SuspendedSentenceScenarioTest : ScenarioTestBase() {
    init {
        test("destroys the creature, its controller loses 3, and the card lands suspended with three time counters") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Suspended Sentence")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Suspended Sentence", bears)
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
            withClue("exiled, not put into the graveyard") {
                game.isInExile(1, "Suspended Sentence") shouldBe true
                game.isInGraveyard(1, "Suspended Sentence") shouldBe false
            }
            val sentence = game.state.getExile(game.player1Id)
                .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Suspended Sentence" }
            game.state.getEntity(sentence)?.get<CountersComponent>()?.getCount(CounterType.TIME) shouldBe 3
            game.state.getEntity(sentence)?.has<SuspendedComponent>() shouldBe true
        }

        test("an indestructible creature survives, but its controller still loses 3 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Suspended Sentence")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(2, "Darksteel Myr")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Suspended Sentence", game.findPermanent("Darksteel Myr")!!)
            game.resolveStack()

            game.isOnBattlefield("Darksteel Myr") shouldBe true
            game.getLifeTotal(2) shouldBe 17
            game.isInExile(1, "Suspended Sentence") shouldBe true
        }
    }
}
