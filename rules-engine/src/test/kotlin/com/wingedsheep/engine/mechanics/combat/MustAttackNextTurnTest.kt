package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.MarkMustAttackExecutor
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.MustAttackDefendersThisTurnComponent
import com.wingedsheep.engine.state.components.combat.MustAttackNextTurnComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackEffect
import com.wingedsheep.sdk.scripting.effects.MustAttackWindow
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * `MarkMustAttackEffect(window = MustAttackWindow.ControllersNextTurn, defender = …)` — "target
 * creature an opponent controls attacks Gideon during its controller's next turn if able" (Gideon,
 * Battle-Forged). The mark waits for the next turn of whoever controls the creature, binds every
 * combat of that turn, and ends with it. A defender that is a planeswalker binds the creature only
 * while it can attack that planeswalker at no cost (CR 508.1d); once it can't — the planeswalker has
 * left the battlefield — the creature may attack anyone or not attack (Gideon rulings).
 */
class MustAttackNextTurnTest : FunSpec({

    val walker = card("Test Taunting Walker") {
        manaCost = "{2}{W}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = "+2: Target creature an opponent controls attacks this planeswalker during its " +
            "controller's next turn if able."
        loyaltyAbility(+2) {
            val creature = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.MarkMustAttackNextTurn(creature, EffectTarget.Self)
        }
    }

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + walker)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.walkerFor(owner: EntityId): EntityId =
        putPermanentOnBattlefield(owner, "Test Taunting Walker").also { pw ->
            replaceState(state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        }

    fun GameTestDriver.activatePlusTwo(pw: EntityId, creature: EntityId) {
        submitSuccess(
            ActivateAbility(player1, pw, walker.script.activatedAbilities[0].id, listOf(ChosenTarget.Permanent(creature)))
        )
        bothPass()
    }

    fun GameTestDriver.opposingCreature(): EntityId =
        putCreatureOnBattlefield(player2, "Centaur Courser").also { removeSummoningSickness(it) }

    test("the creature must attack the planeswalker on its controller's next turn — not its controller") {
        val d = newDriver()
        val pw = d.walkerFor(d.player1)
        val theirs = d.opposingCreature()
        d.activatePlusTwo(pw, theirs)

        d.state.getEntity(theirs)?.get<MustAttackNextTurnComponent>()?.defenderIds shouldBe setOf(pw)
        // Gideon's controller's own combat doesn't use it up.
        d.passPriorityUntil(Step.END)
        d.state.getEntity(theirs)?.get<MustAttackNextTurnComponent>() shouldNotBe null

        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.state.getEntity(theirs)?.get<MustAttackNextTurnComponent>() shouldBe null
        d.state.getEntity(theirs)?.get<MustAttackDefendersThisTurnComponent>()?.defenderIds shouldBe setOf(pw)
        d.declareAttackers(d.player2, emptyMap()).error shouldContain "must attack Test Taunting Walker"
        d.declareAttackers(d.player2, mapOf(theirs to d.player1)).error shouldNotBe null
        d.declareAttackers(d.player2, mapOf(theirs to pw)).error shouldBe null
    }

    test("the published legal attack narrows the bound creature to the planeswalker and makes it mandatory") {
        val d = newDriver()
        val pw = d.walkerFor(d.player1)
        val theirs = d.opposingCreature()
        d.activatePlusTwo(pw, theirs)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)

        val declare = d.legalActions(d.player2).single { it.actionType == "DeclareAttackers" }
        declare.mandatoryAttackers.orEmpty() shouldContainExactly listOf(theirs)
        declare.attackTargetsByAttacker?.get(theirs).orEmpty() shouldContainExactly listOf(pw)
    }

    test("the requirement ends with that turn") {
        val d = newDriver()
        val pw = d.walkerFor(d.player1)
        val theirs = d.opposingCreature()
        d.activatePlusTwo(pw, theirs)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, mapOf(theirs to pw)).error shouldBe null

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        d.state.getEntity(theirs)?.get<MustAttackDefendersThisTurnComponent>() shouldBe null
        d.state.getEntity(theirs)?.get<MustAttackNextTurnComponent>() shouldBe null
    }

    test("a planeswalker that has left the battlefield binds nothing — the creature may attack anyone or stay home") {
        val d = newDriver()
        val pw = d.walkerFor(d.player1)
        val theirs = d.opposingCreature()
        d.activatePlusTwo(pw, theirs)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player2)
        d.moveToGraveyard(pw)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.legalActions(d.player2).single { it.actionType == "DeclareAttackers" }
            .mandatoryAttackers.orEmpty().contains(theirs) shouldBe false
        d.declareAttackers(d.player2, emptyMap()).error shouldBe null
    }

    test("after a change of control it binds the new controller's next turn") {
        val d = newDriver()
        val pw = d.walkerFor(d.player1)
        val theirs = d.opposingCreature()
        d.activatePlusTwo(pw, theirs)
        // Player 1 gains control of it before player 2's turn: player 2's turn no longer uses it.
        d.replaceState(d.state.updateEntity(theirs) { it.with(ControllerComponent(d.player1)) })
        d.passPriorityUntil(Step.END, d.player2)
        d.state.getEntity(theirs)?.get<MustAttackNextTurnComponent>() shouldNotBe null
        // Player 1's next turn is the one it binds — but a planeswalker its own controller controls
        // is no legal defender, so it binds nothing, and the mark is spent with that turn.
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player1)
        d.state.getEntity(theirs)?.get<MustAttackNextTurnComponent>() shouldBe null
        d.state.getEntity(theirs)?.get<MustAttackDefendersThisTurnComponent>()?.defenderIds shouldBe setOf(pw)
        d.declareAttackers(d.player1, emptyMap()).error shouldBe null
    }

    test("marked during its controller's own turn, it waits for that player's following turn") {
        val d = newDriver()
        val mine = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.removeSummoningSickness(mine)
        val effect = MarkMustAttackEffect(EffectTarget.SpecificEntity(mine), MustAttackWindow.ControllersNextTurn)
        d.replaceState(MarkMustAttackExecutor().execute(d.state, effect, EffectContext(sourceId = null, controllerId = d.player2)).state)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, emptyMap()).error shouldBe null

        d.passPriorityUntil(Step.END, d.player2)
        d.state.getEntity(mine)?.get<MustAttackNextTurnComponent>() shouldNotBe null
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player1)
        // An undirected mark: it must attack, anyone will do.
        d.declareAttackers(d.player1, emptyMap()).error shouldContain "must attack"
        d.declareAttackers(d.player1, mapOf(mine to d.player2)).error shouldBe null
    }

    test("ControllersNextCombat still takes no defender") {
        shouldThrow<IllegalArgumentException> {
            MarkMustAttackEffect(EffectTarget.Self, MustAttackWindow.ControllersNextCombat, EffectTarget.Self)
        }
    }
})
