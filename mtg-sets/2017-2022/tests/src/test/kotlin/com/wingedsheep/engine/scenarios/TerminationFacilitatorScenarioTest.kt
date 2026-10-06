package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j22.cards.TerminationFacilitator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Termination Facilitator (J22) —
 * {T}: Put a bounty counter on target creature or planeswalker. Activate only as a sorcery.
 * Whenever a creature or planeswalker an opponent controls with a bounty counter on it is dealt
 * damage, destroy it.
 */
class TerminationFacilitatorScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + TerminationFacilitator)
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bounty(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.BOUNTY) ?: 0

    fun GameTestDriver.markBounty(facilitator: EntityId, target: EntityId) {
        submit(
            ActivateAbility(
                playerId = activePlayer!!,
                sourceId = facilitator,
                abilityId = TerminationFacilitator.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).error shouldBe null
        while (state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.boltAndResolve(caster: EntityId, victim: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        submit(
            CastSpell(caster, bolt, targets = listOf(ChosenTarget.Permanent(victim)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        var guard = 0
        while (guard++ < 10 && state.stack.isNotEmpty()) bothPass()
    }

    // A 5/5 survives a Bolt, so only the bounty trigger can destroy it.
    val bigCreature = "Force of Nature"

    test("the bounty ability is sorcery-speed") {
        TerminationFacilitator.activatedAbilities.first().timing shouldBe TimingRule.SorcerySpeed
    }

    test("a bountied creature an opponent controls is destroyed when it is dealt damage") {
        val d = setup()
        val me = d.activePlayer!!
        val facilitator = d.putCreatureOnBattlefield(me, "Termination Facilitator")
        d.removeSummoningSickness(facilitator)
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(me), bigCreature)

        d.markBounty(facilitator, theirs)
        d.bounty(theirs) shouldBe 1

        d.boltAndResolve(me, theirs)

        d.getGraveyard(d.getOpponent(me)) shouldContain theirs
    }

    test("damage to an unmarked opposing creature does nothing extra") {
        val d = setup()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Termination Facilitator")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(me), bigCreature)

        d.boltAndResolve(me, theirs)

        d.state.getBattlefield() shouldContain theirs
    }

    test("your own bountied creature is not destroyed when dealt damage") {
        val d = setup()
        val me = d.activePlayer!!
        val facilitator = d.putCreatureOnBattlefield(me, "Termination Facilitator")
        d.removeSummoningSickness(facilitator)
        val mine = d.putCreatureOnBattlefield(me, bigCreature)

        d.markBounty(facilitator, mine)
        d.bounty(mine) shouldBe 1

        d.boltAndResolve(me, mine)

        d.state.getBattlefield() shouldContain mine
        d.getGraveyard(me) shouldNotContain mine
    }

    test("the trigger works whoever deals the damage, even with the opponent's own spell") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val facilitator = d.putCreatureOnBattlefield(me, "Termination Facilitator")
        d.removeSummoningSickness(facilitator)
        val theirs = d.putCreatureOnBattlefield(opp, bigCreature)
        d.markBounty(facilitator, theirs)

        // Opponent bolts their own bountied creature at instant speed, holding priority on my turn.
        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.passPriority(me)
        d.submit(
            CastSpell(opp, bolt, targets = listOf(ChosenTarget.Permanent(theirs)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        var guard = 0
        while (guard++ < 10 && d.state.stack.isNotEmpty()) d.bothPass()

        d.getGraveyard(opp) shouldContain theirs
    }
})
