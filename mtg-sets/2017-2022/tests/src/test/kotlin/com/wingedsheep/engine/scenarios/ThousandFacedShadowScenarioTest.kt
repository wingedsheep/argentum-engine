package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.ThousandFacedShadow
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Thousand-Faced Shadow — {U} Creature — Human Ninja 1/1
 * Ninjutsu {2}{U}{U}; Flying
 * When this creature enters from your hand, if it's attacking, create a token that's a copy of
 * another target attacking creature. The token enters tapped and attacking.
 *
 * Pins the trigger's zone gate: ninjutsu puts the Shadow onto the battlefield from hand (so it
 * triggers), a cast Shadow enters from the stack (so it doesn't), and "another" keeps the Shadow
 * from copying itself.
 */
class ThousandFacedShadowScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + ThousandFacedShadow)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tokens(name: String): List<EntityId> =
        getPermanents(player1).filter {
            state.getEntity(it)?.get<CardComponent>()?.name == name && state.getEntity(it)!!.has<TokenComponent>()
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

    fun GameTestDriver.ninjutsu(shadow: EntityId, returned: EntityId) {
        giveMana(player1, Color.BLUE, 4)
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = shadow,
                abilityId = ninjutsuAbility("{2}{U}{U}").id,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(returned)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
    }

    test("ninjutsu: the Shadow copies another attacking creature as a tapped, attacking token") {
        val d = driver()
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions").also(d::removeSummoningSickness)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also(d::removeSummoningSickness)
        d.attackUnblocked(listOf(lions, bears))
        val shadowCard = d.putCardInHand(d.player1, ThousandFacedShadow.name)
        d.ninjutsu(shadowCard, lions)

        var guard = 0
        var targetPrompts = 0
        while (guard++ < 20 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            when (val pd = d.pendingDecision) {
                is ChooseTargetsDecision -> {
                    targetPrompts++
                    val legal = pd.legalTargets[0].orEmpty()
                    legal shouldContain bears
                    legal.none { d.state.getEntity(it)?.get<CardComponent>()?.name == ThousandFacedShadow.name } shouldBe true
                    d.submitTargetSelection(d.player1, listOf(bears)).error shouldBe null
                }
                null -> d.bothPass()
                else -> error("unexpected decision $pd")
            }
        }

        d.findPermanent(d.player1, ThousandFacedShadow.name).shouldNotBeNull()
        d.getHand(d.player1).map { d.state.getEntity(it)?.get<CardComponent>()?.name } shouldContain "Savannah Lions"
        val token = d.tokens("Grizzly Bears").single()
        d.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
        d.state.getEntity(token)!!.get<AttackingComponent>().shouldNotBeNull()
        d.state.getEntity(bears)!!.has<TokenComponent>() shouldBe false
    }

    test("the Shadow can't copy itself: alone in combat, no token is made") {
        val d = driver()
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions").also(d::removeSummoningSickness)
        d.attackUnblocked(listOf(lions))
        val shadowCard = d.putCardInHand(d.player1, ThousandFacedShadow.name)
        d.ninjutsu(shadowCard, lions)

        var guard = 0
        while (guard++ < 20 && (d.state.stack.isNotEmpty() || d.pendingDecision != null)) {
            when (val pd = d.pendingDecision) {
                null -> d.bothPass()
                else -> error("no target prompt expected, got $pd")
            }
        }

        d.findPermanent(d.player1, ThousandFacedShadow.name).shouldNotBeNull()
        d.tokens(ThousandFacedShadow.name).shouldBeEmpty()
        d.getPermanents(d.player1).filter { d.state.getEntity(it)!!.has<TokenComponent>() }.shouldBeEmpty()
    }

    test("a cast Shadow enters from the stack, not from hand: nothing triggers") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val shadowCard = d.putCardInHand(d.player1, ThousandFacedShadow.name)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpell(d.player1, shadowCard).outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(d.player1, ThousandFacedShadow.name).shouldNotBeNull()
        d.state.stack.shouldBeEmpty()
        d.pendingDecision shouldBe null
        d.getHand(d.player1) shouldNotContain shadowCard
        d.getPermanents(d.player1).filter { d.state.getEntity(it)!!.has<TokenComponent>() } shouldHaveSize 0
    }
})
