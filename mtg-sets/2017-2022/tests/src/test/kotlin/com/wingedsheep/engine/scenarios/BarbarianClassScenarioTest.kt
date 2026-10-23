package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.BarbarianClass
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Barbarian Class (AFR #131): level 1 rolls an extra die and ignores the lowest, level 2 pumps a
 * creature once per roll instruction, level 3 gives your creatures haste.
 */
class BarbarianClassScenarioTest : FunSpec({
    val d20 = card("Test d20 Roller") {
        typeLine = "Artifact"
        activatedAbility { effect = Effects.RollDice(DynamicAmount.Fixed(1), sides = 20) }
    }

    fun setup(level: Int): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(BarbarianClass, d20))
            it.initMirrorMatch(Deck.of("Mountain" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        d.putPermanentOnBattlefield(d.activePlayer!!, "Barbarian Class", classLevel = level)
        d.replaceState(d.state.copy(rng = GameRng.seeded(2021)))
        return d to d.activePlayer!!
    }

    fun GameTestDriver.roll(player: EntityId): ExecutionResult {
        val source = putPermanentOnBattlefield(player, d20.name)
        submit(ActivateAbility(player, source, d20.script.activatedAbilities.single().id)).outcome shouldBe Outcome.Done
        return bothPass()
    }

    test("level 1: a d20 is rolled as two, the lower is ignored, and nothing else happens") {
        val (d, player) = setup(1)
        val rolls = d.roll(player).events.filterIsInstance<DieRolledEvent>()
        rolls.size shouldBe 2
        rolls.single { it.ignored }.result shouldBe rolls.minOf { it.result }
        d.state.stack.size shouldBe 0
    }

    test("level 2: each roll gives target creature you control +2/+0 and menace until end of turn") {
        val (d, player) = setup(2)
        val courser = d.putPermanentOnBattlefield(player, "Centaur Courser")
        d.putPermanentOnBattlefield(player, "Savannah Lions")
        d.roll(player)
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(player, listOf(courser)).error shouldBe null
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.state.projectedState.getPower(courser) shouldBe 5
        d.state.projectedState.getToughness(courser) shouldBe 3
        d.state.projectedState.hasKeyword(courser, Keyword.MENACE) shouldBe true
    }

    test("level 3: creatures you control have haste, and not before") {
        for ((level, haste) in listOf(2 to false, 3 to true)) {
            val (d, player) = setup(level)
            val lions = d.putPermanentOnBattlefield(player, "Savannah Lions")
            val theirs = d.putPermanentOnBattlefield(d.getOpponent(player), "Savannah Lions")
            d.state.projectedState.hasKeyword(lions, Keyword.HASTE) shouldBe haste
            d.state.projectedState.hasKeyword(theirs, Keyword.HASTE) shouldBe false
        }
    }
})
