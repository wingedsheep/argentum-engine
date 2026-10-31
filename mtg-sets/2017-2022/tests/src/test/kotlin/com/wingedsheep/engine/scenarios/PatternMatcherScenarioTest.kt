package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.PatternMatcher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pattern Matcher (M20 #234) — {4} Artifact Creature — Golem 3/3.
 *
 *   When this creature enters, you may search your library for a card with the same name as
 *   another creature you control, reveal it, put it into your hand, then shuffle.
 *
 * The search filter nests a source exclusion (`Creature.notSourceItself()`) inside
 * `SharesNameWithYourCard`, so these tests pin down that Pattern Matcher never matches its own
 * name, that a second Pattern Matcher can match the first (ruling 2019-07-12), and that the
 * "may" can be declined.
 */
class PatternMatcherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PatternMatcher))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolves the stack, answering the "may" with [accept]; returns the search decision if shown. */
    fun resolveToSearch(driver: GameTestDriver, accept: Boolean = true): SelectCardsDecision? {
        var safety = 0
        while (safety++ < 40) {
            val pd = driver.pendingDecision
            when {
                pd is SelectCardsDecision -> return pd
                pd is YesNoDecision -> driver.submitYesNo(pd.playerId, accept)
                driver.stackSize > 0 -> driver.bothPass()
                else -> return null
            }
        }
        return null
    }

    test("offers only cards named like another creature you control, never itself") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(me, "Centaur Courser")
        driver.putCardOnTopOfLibrary(me, "Pattern Matcher")

        val matcher = driver.putCardInHand(me, "Pattern Matcher")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, matcher).outcome shouldBe Outcome.Done

        val search = resolveToSearch(driver)
        search shouldNotBe null
        search!!.options shouldContainExactly listOf(bears)

        driver.submitCardSelection(me, listOf(bears))
        resolveToSearch(driver)
        driver.findCardInHand(me, "Grizzly Bears") shouldBe bears
    }

    test("a second Pattern Matcher can look at the first to find a third") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Pattern Matcher")
        val third = driver.putCardOnTopOfLibrary(me, "Pattern Matcher")
        driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        val second = driver.putCardInHand(me, "Pattern Matcher")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, second).outcome shouldBe Outcome.Done

        val search = resolveToSearch(driver)
        search shouldNotBe null
        search!!.options shouldContainExactlyInAnyOrder listOf(third)

        driver.submitCardSelection(me, listOf(third))
        resolveToSearch(driver)
        driver.findCardInHand(me, "Pattern Matcher") shouldBe third
    }

    test("declining the search leaves the library untouched") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        val matcher = driver.putCardInHand(me, "Pattern Matcher")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, matcher).outcome shouldBe Outcome.Done

        resolveToSearch(driver, accept = false) shouldBe null
        driver.findCardInHand(me, "Grizzly Bears") shouldBe null
    }
})
