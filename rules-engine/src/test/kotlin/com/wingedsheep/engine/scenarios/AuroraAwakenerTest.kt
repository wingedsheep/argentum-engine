package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.AuroraAwakener
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Aurora Awakener's Vivid ETB trigger.
 *
 * {6}{G} Creature — Giant Druid 7/7, Trample
 * Vivid — When this creature enters, reveal cards from the top of your library until you
 * reveal X permanent cards, where X is the number of colors among permanents you control.
 * Put any number of those permanent cards onto the battlefield, then put the rest of the
 * revealed cards on the bottom of your library in a random order.
 *
 * Exercises the effect-scaling half of Vivid (Lorwyn Eclipsed mechanic): the
 * [GatherUntilMatchEffect.count] field combined with a `DISTINCT_COLORS` aggregation
 * over permanents on the battlefield. Also verifies the `ChooseAnyNumber` branch of
 * SelectFromCollection and that the unchosen permanents flow back to the library.
 */
class AuroraAwakenerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AuroraAwakener)
        return driver
    }

    fun giveCastingMana(driver: GameTestDriver, playerId: com.wingedsheep.sdk.model.EntityId) {
        driver.giveMana(playerId, Color.GREEN, 1)
        driver.giveColorlessMana(playerId, 6)
    }

    test("reveals up to X=colors permanents, chosen permanents enter the battlefield, rest to bottom") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Pre-ETB colors: Goblin Guide (R), Savannah Lions (W) → 2 colors.
        // After Aurora Awakener enters (G), total becomes 3 colors → reveal until 3 permanents.
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Guide")
        driver.putCreatureOnBattlefield(activePlayer, "Savannah Lions")

        // Library top order (last push = top): nonLand, permanent1, nonLand, permanent2, nonLand, permanent3, deep
        // After revealing top-down, we walk until we've seen 3 permanents (CC, FN, SL) and have
        // also revealed the two Plains in between.
        val deep = driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature")
        val permThird = driver.putCardOnTopOfLibrary(activePlayer, "Savannah Lions")
        val permSecond = driver.putCardOnTopOfLibrary(activePlayer, "Force of Nature")
        val permFirst = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser")

        val aurora = driver.putCardInHand(activePlayer, "Aurora Awakener")
        giveCastingMana(driver, activePlayer)

        driver.castSpell(activePlayer, aurora)
        driver.bothPass() // resolve Aurora onto battlefield, ETB goes on stack
        driver.bothPass() // begin resolving ETB

        // The ETB walked the library top-down and stopped after 3 permanents.
        // A SelectCardsDecision is now pending to pick any number of permanents to keep.
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision shouldNotBe null
        (decision is SelectCardsDecision) shouldBe true
        val selectDecision = decision as SelectCardsDecision
        selectDecision.options.toSet() shouldBe setOf(permFirst, permSecond, permThird)

        // Keep the first two permanents (CC + FN); let the third slide back with the bottom pile.
        val toBattlefield = listOf(permFirst, permSecond)
        driver.submitCardSelection(activePlayer, toBattlefield)

        driver.isPaused shouldBe false

        // Chosen permanents are on the battlefield.
        driver.findPermanent(activePlayer, "Centaur Courser") shouldBe permFirst
        driver.findPermanent(activePlayer, "Force of Nature") shouldBe permSecond

        // The unchosen permanent and the card below the last match are still in the library
        // (the deeper card was never revealed; the third permanent went to the bottom).
        val library = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
        library shouldContain permThird
        library shouldContain deep
        library shouldNotContain permFirst
        library shouldNotContain permSecond

        // Aurora Awakener itself is on the battlefield.
        driver.findPermanent(activePlayer, "Aurora Awakener") shouldNotBe null
    }

    test("with a single color, reveals only until the first permanent is found") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No other permanents; once Aurora enters the sole color is green (1).
        val deep = driver.putCardOnTopOfLibrary(activePlayer, "Savannah Lions")
        val firstPermanent = driver.putCardOnTopOfLibrary(activePlayer, "Centaur Courser")

        val aurora = driver.putCardInHand(activePlayer, "Aurora Awakener")
        giveCastingMana(driver, activePlayer)

        driver.castSpell(activePlayer, aurora)
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe true
        val selectDecision = driver.pendingDecision as SelectCardsDecision
        // Only the first permanent should be offered — the walk stops after 1 match.
        selectDecision.options shouldBe listOf(firstPermanent)

        // Decline to put anything onto the battlefield.
        driver.submitCardSelection(activePlayer, emptyList())

        driver.isPaused shouldBe false
        driver.findPermanent(activePlayer, "Centaur Courser") shouldBe null

        // Both the revealed match and the untouched deeper card are still in the library.
        val library = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
        library shouldContain firstPermanent
        library shouldContain deep
    }

    test("library with no permanents resolves without any battlefield placement") {
        val driver = createDriver()
        // Library is only Plains; the Plains are lands (permanents) — swap for a non-permanent setup.
        // We place Instant-like non-permanents on top, then only basic Plains exist below.
        // In this engine's TestCards, we use a small vanilla non-permanent by relying on registered
        // cards. Since every MTG card is essentially either a permanent or a non-permanent spell,
        // any instant/sorcery works here. Clifftop Lookout's test uses only Grizzly Bears (creature);
        // we instead stack the library with a mix and rely on the GatherUntilMatch fallthrough
        // when no permanents are found.
        driver.initMirrorMatch(deck = Deck.of("Lightning Bolt" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val aurora = driver.putCardInHand(activePlayer, "Aurora Awakener")
        giveCastingMana(driver, activePlayer)

        val libraryBefore = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY)).toList()

        driver.castSpell(activePlayer, aurora)
        driver.bothPass()
        driver.bothPass()

        // With no permanents revealed, there's no selection decision — the trigger finishes cleanly.
        driver.isPaused shouldBe false

        // Aurora is still on the battlefield.
        driver.findPermanent(activePlayer, "Aurora Awakener") shouldNotBe null

        // The library is rearranged (walked top-down, then whatever was walked goes to the bottom)
        // but retains every card — we didn't lose anything to other zones.
        val libraryAfter = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
        libraryAfter.size shouldBe libraryBefore.size
        libraryAfter.toSet() shouldBe libraryBefore.toSet()
    }
})
