package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Contagion Clasp (DDF #63) — {2} Artifact.
 *
 *   When this artifact enters, put a -1/-1 counter on target creature.
 *   {4}, {T}: Proliferate.
 */
class ContagionClaspScenarioTest : ScenarioTestBase() {

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("enters putting a -1/-1 counter on a creature, then {4},{T} proliferates it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Contagion Clasp")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Contagion Clasp").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            count(game, bears, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1

            val clasp = game.findPermanent("Contagion Clasp")!!
            val abilityId = cardRegistry.getCard("Contagion Clasp")!!.activatedAbilities.first().id
            game.execute(ActivateAbility(game.player1Id, clasp, abilityId)).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) {
                game.submitManaSourcesAutoPay()
            }
            game.resolveStack()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
