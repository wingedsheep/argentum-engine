package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.DealtDamageToThisGameComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `TurnTracker.DAMAGE_RECEIVED_FROM_SOURCE` — the damage the ability's own source has dealt to a
 * player this turn, combat and noncombat summed, restarting each turn ("if it has dealt 10 or more
 * damage to that player this turn" — Vessel of the All-Consuming).
 */
class SourceDamageToPlayerThisTurnTest : FunSpec({

    // 3/3; "{0}: this deals 2 damage to target player"; "Whenever this deals damage to a player,
    // if it has dealt 5 or more damage to that player this turn, you gain 10 life."
    val tally = card("Test Damage Tally") {
        manaCost = "{1}"
        typeLine = "Creature — Shaman"
        power = 3
        toughness = 3
        activatedAbility {
            cost = Costs.Free
            val victim = target(Targets.Player)
            effect = Effects.DealDamage(2, victim)
        }
        triggeredAbility {
            trigger = Triggers.self.dealsDamage(Recipient.AnyPlayer)
            interveningIf = Conditions.CompareAmounts(
                DynamicAmounts.damageDealtBySourceThisTurn(), ComparisonOperator.GTE, 5
            )
            effect = Effects.GainLife(10)
        }
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(tally))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.ping(controller: EntityId, source: EntityId, victim: EntityId) {
        submit(
            ActivateAbility(
                playerId = controller,
                sourceId = source,
                abilityId = tally.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(victim))
            )
        ).error shouldBe null
        var guard = 0
        while (guard++ < 10 && state.stack.isNotEmpty()) bothPass()
    }

    test("combat and noncombat damage to the same player add up within the turn") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(me, "Test Damage Tally")
        d.removeSummoningSickness(creature)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(creature), them).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("3 combat damage is under the gate") { d.getLifeTotal(me) shouldBe 20 }

        d.ping(me, creature, them)
        withClue("3 + 2 = 5 this turn meets it") { d.getLifeTotal(me) shouldBe 30 }
        d.state.getEntity(creature)!!.get<DealtDamageToThisGameComponent>()!!
            .damageDealtToOnTurn(them, d.state.turnNumber) shouldBe 5
    }

    test("damage to another player is tallied separately") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(me, "Test Damage Tally")

        d.ping(me, creature, them)
        d.ping(me, creature, me)
        d.ping(me, creature, me)
        withClue("2 to them and 4 to me: neither player has taken 5 from it") {
            d.getLifeTotal(me) shouldBe 16
        }
    }

    test("the tally restarts on a new turn") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val creature = d.putCreatureOnBattlefield(me, "Test Damage Tally")

        d.ping(me, creature, them)
        d.ping(me, creature, them)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, them)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        d.ping(me, creature, them)
        withClue("4 last turn and 2 this turn never reach 5 in one turn") {
            d.getLifeTotal(me) shouldBe 20
        }
        d.state.getEntity(creature)!!.get<DealtDamageToThisGameComponent>()!!
            .damageDealtToOnTurn(them, d.state.turnNumber) shouldBe 2
    }
})
