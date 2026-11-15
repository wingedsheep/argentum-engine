package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.FoeRazerRegent
import com.wingedsheep.mtg.sets.definitions.isd.cards.PreyUpon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Foe-Razer Regent (DTK #187) — "Flying. When this creature enters, you may have it fight target
 * creature you don't control. Whenever a creature you control fights, put two +1/+1 counters on it
 * at the beginning of the next end step."
 *
 * Rulings pinned: the fight is chosen on resolution; a fighter that is gone by the end step gets
 * nothing; a fight during the end step waits for the following end step.
 */
class FoeRazerRegentScenarioTest : FunSpec({

    /** An instant fight, so a fight can happen during the end step. */
    val InstantBrawl = card("Foe-Razer Test Instant Brawl") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val mine = target(TargetFilter.CreatureYouControl)
            val theirs = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.Fight(mine, theirs)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FoeRazerRegent, PreyUpon, InstantBrawl))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.drain() {
        var guard = 0
        while (guard++ < 30 && (pendingDecision != null || state.stack.isNotEmpty())) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    /** Casts the Regent, aims its enters trigger at [target] and answers the "may fight" with [fight]. */
    fun GameTestDriver.castRegent(me: EntityId, target: EntityId, fight: Boolean): EntityId {
        val regent = putCardInHand(me, "Foe-Razer Regent")
        giveMana(me, Color.GREEN, 7)
        castSpell(me, regent)
        var guard = 0
        while (guard++ < 30 && (pendingDecision != null || state.stack.isNotEmpty())) {
            when (pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(me, listOf(target))
                is YesNoDecision -> submitYesNo(me, fight)
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
        return findPermanent(me, "Foe-Razer Regent")!!
    }

    fun GameTestDriver.resolveEndStep() {
        passPriorityUntil(Step.END)
        drain()
    }

    test("it fights on entering and grows by two counters at the next end step") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        val regent = driver.castRegent(me, courser, fight = true)

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.damageOn(regent) shouldBe 3
        withClue("the counters wait for the end step") { driver.plusOnes(regent) shouldBe 0 }

        driver.resolveEndStep()
        driver.plusOnes(regent) shouldBe 2
    }

    test("declining the fight means no fight and no counters") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val regent = driver.castRegent(me, courser, fight = false)

        driver.damageOn(courser) shouldBe 0
        driver.resolveEndStep()
        driver.plusOnes(regent) shouldBe 0
    }

    test("another creature you control that fights gets the counters; the Regent itself doesn't") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val regent = driver.putCreatureOnBattlefield(me, "Foe-Razer Regent")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 1/1

        val spell = driver.putCardInHand(me, "Prey Upon")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell, listOf(courser, lions))
        driver.drain()

        driver.resolveEndStep()
        driver.plusOnes(courser) shouldBe 2
        driver.plusOnes(regent) shouldBe 0
    }

    test("a fighter that died in the fight gets nothing at the end step") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val regent = driver.putCreatureOnBattlefield(me, "Foe-Razer Regent")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature") // 5/5

        val spell = driver.putCardInHand(me, "Prey Upon")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell, listOf(lions, force))
        driver.drain()
        driver.findPermanent(me, "Savannah Lions") shouldBe null

        driver.resolveEndStep()
        driver.plusOnes(regent) shouldBe 0
        driver.plusOnes(force) shouldBe 0
        (lions in driver.getGraveyard(me)) shouldBe true
    }

    test("a fight during the end step waits for the following end step") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Foe-Razer Regent")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.passPriorityUntil(Step.END)
        val spell = driver.putCardInHand(me, "Foe-Razer Test Instant Brawl")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, spell, listOf(courser, lions))
        driver.drain()
        driver.findPermanent(opponent, "Savannah Lions") shouldBe null

        driver.passPriorityUntil(Step.UPKEEP)
        withClue("no counters in this turn's end step") { driver.plusOnes(courser) shouldBe 0 }

        driver.resolveEndStep()
        driver.plusOnes(courser) shouldBe 2
    }
})
