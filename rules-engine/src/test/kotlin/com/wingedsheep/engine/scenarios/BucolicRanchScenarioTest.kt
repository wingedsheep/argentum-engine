package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BridledBighorn
import com.wingedsheep.mtg.sets.definitions.otj.cards.BucolicRanch
import com.wingedsheep.mtg.sets.definitions.otj.cards.IntrepidStablemaster
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Bucolic Ranch — Land — Desert
 *
 * {3}, {T}: Look at the top card of your library. If it's a Mount card, you may reveal it and put
 * it into your hand. If you don't put it into your hand, you may put it on the bottom of your
 * library.
 *
 * (The Intrepid Stablemaster card is registered only so the "Mount" creature it represents is
 * available as a top-of-library card to find.)
 */
class BucolicRanchScenarioTest : FunSpec({

    // index 2 is the {3},{T} look ability (0 = {C}, 1 = any-color restricted, 2 = look).
    val lookAbilityId = BucolicRanch.activatedAbilities[2].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BucolicRanch)
        driver.registerCard(BridledBighorn) // a Sheep Mount
        driver.registerCard(IntrepidStablemaster) // not a Mount, used only as a non-Mount top card
        return driver
    }

    fun GameTestDriver.handNames(playerId: com.wingedsheep.sdk.model.EntityId): List<String> =
        getHand(playerId).mapNotNull { getCardName(it) }

    test("look ability: a Mount on top may be put into hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val ranch = driver.putPermanentOnBattlefield(me, "Bucolic Ranch")
        driver.removeSummoningSickness(ranch)
        // A Mount creature card on top of the library.
        val mount = driver.putCardOnTopOfLibrary(me, "Bridled Bighorn")
        driver.giveColorlessMana(me, 3)

        driver.submit(
            ActivateAbility(playerId = me, sourceId = ranch, abilityId = lookAbilityId)
        ).isSuccess shouldBe true
        driver.bothPass()

        // A select decision for the Mount → choose to take it.
        if (driver.isPaused) {
            driver.submitCardSelection(me, listOf(mount))
            // Possible second (optional bottom) decision over the now-empty remainder: pass through.
            while (driver.isPaused) {
                driver.submitCardSelection(me, emptyList())
            }
        }
        driver.handNames(me) shouldContain "Bridled Bighorn"
    }

    test("look ability: a non-Mount on top cannot be taken into hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val ranch = driver.putPermanentOnBattlefield(me, "Bucolic Ranch")
        driver.removeSummoningSickness(ranch)
        // A non-Mount creature card on top.
        driver.putCardOnTopOfLibrary(me, "Intrepid Stablemaster")
        driver.giveColorlessMana(me, 3)

        driver.submit(
            ActivateAbility(playerId = me, sourceId = ranch, abilityId = lookAbilityId)
        ).isSuccess shouldBe true
        driver.bothPass()

        // Decline every optional selection.
        while (driver.isPaused) {
            driver.submitCardSelection(me, emptyList())
        }
        // It is not a Mount, so it can never be placed into hand.
        driver.handNames(me) shouldNotContain "Intrepid Stablemaster"
    }
})
