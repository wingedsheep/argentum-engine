package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pc2.cards.SakashimasStudent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sakashima's Student — {2}{U}{U} Creature — Human Ninja 0/0
 * Ninjutsu {1}{U}
 * You may have this creature enter as a copy of any creature on the battlefield, except it's a
 * Ninja in addition to its other creature types.
 *
 * Pins the composition: ninjutsu's effect-driven entry asks the [EntersAsCopy] choice and replays
 * the move, so the copy still arrives tapped and attacking (and deals combat damage); the returned
 * attacker is in hand and can't be chosen; declining leaves a 0/0 that dies.
 */
class SakashimasStudentScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + SakashimasStudent)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Attack with [attackers] unblocked and stop with player 1 holding priority in declare blockers. */
    fun GameTestDriver.attackUnblocked(attackers: List<EntityId>) {
        val opponent = getOpponent(player1)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, attackers.associateWith { opponent }).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareBlockers(opponent, emptyMap()).outcome shouldBe Outcome.Done
        var guard = 0
        while (state.priorityPlayerId != player1 && state.step == Step.DECLARE_BLOCKERS && guard++ < 4) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    fun GameTestDriver.ninjutsu(student: EntityId, returned: EntityId) {
        giveMana(player1, Color.BLUE, 2)
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = student,
                abilityId = ninjutsuAbility("{1}{U}").id,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(returned)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
    }

    /** Resolve the ninjutsu ability, answering the copy prompt with [choose] (empty = decline). */
    fun GameTestDriver.resolveNinjutsu(returned: EntityId, choose: (SelectCardsDecision) -> List<EntityId>) {
        var prompts = 0
        var guard = 0
        while (guard++ < 20 && (state.stack.isNotEmpty() || pendingDecision != null)) {
            when (val pd = pendingDecision) {
                is SelectCardsDecision -> {
                    prompts++
                    pd.options shouldNotContain returned
                    submitCardSelection(player1, choose(pd)).error shouldBe null
                }
                null -> bothPass()
                else -> error("unexpected decision $pd")
            }
        }
        prompts shouldBe 1
    }

    test("ninjutsu in as a copy: a tapped, attacking Ninja copy that deals combat damage") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions").also(d::removeSummoningSickness)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also(d::removeSummoningSickness)
        d.attackUnblocked(listOf(lions, bears))
        d.ninjutsu(d.putCardInHand(d.player1, SakashimasStudent.name), lions)

        d.resolveNinjutsu(lions) { pd ->
            pd.options shouldContain bears
            listOf(bears)
        }

        d.getHand(d.player1).map { d.state.getEntity(it)?.get<CardComponent>()?.name } shouldContain "Savannah Lions"
        val student = d.getPermanents(d.player1).single {
            it != bears && d.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
        val projected = d.state.projectedState
        projected.getPower(student) shouldBe 2
        projected.getToughness(student) shouldBe 2
        projected.hasSubtype(student, "Bear") shouldBe true
        projected.hasSubtype(student, "Ninja") shouldBe true
        projected.hasSubtype(bears, "Ninja") shouldBe false
        d.state.getEntity(student)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(student)!!.get<AttackingComponent>().shouldNotBeNull()

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opponent) shouldBe 16
    }

    test("declining the copy leaves a 0/0 that dies") {
        val d = driver()
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions").also(d::removeSummoningSickness)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also(d::removeSummoningSickness)
        d.attackUnblocked(listOf(lions, bears))
        d.ninjutsu(d.putCardInHand(d.player1, SakashimasStudent.name), lions)

        d.resolveNinjutsu(lions) { emptyList() }

        d.findPermanent(d.player1, SakashimasStudent.name) shouldBe null
        d.getGraveyardCardNames(d.player1) shouldContain SakashimasStudent.name
    }
})
