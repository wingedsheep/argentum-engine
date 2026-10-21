package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Tyvar the Bellicose — "Whenever one or more Elves you control attack, they gain deathtouch until
 * end of turn. Each creature you control has 'Whenever a mana ability of this creature resolves,
 * put a number of +1/+1 counters on it equal to the amount of mana this creature produced. This
 * ability triggers only once each turn.'"
 */
class TyvarTheBellicoseScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.tapElves(playerId: EntityId, elves: EntityId) =
        execute(ActivateAbility(playerId, elves, cardRegistry.requireCard("Llanowar Elves").activatedAbilities.single().id))

    init {
        test("a creature you control tapped for mana gets a +1/+1 counter per mana it produced") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tyvar the Bellicose")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elves = g.findPermanent("Llanowar Elves")!!

            g.tapElves(g.player1Id, elves).error shouldBe null
            g.state.stack.size shouldBe 1
            g.resolveStack()

            g.plusOnes(elves) shouldBe 1

            // The granted ability triggers only once each turn: untapped and tapped again, nothing.
            g.state = g.state.updateEntity(elves) { it.without<TappedComponent>() }
            g.tapElves(g.player1Id, elves).error shouldBe null
            g.state.stack.size shouldBe 0
            g.plusOnes(elves) shouldBe 1
        }

        test("a Tyvar that leaves and returns grants a new ability that can trigger again this turn") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tyvar the Bellicose")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardInHand(1, "Cloudshift")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elves = g.findPermanent("Llanowar Elves")!!

            g.tapElves(g.player1Id, elves).error shouldBe null
            g.resolveStack()
            g.plusOnes(elves) shouldBe 1

            // Flickered, Tyvar is a new object; the ability it now grants is a different one.
            g.castSpell(1, "Cloudshift", targetId = g.findPermanent("Tyvar the Bellicose")!!).error shouldBe null
            g.resolveStack()

            g.state = g.state.updateEntity(elves) { it.without<TappedComponent>() }
            g.tapElves(g.player1Id, elves).error shouldBe null
            g.state.stack.size shouldBe 1
            g.resolveStack()
            g.plusOnes(elves) shouldBe 2
        }

        test("an opponent's creature doesn't have the ability") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tyvar the Bellicose")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elves = g.findPermanent("Llanowar Elves")!!

            g.passPriority()
            g.tapElves(g.player2Id, elves).error shouldBe null

            g.state.stack.size shouldBe 0
            g.plusOnes(elves) shouldBe 0
        }

        test("attacking Elves gain deathtouch; a non-Elf attacking alongside them doesn't") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tyvar the Bellicose")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            g.declareAttackers(
                mapOf("Tyvar the Bellicose" to 2, "Llanowar Elves" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            g.resolveStack()

            val projected = g.state.projectedState
            projected.hasKeyword(g.findPermanent("Tyvar the Bellicose")!!, Keyword.DEATHTOUCH) shouldBe true
            projected.hasKeyword(g.findPermanent("Llanowar Elves")!!, Keyword.DEATHTOUCH) shouldBe true
            projected.hasKeyword(g.findPermanent("Grizzly Bears")!!, Keyword.DEATHTOUCH) shouldBe false
        }
    }
}
