package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ice.cards.SnowCoveredIsland
import com.wingedsheep.mtg.sets.definitions.mh1.cards.MaritLagesSlumber
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Marit Lage's Slumber — {1}{U} Legendary Snow Enchantment (MH1; reprinted in J22).
 *
 *   Whenever Marit Lage's Slumber or another snow permanent you control enters, scry 1.
 *   At the beginning of your upkeep, if you control ten or more snow permanents, sacrifice Marit
 *   Lage's Slumber. If you do, create Marit Lage, a legendary 20/20 black Avatar creature token
 *   with flying and indestructible.
 */
class MaritLagesSlumberScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MaritLagesSlumber, SnowCoveredIsland))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    /** Give [player] the Slumber plus [snowLands] Snow-Covered Islands, then advance to their upkeep. */
    fun GameTestDriver.opponentUpkeepWith(snowLands: Int): Pair<EntityId, EntityId> {
        val opponent = getOpponent(activePlayer!!)
        val slumber = putPermanentOnBattlefield(opponent, "Marit Lage's Slumber")
        repeat(snowLands) { putLandOnBattlefield(opponent, "Snow-Covered Island") }
        passPriorityUntil(Step.UPKEEP)
        activePlayer shouldBe opponent
        return opponent to slumber
    }

    test("casting it scries 1 off its own entering") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        val slumber = driver.putCardInHand(me, "Marit Lage's Slumber")
        driver.giveMana(me, Color.BLUE, 2)

        driver.submit(CastSpell(me, slumber, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        driver.bothPass() // the enchantment resolves
        driver.state.stack.size shouldBe 1
        driver.bothPass() // the scry trigger resolves

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(forest)
        driver.submitCardSelection(me, listOf(forest))

        driver.state.getLibrary(me).last() shouldBe forest
    }

    test("another snow permanent entering under your control scries 1") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Marit Lage's Slumber")
        val top = driver.state.getLibrary(me).first()
        val land = driver.putCardInHand(me, "Snow-Covered Island")

        driver.playLand(me, land).outcome shouldBe Outcome.Done
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldBe listOf(top)
        driver.submitCardSelection(me, listOf(top))
        driver.state.getLibrary(me).last() shouldBe top
    }

    test("a non-snow permanent entering doesn't trigger it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Marit Lage's Slumber")

        driver.playLand(me, driver.putCardInHand(me, "Island")).outcome shouldBe Outcome.Done
        driver.state.stack.size shouldBe 0
        driver.state.pendingDecision.shouldBeNull()
    }

    test("a snow permanent entering under another player's control doesn't trigger it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Marit Lage's Slumber")

        driver.playLand(me, driver.putCardInHand(me, "Snow-Covered Island")).outcome shouldBe Outcome.Done
        driver.state.stack.size shouldBe 0
        driver.state.pendingDecision.shouldBeNull()
    }

    test("with ten snow permanents at upkeep it is sacrificed for Marit Lage") {
        val driver = createDriver()
        val (opponent, slumber) = driver.opponentUpkeepWith(snowLands = 9) // 9 lands + the Slumber = 10

        driver.state.stack.size shouldBe 1
        driver.resolveStack()

        driver.findPermanent(opponent, "Marit Lage's Slumber").shouldBeNull()
        driver.getGraveyard(opponent).contains(slumber) shouldBe true

        val marit = driver.findPermanent(opponent, "Marit Lage").shouldNotBeNull()
        val projected = driver.state.projectedState
        projected.getPower(marit) shouldBe 20
        projected.getToughness(marit) shouldBe 20
        projected.hasKeyword(marit, Keyword.FLYING) shouldBe true
        projected.hasKeyword(marit, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.isLegendary(marit) shouldBe true
        projected.hasColor(marit, Color.BLACK) shouldBe true
        projected.getColors(marit).size shouldBe 1
        projected.hasSubtype(marit, "Avatar") shouldBe true
    }

    test("with only nine snow permanents the upkeep ability doesn't trigger") {
        val driver = createDriver()
        val (opponent, slumber) = driver.opponentUpkeepWith(snowLands = 8)

        driver.state.stack.size shouldBe 0
        driver.findPermanent(opponent, "Marit Lage's Slumber") shouldBe slumber
        driver.findPermanent(opponent, "Marit Lage").shouldBeNull()
    }

    test("dropping below ten snow permanents before resolution means no sacrifice and no token") {
        val driver = createDriver()
        val (opponent, slumber) = driver.opponentUpkeepWith(snowLands = 9)
        driver.state.stack.size shouldBe 1

        driver.moveToGraveyard(driver.getLands(opponent).first())
        driver.resolveStack()

        driver.findPermanent(opponent, "Marit Lage's Slumber") shouldBe slumber
        driver.findPermanent(opponent, "Marit Lage").shouldBeNull()
    }

    test("if the Slumber has left the battlefield before resolution, no Marit Lage is created") {
        val driver = createDriver()
        val (opponent, slumber) = driver.opponentUpkeepWith(snowLands = 11) // still ten without it
        driver.state.stack.size shouldBe 1

        driver.moveToGraveyard(slumber)
        driver.resolveStack()

        driver.findPermanent(opponent, "Marit Lage").shouldBeNull()
    }
})
