package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.PoliticalTriumph
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Political Triumph (MSH) — "Whenever a creature you control enters, scry 1 and put a plan counter
 * on this enchantment."
 *
 * Pins the accumulator, which is the half that failed in playtesting. The creature has to be *cast*
 * rather than dropped in with `putCreatureOnBattlefield`, because that helper writes the battlefield
 * directly and emits no zone-change event, so no enters trigger would fire.
 *
 * The scry is the *first* element of the composite and pauses for a decision, so this also covers
 * the composite resuming its tail (the counter) after that decision — every other card in the repo
 * puts `Effects.Scry` last, so nothing else exercises that ordering.
 */
class PoliticalTriumphScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(PoliticalTriumph)
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.planCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLAN) ?: 0

    /**
     * Resolve the stack, answering any decision (the scry) along the way, and stop as soon as it is
     * empty — passing further would run the turn past the main phase and make the next cast illegal.
     */
    fun GameTestDriver.settle() {
        repeat(12) {
            if (state.pendingDecision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
    }

    test("a creature cast afterwards puts a plan counter on it") {
        val d = driver()
        val triumph = d.putPermanentOnBattlefield(d.player1, "Political Triumph")
        d.planCounters(triumph) shouldBe 0

        val creature = d.putCardInHand(d.player1, "Centaur Courser")
        d.giveMana(d.player1, Color.GREEN, 3)
        d.castSpell(d.player1, creature).isSuccess shouldBe true
        d.settle()

        d.planCounters(triumph) shouldBe 1
    }

    // Mirrors the playtest sequence exactly: the enchantment is *cast* rather than placed by the
    // driver, in case entering via resolution registered its trigger differently.
    test("it still accumulates when cast from hand rather than placed") {
        val d = driver()
        val triumphCard = d.putCardInHand(d.player1, "Political Triumph")
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, triumphCard).isSuccess shouldBe true
        d.settle()

        val creature = d.putCardInHand(d.player1, "Centaur Courser")
        d.giveMana(d.player1, Color.GREEN, 3)
        d.castSpell(d.player1, creature).isSuccess shouldBe true
        d.settle()

        d.planCounters(triumphCard) shouldBe 1
    }

    // The client renders an intervening-if condition as a "current/required" badge. It used to be
    // evaluated with sourceId = null, so any condition reading EntityReference.Source — counters on
    // this permanent, its power, whether it's attacking — resolved to 0 and the badge sat at "0/4"
    // forever while the ability itself worked. Pin the badge against the real counter count.
    test("the trigger-condition badge tracks the real plan-counter count") {
        val d = driver()
        val triumph = d.putPermanentOnBattlefield(d.player1, "Political Triumph")

        val creature = d.putCardInHand(d.player1, "Centaur Courser")
        d.giveMana(d.player1, Color.GREEN, 3)
        d.castSpell(d.player1, creature).isSuccess shouldBe true
        d.settle()
        d.planCounters(triumph) shouldBe 1

        val view = ClientStateTransformer(d.cardRegistry).transform(d.state, d.player1)
        val badge = view.cards.getValue(triumph)
            .activeEffects.first { effect -> effect.effectId == "condition_compare" }
        badge.name shouldBe "1/4"
    }

    test("a second creature adds a second plan counter") {
        val d = driver()
        val triumph = d.putPermanentOnBattlefield(d.player1, "Political Triumph")

        repeat(2) {
            val creature = d.putCardInHand(d.player1, "Centaur Courser")
            d.giveMana(d.player1, Color.GREEN, 3)
            d.castSpell(d.player1, creature).isSuccess shouldBe true
            d.settle()
        }

        d.planCounters(triumph) shouldBe 2
    }
})
