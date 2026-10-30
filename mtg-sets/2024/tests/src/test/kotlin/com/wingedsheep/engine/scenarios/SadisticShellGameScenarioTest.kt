package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsc.cards.SadisticShellGame
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Sadistic Shell Game (DSC #24) — {4}{B} Sorcery.
 * "Starting with the next opponent in turn order, each player chooses a creature you don't control.
 *  Destroy the chosen creatures."
 *
 * Four-player free-for-all with seat 2 casting, so the choices run seat 3, seat 4, seat 1, then the
 * caster last — not seat order and not starting with the caster.
 */
class SadisticShellGameScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver().apply { registerCards(TestCards.all + SadisticShellGame) }
        val players = d.initMultiplayer(List(4) { Deck.of("Swamp" to 40) }, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players
    }

    fun GameTestDriver.cast(caster: EntityId) {
        val spell = putCardInHand(caster, "Sadistic Shell Game")
        giveColorlessMana(caster, 4)
        giveMana(caster, Color.BLACK, 1)
        castSpell(caster, spell).error shouldBe null
        var guard = 0
        while (stackSize > 0 && !isPaused && guard++ < 10) passPriority(state.priorityPlayerId!!)
    }

    test("starting with the next opponent, each player picks a creature the caster doesn't control; all are destroyed") {
        val (d, players) = setup()
        val (p1, p2, p3, p4) = players
        d.activePlayer shouldBe p2
        val casters = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val bears = listOf(p1, p3, p4).associateWith { d.putCreatureOnBattlefield(it, "Grizzly Bears") }

        d.cast(p2)

        // Each chooser takes the next opponent's creature in seat order, so all three die.
        val pickFor = mapOf(p3 to bears.getValue(p4), p4 to bears.getValue(p1), p1 to bears.getValue(p3), p2 to bears.getValue(p3))
        val choosers = mutableListOf<EntityId>()
        while (d.pendingDecision != null) {
            val decision = d.pendingDecision as SelectCardsDecision
            choosers += decision.playerId
            withClue("the caster's own creature is never offered") {
                decision.options shouldContainExactlyInAnyOrder bears.values
            }
            d.submitCardSelection(decision.playerId, listOf(pickFor.getValue(decision.playerId)))
        }

        choosers shouldBe listOf(p3, p4, p1, p2)
        bears.forEach { (owner, bear) -> d.getGraveyard(owner).contains(bear) shouldBe true }
        d.state.getBattlefield().contains(casters) shouldBe true
    }

    test("a creature chosen by several players is destroyed, the unchosen survive") {
        val (d, players) = setup()
        val (p1, p2, p3, p4) = players
        val target = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val spared = listOf(p3, p4).map { d.putCreatureOnBattlefield(it, "Grizzly Bears") }

        d.cast(p2)
        while (d.pendingDecision != null) {
            val decision = d.pendingDecision as SelectCardsDecision
            d.submitCardSelection(decision.playerId, listOf(target))
        }

        d.getGraveyard(p1).contains(target) shouldBe true
        spared.forEach { d.state.getBattlefield().contains(it) shouldBe true }
    }
})
