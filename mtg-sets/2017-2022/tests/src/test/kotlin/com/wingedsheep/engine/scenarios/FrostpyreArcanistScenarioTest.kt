package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.FrostpyreArcanist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Frostpyre Arcanist (KHM #58) — {4}{U} Creature — Giant Wizard 2/5.
 *
 *   This spell costs {1} less to cast if you control a Giant or a Wizard.
 *   When this creature enters, search your library for an instant or sorcery card with the same
 *   name as a card in your graveyard, reveal it, put it into your hand, then shuffle.
 *
 * The search offers only instants/sorceries named like a card in *your* graveyard — an opponent's
 * graveyard doesn't count — and the discount applies only while you control a Giant or a Wizard.
 */
class FrostpyreArcanistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FrostpyreArcanist))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolves the stack, returning the ETB search decision if one is presented. */
    fun resolveToSearch(driver: GameTestDriver): SelectCardsDecision? {
        var safety = 0
        while (safety++ < 40) {
            val pd = driver.pendingDecision
            when {
                pd is SelectCardsDecision -> return pd
                driver.stackSize > 0 -> driver.bothPass()
                else -> return null
            }
        }
        return null
    }

    test("ETB offers only instants and sorceries named like a card in your own graveyard") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putCardInGraveyard(me, "Lightning Bolt")
        driver.putCardInGraveyard(opponent, "Doom Blade")
        val bolt = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        driver.putCardOnTopOfLibrary(me, "Doom Blade")
        driver.putCardOnTopOfLibrary(me, "Counterspell")

        val arcanist = driver.putCardInHand(me, "Frostpyre Arcanist")
        driver.giveMana(me, Color.BLUE, 5)
        driver.castSpell(me, arcanist).outcome shouldBe Outcome.Done

        val search = resolveToSearch(driver)
        search shouldNotBe null
        search!!.options shouldContainExactly listOf(bolt)

        driver.submitCardSelection(me, listOf(bolt))
        resolveToSearch(driver)
        driver.findCardInHand(me, "Lightning Bolt") shouldBe bolt
    }

    test("with no name match in your graveyard the search finds nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCardInGraveyard(me, "Counterspell")
        driver.putCardOnTopOfLibrary(me, "Lightning Bolt")

        val arcanist = driver.putCardInHand(me, "Frostpyre Arcanist")
        driver.giveMana(me, Color.BLUE, 5)
        driver.castSpell(me, arcanist).outcome shouldBe Outcome.Done

        val search = resolveToSearch(driver)
        (search == null || search.options.isEmpty()) shouldBe true
        if (search != null) driver.submitCardSelection(me, emptyList())
        resolveToSearch(driver)
        driver.findCardInHand(me, "Lightning Bolt") shouldBe null
    }

    test("costs {1} less while you control a Giant or a Wizard") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val first = driver.putCardInHand(me, "Frostpyre Arcanist")
        driver.giveMana(me, Color.BLUE, 4)
        (driver.castSpell(me, first).outcome == Outcome.Done) shouldBe false

        driver.putCreatureOnBattlefield(me, "Frostpyre Arcanist")
        driver.castSpell(me, first).outcome shouldBe Outcome.Done
    }
})
