package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Petrifying Meddler — "When you cast this spell, tap up to one target creature and put a stun
 * counter on it." The cast trigger resolves before the creature spell, even if the spell is
 * countered, and "up to one" allows choosing no target.
 */
class PetrifyingMeddlerScenarioTest : ScenarioTestBase() {
    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Petrifying Meddler")
        .withCardInHand(1, "Counterspell")
        .withLandsOnBattlefield(1, "Island", 7)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.get<TappedComponent>() != null
    private fun TestGame.stun(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    init {
        test("cast trigger taps the target and adds a stun counter, even if the spell is countered") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Petrifying Meddler").error shouldBe null
            game.selectTargets(listOf(bears)).error shouldBe null
            game.castSpellTargetingStackSpell(1, "Counterspell", "Petrifying Meddler").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Petrifying Meddler") shouldBe true
            game.tapped(bears) shouldBe true
            game.stun(bears) shouldBe 1
        }

        test("up to one target allows choosing no creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Petrifying Meddler").error shouldBe null
            game.skipTargets().error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Petrifying Meddler") shouldBe true
            game.tapped(bears) shouldBe false
            game.stun(bears) shouldBe 0
        }
    }
}
