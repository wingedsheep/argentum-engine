package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dionus, Elvish Archdruid — "Elves you control have 'Whenever this creature becomes tapped during
 * your turn, untap it and put a +1/+1 counter on it. This ability triggers only once each turn.'"
 *
 * The granted trigger composes three gates: the Elf filter on the grant, the "during your turn"
 * trigger restriction, and the once-each-turn cap keyed per granting Dionus.
 */
class DionusElvishArchdruidScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.tapped(id: EntityId): Boolean =
        state.getEntity(id)?.has<TappedComponent>() == true

    private fun TestGame.tapElves(playerId: EntityId, elves: EntityId) =
        execute(ActivateAbility(playerId, elves, cardRegistry.requireCard("Llanowar Elves").activatedAbilities.single().id))

    init {
        test("an Elf tapped during your turn untaps and gets a counter, only once each turn") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dionus, Elvish Archdruid")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elves = g.findPermanent("Llanowar Elves")!!

            g.tapElves(g.player1Id, elves).error shouldBe null
            g.state.stack.size shouldBe 1
            g.resolveStack()

            withClue("first tap this turn: untapped with a +1/+1 counter") {
                g.tapped(elves) shouldBe false
                g.plusOnes(elves) shouldBe 1
            }

            g.tapElves(g.player1Id, elves).error shouldBe null
            withClue("the granted ability triggers only once each turn") {
                g.state.stack.size shouldBe 0
                g.tapped(elves) shouldBe true
                g.plusOnes(elves) shouldBe 1
            }
        }

        test("an Elf tapped during an opponent's turn doesn't trigger") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dionus, Elvish Archdruid")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elves = g.findPermanent("Llanowar Elves")!!

            g.passPriority()
            g.tapElves(g.player1Id, elves).error shouldBe null

            g.state.stack.size shouldBe 0
            g.tapped(elves) shouldBe true
            g.plusOnes(elves) shouldBe 0
        }

        test("a non-Elf creature you control doesn't have the ability") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dionus, Elvish Archdruid")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            g.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            g.resolveStack()

            val bears = g.findPermanent("Grizzly Bears")!!
            g.tapped(bears) shouldBe true
            g.plusOnes(bears) shouldBe 0
        }
    }
}
