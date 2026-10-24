package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.AcademicProbation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Academic Probation (STX #7).
 *
 * The load-bearing claim of the first mode is that the chosen name outlives the spell: the ban is
 * checked on the opponent's own turn, long after the resolution that chose the name is gone.
 */
class AcademicProbationScenarioTest : FunSpec({

    val pinger = card("Probation Test Pinger") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.GainLife(1)
        }
    }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AcademicProbation, pinger))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castsBolt(player: EntityId, bolt: EntityId): Boolean {
        giveMana(player, Color.RED, 1)
        return legalActions(player).any { (it.action as? CastSpell)?.cardId == bolt }
    }

    test("opponents can't cast spells with the chosen name until your next turn") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val oppBolt = d.putCardInHand(opp, "Lightning Bolt")
        val oppGrowth = d.putCardInHand(opp, "Giant Growth")
        d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.giveMana(me, Color.WHITE, 2)
        val probation = d.putCardInHand(me, "Academic Probation")
        d.submit(CastSpell(playerId = me, cardId = probation, chosenModes = listOf(0))).error shouldBe null
        d.bothPass()
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe me
        d.submitDecision(me, OptionChosenResponse(decision.id, decision.options.indexOf("Lightning Bolt")))
            .error shouldBe null

        // The opponent's own turn: the name is still banned, other spells are not.
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.castsBolt(opp, oppBolt) shouldBe false
        d.giveMana(opp, Color.GREEN, 1)
        d.legalActions(opp).any { (it.action as? CastSpell)?.cardId == oppGrowth } shouldBe true

        // It lifts as your next turn begins.
        d.passPriorityUntil(Step.UPKEEP, me)
        d.submit(PassPriority(me)).error shouldBe null
        d.castsBolt(opp, oppBolt) shouldBe true
    }

    test("the caster can still cast spells with the chosen name") {
        val d = setup()
        val me = d.activePlayer!!
        val myBolt = d.putCardInHand(me, "Lightning Bolt")

        d.giveMana(me, Color.WHITE, 2)
        val probation = d.putCardInHand(me, "Academic Probation")
        d.submit(CastSpell(playerId = me, cardId = probation, chosenModes = listOf(0))).error shouldBe null
        d.bothPass()
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(me, OptionChosenResponse(decision.id, decision.options.indexOf("Lightning Bolt")))
            .error shouldBe null

        d.castsBolt(me, myBolt) shouldBe true
    }

    test("the targeted permanent can't attack, block or activate until your next turn") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val victim = d.putCreatureOnBattlefield(opp, pinger.name)

        d.giveMana(me, Color.WHITE, 2)
        val probation = d.putCardInHand(me, "Academic Probation")
        d.submit(
            CastSpell(
                playerId = me,
                cardId = probation,
                targets = listOf(ChosenTarget.Permanent(victim)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(victim)))
            )
        ).error shouldBe null
        d.bothPass()

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.removeSummoningSickness(victim)
        d.state.projectedState.cantAttack(victim) shouldBe true
        d.state.projectedState.cantBlock(victim) shouldBe true
        d.legalActions(opp).any { (it.action as? ActivateAbility)?.sourceId == victim } shouldBe false

        d.passPriorityUntil(Step.UPKEEP, me)
        d.state.projectedState.cantAttack(victim) shouldBe false
        d.state.projectedState.cantBlock(victim) shouldBe false
    }
})
