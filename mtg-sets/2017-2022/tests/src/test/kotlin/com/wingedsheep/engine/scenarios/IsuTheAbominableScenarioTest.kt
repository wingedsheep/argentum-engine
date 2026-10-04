package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ice.cards.SnowCoveredForest
import com.wingedsheep.mtg.sets.definitions.ice.cards.SnowCoveredIsland
import com.wingedsheep.mtg.sets.definitions.j22.cards.IsuTheAbominable
import com.wingedsheep.mtg.sets.definitions.khm.cards.BergStrider
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Isu the Abominable — {3}{U}{U} Legendary Snow Creature — Yeti, 5/5 (J22).
 *
 *   You may look at the top card of your library any time.
 *   You may play snow lands and cast snow spells from the top of your library.
 *   Whenever another snow permanent you control enters, you may pay {G}, {W}, or {U}. If you do,
 *   put a +1/+1 counter on Isu.
 */
class IsuTheAbominableScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(IsuTheAbominable, SnowCoveredForest, SnowCoveredIsland, BergStrider)
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.offersFromLibrary(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any {
            it.sourceZone == "LIBRARY" &&
                ((it.action as? PlayLand)?.cardId == cardId || (it.action as? CastSpell)?.cardId == cardId)
        }

    test("plays a snow land from the top, and its entering lets Isu grow by paying with it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val isu = driver.putCreatureOnBattlefield(me, "Isu the Abominable")
        val land = driver.putCardOnTopOfLibrary(me, "Snow-Covered Forest")

        driver.offersFromLibrary(me, land) shouldBe true
        driver.playLand(me, land).outcome shouldBe Outcome.Done
        driver.bothPass() // the trigger resolves

        // The only mana available is the Snow-Covered Forest itself: {G} is offered, {W} and {U} aren't.
        val decision = driver.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options shouldBe listOf("Pay {G}: put a +1/+1 counter on Isu", "Don't pay")
        driver.submitDecision(me, OptionChosenResponse(decision.id, 0))

        driver.isTapped(land) shouldBe true
        driver.plusOneCounters(isu) shouldBe 1
        driver.state.projectedState.getPower(isu) shouldBe 6
    }

    test("declining the payment leaves Isu without a counter") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val isu = driver.putCreatureOnBattlefield(me, "Isu the Abominable")
        val land = driver.putCardInHand(me, "Snow-Covered Island")
        driver.playLand(me, land)
        driver.bothPass()

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options shouldBe listOf("Pay {U}: put a +1/+1 counter on Isu", "Don't pay")
        driver.submitDecision(me, OptionChosenResponse(decision.id, 1))

        driver.isTapped(land) shouldBe false
        driver.plusOneCounters(isu) shouldBe 0
    }

    test("a non-snow land on top can't be played") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Isu the Abominable")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")

        driver.offersFromLibrary(me, forest) shouldBe false
        driver.playLand(me, forest).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        driver.findPermanent(me, "Forest") shouldBe null
    }

    test("casts a snow spell from the top, but not a non-snow one") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Isu the Abominable")
        driver.giveMana(me, Color.BLUE, 5)
        driver.giveMana(me, Color.GREEN, 2) // enough for Grizzly Bears too, so only the filter refuses it

        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        driver.offersFromLibrary(me, bears) shouldBe false

        val strider = driver.putCardOnTopOfLibrary(me, "Berg Strider")
        driver.offersFromLibrary(me, strider) shouldBe true
        driver.castSpell(me, strider).outcome shouldBe Outcome.Done
        driver.state.stack.isNotEmpty() shouldBe true
    }

    test("a non-snow permanent entering doesn't trigger Isu") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val isu = driver.putCreatureOnBattlefield(me, "Isu the Abominable")
        val forest = driver.putCardInHand(me, "Forest")
        driver.playLand(me, forest)

        driver.state.stack.isEmpty() shouldBe true
        driver.state.pendingDecision shouldBe null
        driver.plusOneCounters(isu) shouldBe 0
        driver.findPermanent(me, "Forest") shouldNotBe null
    }
})
