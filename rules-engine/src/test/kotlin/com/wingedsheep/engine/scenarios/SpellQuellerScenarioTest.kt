package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.SpellQueller
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Spell Queller (EMN) — {1}{W}{U} 2/3 Spirit, Flash, Flying.
 *
 * "When this creature enters, exile target spell with mana value 4 or less."
 * "When this creature leaves the battlefield, the exiled card's owner may cast that card without
 * paying its mana cost."
 *
 * Covers the new `ExileTargetSpellEffect.linkToSource` flag (the exiled card is recorded in the
 * Queller's linked-exile pile) and the leaves-the-battlefield payoff, which is a cast *during
 * resolution* by the card's **owner** — not a lingering may-play permission for the Queller's
 * controller.
 */
class SpellQuellerScenarioTest : FunSpec({

    /**
     * Board: the opponent is the active player and casts Grizzly Bears (a targetless creature
     * spell, mana value 2). We flash in Spell Queller in response; its ETB exiles the Bears.
     * Returns the driver with the Bears exiled and the Queller on the battlefield.
     */
    fun quellTheBears(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SpellQueller)
        // startingPlayer = 1 → the opponent takes the first turn, so they can cast a sorcery-speed
        // creature spell while we hold up flash.
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val opp = driver.activePlayer!!
        val me = driver.getOpponent(opp)

        val bears = driver.putCardInHand(opp, "Grizzly Bears")
        driver.giveColorlessMana(opp, 1)
        driver.giveMana(opp, Color.GREEN, 1)
        driver.castSpell(opp, bears).isSuccess shouldBe true
        driver.getStackSpellNames().contains("Grizzly Bears") shouldBe true

        // Opponent passes priority, giving us the instant-speed window.
        driver.passPriority(opp)

        val quellerCard = driver.putCardInHand(me, "Spell Queller")
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, quellerCard).isSuccess shouldBe true
        driver.bothPass() // resolve Spell Queller onto the battlefield
        driver.bothPass() // resolve its enters trigger
        driver.submitTargetSelection(me, listOf(bears))
        driver.bothPass()

        return Triple(driver, me, opp).also {
            withClue("the quelled spell is exiled, not countered, and never resolved") {
                driver.getExile(opp).contains(bears) shouldBe true
                driver.getPermanents(opp).contains(bears) shouldBe false
            }
        }
    }

    test("ETB exiles the target spell and links it to the Queller") {
        val (driver, me, opp) = quellTheBears()
        val bears = driver.getExile(opp).first { driver.getCardName(it) == "Grizzly Bears" }

        val queller = driver.findPermanent(me, "Spell Queller")
        queller shouldNotBe null
        val linked = driver.state.getEntity(queller!!)?.get<LinkedExileComponent>()
        withClue("linkToSource records the exiled card so the leaves trigger can find it") {
            linked?.exiledIds shouldBe listOf(bears)
        }
    }

    test("when the Queller leaves, the exiled card's owner may cast it for free") {
        val (driver, me, opp) = quellTheBears()
        val queller = driver.findPermanent(me, "Spell Queller")!!

        // Kill our own Queller with a Lightning Bolt (2/3 takes 3 damage).
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(queller)).isSuccess shouldBe true
        driver.bothPass() // resolve the Bolt; the Queller dies and its leaves trigger goes on the stack
        driver.bothPass() // resolve the leaves trigger

        // The *owner* of the exiled card decides — and casts it.
        driver.submitYesNo(opp, true).isSuccess shouldBe true
        driver.bothPass()

        withClue("Grizzly Bears was cast for free by its owner and resolved under their control") {
            driver.findPermanent(opp, "Grizzly Bears") shouldNotBe null
        }
        withClue("no mana was spent — the opponent had none left after casting the Bears the first time") {
            driver.getExileCardNames(opp).contains("Grizzly Bears") shouldBe false
        }
    }

    test("the owner may decline, and the card stays exiled") {
        val (driver, me, opp) = quellTheBears()
        val queller = driver.findPermanent(me, "Spell Queller")!!

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(queller)).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        driver.submitYesNo(opp, false).isSuccess shouldBe true
        driver.bothPass()

        withClue("declining the 'may' leaves the card in exile forever") {
            driver.getExileCardNames(opp).contains("Grizzly Bears") shouldBe true
            driver.findPermanent(opp, "Grizzly Bears") shouldBe null
        }
    }

    test("a Queller that exiled nothing does nothing when it leaves") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SpellQueller)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val queller = driver.putCreatureOnBattlefield(me, "Spell Queller")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(queller)).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        withClue("an empty linked-exile pile yields no owners, so the trigger asks nobody anything") {
            driver.state.pendingDecision shouldBe null
            driver.assertInGraveyard(me, "Spell Queller")
        }
    }
})
