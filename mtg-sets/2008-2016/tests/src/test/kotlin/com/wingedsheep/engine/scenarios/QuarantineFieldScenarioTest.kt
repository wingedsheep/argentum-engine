package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Quarantine Field (BFZ #43) — {X}{X}{W}{W} Enchantment. "This enchantment enters with X isolation
 * counters on it. When this enchantment enters, for each isolation counter on it, exile up to one
 * target nonland permanent an opponent controls until this enchantment leaves the battlefield."
 *
 * The entry trigger's target cap is read off the Field's own counters as the trigger goes on the
 * stack, and the exile is a duration ("until … leaves"), so every exiled card returns at once when
 * the Field leaves — and nothing is exiled if it is already gone when the trigger resolves.
 */
class QuarantineFieldScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.creaturesNamed(player: EntityId, name: String): Int =
        getPermanents(player).count { getCardName(it) == name }

    /** Cast the Field for X = 2 and stop at its entry trigger's target choice. */
    fun GameTestDriver.castFieldForTwo(player: EntityId): EntityId {
        val field = putCardInHand(player, "Quarantine Field")
        giveMana(player, Color.WHITE, 6)
        castXSpell(player, field, xValue = 2).outcome shouldBe Outcome.Done
        bothPass() // the Field resolves and enters with two isolation counters; its trigger asks for targets
        state.getEntity(field)?.get<CountersComponent>()?.getCount(CounterType.ISOLATION) shouldBe 2
        return field
    }

    test("X = 2: exiles up to two of the opponent's nonland permanents, and both return when it leaves") {
        val d = setup()
        val me = d.player1
        val opponent = d.player2
        val seeker = d.putCreatureOnBattlefield(opponent, "Glory Seeker")
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.putCreatureOnBattlefield(opponent, "Hill Giant")

        val field = d.castFieldForTwo(me)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.targetRequirements.single().maxTargets shouldBe 2
        d.submitTargetSelection(me, listOf(seeker, bears))
        d.bothPass() // resolve the entry trigger

        d.creaturesNamed(opponent, "Glory Seeker") shouldBe 0
        d.creaturesNamed(opponent, "Grizzly Bears") shouldBe 0
        d.creaturesNamed(opponent, "Hill Giant") shouldBe 1
        d.getExileCardNames(opponent).toSet() shouldBe setOf("Glory Seeker", "Grizzly Bears")

        val wipeClean = d.putCardInHand(me, "Wipe Clean")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpellWithTargets(me, wipeClean, listOf(ChosenTarget.Permanent(field)))
        d.bothPass() // Wipe Clean exiles the Field; both cards return at once, without a trigger

        d.creaturesNamed(opponent, "Glory Seeker") shouldBe 1
        d.creaturesNamed(opponent, "Grizzly Bears") shouldBe 1
        d.getExileCardNames(opponent) shouldBe emptyList()
    }

    test("the Field leaving before its trigger resolves exiles nothing") {
        val d = setup()
        val me = d.player1
        val opponent = d.player2
        val seeker = d.putCreatureOnBattlefield(opponent, "Glory Seeker")
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val field = d.castFieldForTwo(me)
        d.submitTargetSelection(me, listOf(seeker, bears))

        // With the trigger on the stack, exile the Field in response.
        val wipeClean = d.putCardInHand(me, "Wipe Clean")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpellWithTargets(me, wipeClean, listOf(ChosenTarget.Permanent(field)))
        d.bothPass() // Wipe Clean resolves
        d.bothPass() // the entry trigger resolves with its source gone

        d.creaturesNamed(opponent, "Glory Seeker") shouldBe 1
        d.creaturesNamed(opponent, "Grizzly Bears") shouldBe 1
        d.getExileCardNames(opponent) shouldBe emptyList()
    }
})
