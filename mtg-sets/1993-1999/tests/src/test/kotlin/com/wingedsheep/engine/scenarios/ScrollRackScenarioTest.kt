package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmp.cards.ScrollRack
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scroll Rack — {1}, {T}: Exile any number of cards from your hand face down. Put that many cards
 * from the top of your library into your hand. Then look at the exiled cards and put them on top
 * of your library in any order.
 *
 * Pins the printed order of operations (the cards taken are the ones that were on top *before*
 * the exiled cards go back), that "that many" follows the number exiled, and that the exiled
 * cards end up on top in the chosen order with nothing left in exile.
 */
class ScrollRackScenarioTest : FunSpec({

    val rackAbility = ScrollRack.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ScrollRack)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("swaps the chosen hand cards for the same number of top cards, then stacks them in the chosen order") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val rack = driver.putPermanentOnBattlefield(you, "Scroll Rack")
        val forest = driver.putCardInHand(you, "Forest")
        val mountain = driver.putCardInHand(you, "Mountain")
        val secondFromTop = driver.putCardOnTopOfLibrary(you, "Swamp")
        val top = driver.putCardOnTopOfLibrary(you, "Plains")
        driver.giveColorlessMana(you, 1)

        driver.submit(ActivateAbility(you, rack, rackAbility)).error shouldBe null
        driver.bothPass()

        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options.toSet() shouldBe driver.getHand(you).toSet()
        driver.submitCardSelection(you, listOf(forest, mountain)).error shouldBe null

        val order = driver.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        order.cards.toSet() shouldBe setOf(forest, mountain)
        driver.submitOrderedResponse(you, listOf(mountain, forest)).error shouldBe null

        driver.pendingDecision shouldBe null
        driver.getHand(you) shouldContainAll listOf(top, secondFromTop)
        driver.getHand(you) shouldNotContainAnyOf listOf(forest, mountain)
        driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).take(2) shouldBe listOf(mountain, forest)
        driver.getExile(you) shouldBe emptyList()
    }

    test("exiling nothing takes nothing and leaves hand and library alone") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val rack = driver.putPermanentOnBattlefield(you, "Scroll Rack")
        val top = driver.putCardOnTopOfLibrary(you, "Plains")
        val handBefore = driver.getHand(you)
        driver.giveColorlessMana(you, 1)

        driver.submit(ActivateAbility(you, rack, rackAbility)).error shouldBe null
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(you, emptyList()).error shouldBe null

        driver.pendingDecision shouldBe null
        driver.getHand(you).toSet() shouldBe handBefore.toSet()
        driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).first() shouldBe top
    }
})
