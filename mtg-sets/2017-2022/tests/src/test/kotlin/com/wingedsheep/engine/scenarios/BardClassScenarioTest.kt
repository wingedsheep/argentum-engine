package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.BardClass
import com.wingedsheep.mtg.sets.definitions.afr.cards.DelinaWildMage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bard Class {R}{G} — Enchantment — Class (AFR #217).
 *
 * Pins the three class abilities against the engine: level 1's enters-with-counter replacement
 * applies only to *legendary* creatures, level 2's colored-only reduction is gated on the Class's
 * reached level and never touches generic mana, and level 3's legendary-cast trigger impulses the
 * top two cards. Delina, Wild Mage ({3}{R}, legendary 3/2) is the legendary spell throughout.
 */
class BardClassScenarioTest : FunSpec({

    fun setup(level: Int): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BardClass)
        driver.registerCard(DelinaWildMage)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val bard = driver.putPermanentOnBattlefield(you, "Bard Class")
        driver.replaceState(driver.state.updateEntity(bard) { it.with(ClassLevelComponent(currentLevel = level)) })
        return driver to you
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("level 1: a legendary creature enters with a +1/+1 counter, a nonlegendary one doesn't") {
        val (driver, you) = setup(level = 1)

        val delina = driver.putCardInHand(you, "Delina, Wild Mage")
        driver.giveMana(you, Color.RED, 4)
        driver.castSpell(you, delina).outcome shouldBe Outcome.Done
        driver.bothPass()
        val delinaOnBattlefield = driver.findPermanent(you, "Delina, Wild Mage")!!
        driver.plusOneCounters(delinaOnBattlefield) shouldBe 1

        val bears = driver.putCardInHand(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 2)
        driver.castSpell(you, bears).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.plusOneCounters(driver.findPermanent(you, "Grizzly Bears")!!) shouldBe 0
    }

    test("level 1 does not reduce legendary spells") {
        val (driver, you) = setup(level = 1)

        val delina = driver.putCardInHand(you, "Delina, Wild Mage")
        driver.giveColorlessMana(you, 3)
        driver.castSpell(you, delina).outcome shouldNotBe Outcome.Done
    }

    test("level 2: a legendary spell costs {R}{G} less, colored mana only") {
        val (driver, you) = setup(level = 2)

        val delina = driver.putCardInHand(you, "Delina, Wild Mage")
        withClue("{3}{R} loses only its {R}, never generic: two mana is not enough") {
            driver.giveColorlessMana(you, 2)
            driver.castSpell(you, delina).outcome shouldNotBe Outcome.Done
        }

        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, delina).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(you, "Delina, Wild Mage") shouldNotBe null
    }

    test("level 3: casting a legendary spell exiles the top two cards, playable this turn") {
        val (driver, you) = setup(level = 3)

        val bears = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val forest = driver.putCardOnTopOfLibrary(you, "Forest")

        val delina = driver.putCardInHand(you, "Delina, Wild Mage")
        driver.giveColorlessMana(you, 3)
        driver.castSpell(you, delina).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the cast trigger
        driver.getExile(you) shouldContainAll listOf(forest, bears)
        driver.bothPass() // resolve Delina

        driver.playLand(you, forest).outcome shouldBe Outcome.Done
        driver.giveMana(you, Color.GREEN, 2)
        driver.castSpell(you, bears).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(you, "Grizzly Bears") shouldNotBe null
    }
})
