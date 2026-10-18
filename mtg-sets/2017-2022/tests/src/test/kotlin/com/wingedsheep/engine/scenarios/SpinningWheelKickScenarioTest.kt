package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.SpinningWheelKick
import com.wingedsheep.mtg.sets.definitions.neo.cards.TheWanderingEmperor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spinning Wheel Kick: {X}{X}{G}{G} Sorcery
 * Target creature you control deals damage equal to its power to each of X target creatures
 * and/or planeswalkers.
 *
 * The creature you control is the source and is not itself hit; each of the X targets takes its
 * power; an illegal one among the X is skipped while the rest are still hit, and an illegal
 * kicker deals no damage at all (CR 608.2b).
 */
class SpinningWheelKickScenarioTest : FunSpec({

    data class Setup(val driver: GameTestDriver, val caster: EntityId, val opponent: EntityId, val kicker: EntityId)

    /** Caster in their main phase with a 3/3 Centaur Courser to kick with. */
    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SpinningWheelKick, TheWanderingEmperor))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val kicker = driver.putCreatureOnBattlefield(caster, "Centaur Courser")
        return Setup(driver, caster, opponent, kicker)
    }

    fun GameTestDriver.kick(caster: EntityId, x: Int, targets: List<EntityId>) {
        val spell = putCardInHand(caster, "Spinning Wheel Kick")
        giveMana(caster, Color.GREEN, 2 + 2 * x)
        castXSpell(caster, spell, x, targets).error shouldBe null
    }

    fun GameTestDriver.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    fun GameTestDriver.onBattlefield(id: EntityId) = id in state.getBattlefield()

    test("the kicker deals its power to each of the X targets, creature and planeswalker alike") {
        val (driver, caster, opponent, kicker) = setup()
        val giant = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val guide = driver.putCreatureOnBattlefield(opponent, "Goblin Guide")
        val emperor = driver.putPermanentOnBattlefield(opponent, "The Wandering Emperor")
        driver.addComponent(emperor, CountersComponent(mapOf(CounterType.LOYALTY to 5)))
        val bystander = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        driver.kick(caster, x = 3, targets = listOf(kicker, giant, guide, emperor))
        driver.bothPass()

        driver.damageOn(giant) shouldBe 3
        driver.onBattlefield(guide) shouldBe false
        driver.state.getEntity(emperor)?.get<CountersComponent>()?.counters?.get(CounterType.LOYALTY) shouldBe 2
        driver.damageOn(kicker) shouldBe 0
        driver.damageOn(bystander) shouldBe 0
    }

    test("X = 0 needs only the kicker and deals no damage") {
        val (driver, caster, opponent, kicker) = setup()
        val guide = driver.putCreatureOnBattlefield(opponent, "Goblin Guide")

        driver.kick(caster, x = 0, targets = listOf(kicker))
        driver.bothPass()

        driver.damageOn(guide) shouldBe 0
        driver.damageOn(kicker) shouldBe 0
    }

    test("a target among the X that left before resolution is skipped; the others are still hit") {
        val (driver, caster, opponent, kicker) = setup()
        val gone = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        val giant = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        driver.kick(caster, x = 2, targets = listOf(kicker, gone, giant))
        driver.moveToGraveyard(gone)
        driver.bothPass()

        driver.damageOn(giant) shouldBe 3
    }

    test("if the creature you control is an illegal target, no damage is dealt") {
        val (driver, caster, opponent, kicker) = setup()
        val giant = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        driver.kick(caster, x = 1, targets = listOf(kicker, giant))
        driver.moveToGraveyard(kicker)
        driver.bothPass()

        driver.damageOn(giant) shouldBe 0
    }

    test("the kicker may also be one of the X targets, and then damages itself") {
        val (driver, caster, opponent, kicker) = setup()
        val giant = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        driver.kick(caster, x = 2, targets = listOf(kicker, giant, kicker))
        driver.bothPass()

        driver.damageOn(giant) shouldBe 3
        driver.onBattlefield(kicker) shouldBe false
    }
})
