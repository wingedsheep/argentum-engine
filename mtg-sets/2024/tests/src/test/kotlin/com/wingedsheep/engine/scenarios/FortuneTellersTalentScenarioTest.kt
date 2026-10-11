package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blc.cards.FortuneTellersTalent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fortune Teller's Talent {U} — Enchantment — Class
 * You may look at the top card of your library any time.
 * {3}{U}: Level 2 — As long as you've cast a spell this turn, you may play cards from the top of
 * your library.
 * {2}{U}: Level 3 — Spells you cast from anywhere other than your hand cost {2} less to cast.
 *
 * The level 2 and 3 abilities live in `classLevels`, so these tests pin that the cast-permission and
 * land-play scans read the Class's *reached* levels, not just its base statics.
 */
class FortuneTellersTalentScenarioTest : FunSpec({

    fun setup(level: Int): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FortuneTellersTalent)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val talent = driver.putPermanentOnBattlefield(you, "Fortune Teller's Talent")
        driver.replaceState(driver.state.updateEntity(talent) { it.with(ClassLevelComponent(currentLevel = level)) })
        return driver to you
    }

    fun GameTestDriver.castASpellFromHand(you: EntityId) {
        val bears = putCardInHand(you, "Grizzly Bears")
        giveMana(you, Color.GREEN, 2)
        castSpell(you, bears).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("level 1 grants no permission to play from the top of the library") {
        val (driver, you) = setup(level = 1)
        driver.castASpellFromHand(you)

        val forest = driver.putCardOnTopOfLibrary(you, "Forest")
        driver.playLand(you, forest).outcome shouldNotBe Outcome.Done
    }

    test("level 2 does nothing until you've cast a spell this turn") {
        val (driver, you) = setup(level = 2)

        val forest = driver.putCardOnTopOfLibrary(you, "Forest")
        driver.playLand(you, forest).outcome shouldNotBe Outcome.Done
        driver.findPermanent(you, "Forest") shouldBe null
    }

    test("level 2 lets you play a land from the top after casting a spell") {
        val (driver, you) = setup(level = 2)
        driver.castASpellFromHand(you)

        val forest = driver.putCardOnTopOfLibrary(you, "Forest")
        driver.playLand(you, forest).outcome shouldBe Outcome.Done
        driver.findPermanent(you, "Forest") shouldNotBe null
    }

    test("level 2 offers the top card's land play and cast as legal actions") {
        val (driver, you) = setup(level = 2)
        driver.castASpellFromHand(you)

        val forest = driver.putCardOnTopOfLibrary(you, "Forest")
        driver.legalActions(you).any { (it.action as? PlayLand)?.cardId == forest } shouldBe true

        driver.playLand(you, forest).outcome shouldBe Outcome.Done
        val bearsOnTop = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 2)
        driver.legalActions(you).any { (it.action as? CastSpell)?.cardId == bearsOnTop } shouldBe true
    }

    test("level 2 lets you cast a spell from the top at full cost") {
        val (driver, you) = setup(level = 2)
        driver.castASpellFromHand(you)

        val bearsOnTop = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 1)
        driver.castSpell(you, bearsOnTop).outcome shouldNotBe Outcome.Done

        driver.giveMana(you, Color.GREEN, 1)
        driver.castSpell(you, bearsOnTop).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getBattlefield(you).contains(bearsOnTop) shouldBe true
    }

    test("level 3 makes a spell cast from the top cost {2} less") {
        val (driver, you) = setup(level = 3)
        driver.castASpellFromHand(you)

        // Grizzly Bears costs {1}{G}; the reduction removes the generic {1}, leaving {G}.
        val bearsOnTop = driver.putCardOnTopOfLibrary(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 1)
        driver.castSpell(you, bearsOnTop).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getBattlefield(you).contains(bearsOnTop) shouldBe true
    }

    test("level 3 does not reduce spells cast from your hand") {
        val (driver, you) = setup(level = 3)

        val bears = driver.putCardInHand(you, "Grizzly Bears")
        driver.giveMana(you, Color.GREEN, 1)
        driver.castSpell(you, bears).outcome shouldNotBe Outcome.Done
    }
})
