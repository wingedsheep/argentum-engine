package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cursed Wombat (MH3) — "{2}{B}{G}: Adapt 2." and permanents you control have "Whenever one or more
 * +1/+1 counters are put on this permanent, put an additional +1/+1 counter on it. This ability
 * triggers only once each turn."
 */
class CursedWombatScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.settle() {
        var guard = 0
        while (guard++ < 20) {
            when (val d = getPendingDecision()) {
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay().error shouldBe null
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("unexpected decision $d")
            }
        }
    }

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Cursed Wombat")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardsInHand(1, "Battlegrowth", 3)
        .withLandsOnBattlefield(1, "Forest", 5)
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.adapt() {
        val wombat = findPermanent("Cursed Wombat")!!
        val ability = cardRegistry.requireCard("Cursed Wombat").script.activatedAbilities[0].id
        execute(ActivateAbility(playerId = player1Id, sourceId = wombat, abilityId = ability)).error shouldBe null
        settle()
    }

    private fun TestGame.grow(id: EntityId) {
        castSpell(1, "Battlegrowth", targetId = id).error shouldBe null
        settle()
    }

    init {
        test("adapt 2 gets one additional counter from the granted trigger") {
            val game = game()
            val wombat = game.findPermanent("Cursed Wombat")!!
            game.adapt()
            withClue("two from adapt plus one additional") { game.plusOnes(wombat) shouldBe 3 }
            game.state.projectedState.getPower(wombat) shouldBe 5
            game.state.projectedState.getToughness(wombat) shouldBe 6
        }

        test("another permanent you control gets the bonus only once each turn") {
            val game = game()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.grow(bears)
            withClue("one from Battlegrowth plus one additional") { game.plusOnes(bears) shouldBe 2 }
            game.grow(bears)
            withClue("the granted ability already triggered this turn") { game.plusOnes(bears) shouldBe 3 }
        }

        test("an opponent's permanent does not have the ability") {
            val game = game()
            val giant = game.findPermanent("Hill Giant")!!
            game.grow(giant)
            game.plusOnes(giant) shouldBe 1
        }
    }
}
