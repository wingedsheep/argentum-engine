package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Identity Thief (EMN #64) — {2}{U}{U} 0/3 Shapeshifter. "Whenever this creature attacks, you may
 * exile another target nontoken creature. If you do, this creature becomes a copy of that creature
 * until end of turn. Return the exiled card to the battlefield under its owner's control at the
 * beginning of the next end step."
 *
 * The copy source has already been exiled when the copy happens, so Identity Thief must copy it as
 * it last existed on the battlefield: its copiable values only (not its counters), and what it was
 * copying if it was a copy (rulings 2016-07-13). The copy outlasts the exiled card's return in the
 * end step and ends at cleanup; the Thief stays attacking throughout.
 */
class IdentityThiefScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    /** Answer the attack trigger: pick [target] if asked, then accept or decline the "you may". */
    fun GameTestDriver.resolveTheft(p: EntityId, target: EntityId, accept: Boolean) {
        var answered = false
        var guard = 0
        while (!answered && guard++ < 40) {
            when (pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(p, listOf(target)).error shouldBe null
                is YesNoDecision -> {
                    submitYesNo(p, accept).error shouldBe null
                    answered = true
                }
                else -> bothPass()
            }
        }
        withClue("the attack trigger asked whether to exile the target") { answered shouldBe true }
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    test("exiles the target, becomes a copy of it until end of turn, and the card returns at the end step") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val thief = d.putCreatureOnBattlefield(p, "Identity Thief")
        d.removeSummoningSickness(thief)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        d.replaceState(d.state.updateEntity(giant) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        })

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p, listOf(thief), opp).error shouldBe null
        d.resolveTheft(p, giant, accept = true)

        withClue("Hill Giant is exiled") {
            d.getExile(opp) shouldContain giant
        }
        withClue("Identity Thief copies Hill Giant's copiable values, not its +1/+1 counters") {
            d.name(thief) shouldBe "Hill Giant"
            d.state.projectedState.getPower(thief) shouldBe 3
            d.state.projectedState.getToughness(thief) shouldBe 3
        }
        withClue("becoming a copy doesn't remove it from combat") {
            d.state.getEntity(thief)?.has<AttackingComponent>() shouldBe true
        }

        d.passPriorityUntil(Step.END)
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        withClue("the unblocked copy dealt 3 combat damage") {
            d.getLifeTotal(opp) shouldBe 17
        }
        withClue("the exiled card returned under its owner's control, without its counters") {
            d.getExile(opp) shouldNotContain giant
            val returned = d.findPermanent(opp, "Hill Giant")
            returned shouldNotBe null
            (d.state.getEntity(returned!!)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        }
        withClue("Identity Thief is still a copy during the end step the card returns in") {
            d.name(thief) shouldBe "Hill Giant"
        }

        d.passPriorityUntil(Step.UPKEEP, opp)
        withClue("the copy ended at cleanup") {
            d.name(thief) shouldBe "Identity Thief"
            d.state.projectedState.getPower(thief) shouldBe 0
            d.state.projectedState.getToughness(thief) shouldBe 3
        }
    }

    test("a targeted Clone is copied as what it was copying, not as the Clone card now in exile") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val thief = d.putCreatureOnBattlefield(p, "Identity Thief")
        d.removeSummoningSickness(thief)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val clone = d.putCardInHand(p, "Clone")
        d.giveMana(p, Color.BLUE, 4)
        d.castSpell(p, clone).error shouldBe null
        d.bothPass()
        d.submitCardSelection(p, listOf(giant))
        d.name(clone) shouldBe "Hill Giant"

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p, listOf(thief), opp).error shouldBe null
        d.resolveTheft(p, clone, accept = true)

        d.getExile(p) shouldContain clone
        withClue("Identity Thief copies the Clone's last-known copiable values: Hill Giant") {
            d.name(thief) shouldBe "Hill Giant"
            d.state.projectedState.getPower(thief) shouldBe 3
            d.state.projectedState.getToughness(thief) shouldBe 3
        }
    }

    test("declining exiles nothing and leaves Identity Thief as it is") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val thief = d.putCreatureOnBattlefield(p, "Identity Thief")
        d.removeSummoningSickness(thief)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p, listOf(thief), opp).error shouldBe null
        d.resolveTheft(p, giant, accept = false)

        d.getExile(opp) shouldNotContain giant
        d.state.getBattlefield() shouldContain giant
        d.name(thief) shouldBe "Identity Thief"
    }
})
