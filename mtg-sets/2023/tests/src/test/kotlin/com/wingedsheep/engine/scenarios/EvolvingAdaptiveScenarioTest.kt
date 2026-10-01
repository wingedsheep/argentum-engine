package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.lea.cards.WallOfWood
import com.wingedsheep.mtg.sets.definitions.one.cards.EvolvingAdaptive
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Evolving Adaptive (ONE #167) — {G} 0/0 Creature — Phyrexian Warrior.
 *
 * "This creature enters with an oil counter on it. This creature gets +1/+1 for each oil counter
 *  on it. Whenever another creature you control enters, if that creature has greater power or
 *  toughness than this creature, put an oil counter on this creature."
 */
class EvolvingAdaptiveScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EvolvingAdaptive, WallOfWood, GrizzlyBears))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun cast(driver: GameTestDriver, name: String, color: Color, colored: Int, generic: Int = 0) {
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, name)
        driver.giveMana(p1, color, colored)
        if (generic > 0) driver.giveColorlessMana(p1, generic)
        driver.castSpell(p1, card).outcome shouldBe Outcome.Done
        driver.bothPass()
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun castAdaptive(driver: GameTestDriver): EntityId {
        cast(driver, "Evolving Adaptive", Color.GREEN, 1)
        val adaptive = driver.findPermanent(driver.player1, "Evolving Adaptive")
        adaptive shouldNotBe null
        return adaptive!!
    }

    test("enters with an oil counter and is a 1/1") {
        val driver = newDriver()
        val adaptive = castAdaptive(driver)
        oil(driver, adaptive) shouldBe 1
        driver.state.projectedState.getPower(adaptive) shouldBe 1
        driver.state.projectedState.getToughness(adaptive) shouldBe 1
    }

    test("a creature that is not bigger on either axis adds nothing") {
        val driver = newDriver()
        val adaptive = castAdaptive(driver)
        cast(driver, "Savannah Lions", Color.WHITE, 1)
        oil(driver, adaptive) shouldBe 1
        driver.state.projectedState.getPower(adaptive) shouldBe 1
    }

    test("greater toughness alone is enough, and the growing body raises the bar") {
        val driver = newDriver()
        val adaptive = castAdaptive(driver)

        // Wall of Wood 0/3: toughness 3 > 1.
        cast(driver, "Wall of Wood", Color.GREEN, 1)
        oil(driver, adaptive) shouldBe 2
        driver.state.projectedState.getPower(adaptive) shouldBe 2
        driver.state.projectedState.getToughness(adaptive) shouldBe 2

        // Grizzly Bears 2/2 vs a 2/2 Adaptive: neither greater.
        cast(driver, "Grizzly Bears", Color.GREEN, 1, generic = 1)
        oil(driver, adaptive) shouldBe 2

        // Centaur Courser 3/3: greater power.
        cast(driver, "Centaur Courser", Color.GREEN, 1, generic = 2)
        oil(driver, adaptive) shouldBe 3
        driver.state.projectedState.getPower(adaptive) shouldBe 3
        driver.state.projectedState.getToughness(adaptive) shouldBe 3
    }

    test("an opponent's creature entering does not trigger it") {
        val driver = newDriver()
        val adaptive = castAdaptive(driver)
        val p2 = driver.player2
        driver.passPriorityUntil(Step.UPKEEP) // advance into player 2's turn
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.activePlayerId shouldBe p2
        val courser = driver.putCardInHand(p2, "Centaur Courser")
        driver.giveMana(p2, Color.GREEN, 1)
        driver.giveColorlessMana(p2, 2)
        driver.castSpell(p2, courser).outcome shouldBe Outcome.Done
        driver.bothPass()
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.findPermanent(p2, "Centaur Courser") shouldNotBe null
        oil(driver, adaptive) shouldBe 1
    }
})
