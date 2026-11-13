package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.KozileksChanneler
import com.wingedsheep.mtg.sets.definitions.bfz.cards.SanctumOfUgin
import com.wingedsheep.mtg.sets.definitions.fdn.cards.SireOfSevenDeaths
import com.wingedsheep.mtg.sets.definitions.roe.cards.PelakkaWurm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sanctum of Ugin (BFZ #242) — Land.
 *
 *   {T}: Add {C}.
 *   Whenever you cast a colorless spell with mana value 7 or greater, you may sacrifice this land.
 *   If you do, search your library for a colorless creature card, reveal it, put it into your
 *   hand, then shuffle.
 *
 * Sire of Seven Deaths ({7}) is a colorless spell at the threshold; Pelakka Wurm ({4}{G}{G}{G})
 * is mana value 7 but green; Kozilek's Channeler ({5}) is colorless but under the threshold.
 */
class SanctumOfUginScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(SanctumOfUgin, SireOfSevenDeaths, PelakkaWurm, KozileksChanneler))
        return driver
    }

    /** Result of draining the stack: whether the "may sacrifice" was offered and what the search offered. */
    data class Resolution(val offered: Boolean, val searchOptions: List<EntityId>?)

    fun castAndResolve(
        driver: GameTestDriver,
        me: EntityId,
        spell: EntityId,
        accept: Boolean,
        pick: EntityId?,
    ): Resolution {
        driver.giveMana(me, Color.GREEN, 7)
        driver.castSpell(me, spell)
        var offered = false
        var searchOptions: List<EntityId>? = null
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val d = driver.pendingDecision) {
                is YesNoDecision -> {
                    offered = true
                    driver.submitYesNo(me, accept)
                }
                is SelectCardsDecision -> {
                    searchOptions = d.options
                    driver.submitCardSelection(me, listOfNotNull(pick))
                }
                null -> driver.bothPass()
                else -> error("unexpected decision $d")
            }
        }
        return Resolution(offered, searchOptions)
    }

    test("casting a colorless mana value 7 spell lets you sacrifice it to tutor a colorless creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(me, "Sanctum of Ugin")
        val channeler = driver.putCardOnTopOfLibrary(me, "Kozilek's Channeler")
        val wurm = driver.putCardOnTopOfLibrary(me, "Pelakka Wurm")
        val sire = driver.putCardInHand(me, "Sire of Seven Deaths")

        val result = castAndResolve(driver, me, sire, accept = true, pick = channeler)

        withClue("the trigger offered the sacrifice") { result.offered shouldBe true }
        withClue("only colorless creature cards are searchable") {
            result.searchOptions shouldNotBe null
            result.searchOptions!! shouldContain channeler
            result.searchOptions shouldNotContain wurm
        }
        withClue("Sanctum was sacrificed, the Channeler is in hand, and Sire resolved") {
            driver.findPermanent(me, "Sanctum of Ugin") shouldBe null
            driver.getGraveyardCardNames(me) shouldContain "Sanctum of Ugin"
            driver.getHand(me) shouldContain channeler
            driver.findPermanent(me, "Sire of Seven Deaths") shouldNotBe null
        }
    }

    test("declining the sacrifice keeps the land and searches nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(me, "Sanctum of Ugin")
        val channeler = driver.putCardOnTopOfLibrary(me, "Kozilek's Channeler")
        val sire = driver.putCardInHand(me, "Sire of Seven Deaths")

        val result = castAndResolve(driver, me, sire, accept = false, pick = channeler)

        result.offered shouldBe true
        result.searchOptions shouldBe null
        driver.findPermanent(me, "Sanctum of Ugin") shouldNotBe null
        driver.getHand(me) shouldNotContain channeler
    }

    test("a colored mana value 7 spell does not trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(me, "Sanctum of Ugin")
        val wurm = driver.putCardInHand(me, "Pelakka Wurm")

        val result = castAndResolve(driver, me, wurm, accept = true, pick = null)

        result.offered shouldBe false
        driver.findPermanent(me, "Sanctum of Ugin") shouldNotBe null
        driver.findPermanent(me, "Pelakka Wurm") shouldNotBe null
    }

    test("a colorless spell under mana value 7 does not trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putLandOnBattlefield(me, "Sanctum of Ugin")
        val channeler = driver.putCardInHand(me, "Kozilek's Channeler")

        val result = castAndResolve(driver, me, channeler, accept = true, pick = null)

        result.offered shouldBe false
        driver.findPermanent(me, "Sanctum of Ugin") shouldNotBe null
        driver.findPermanent(me, "Kozilek's Channeler") shouldNotBe null
    }
})
