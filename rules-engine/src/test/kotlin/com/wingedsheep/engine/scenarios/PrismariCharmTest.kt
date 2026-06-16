package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.PrismariCharm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Prismari Charm (SOS #211).
 *
 * Prismari Charm {U}{R}
 * Instant
 * Choose one —
 * • Surveil 2, then draw a card.
 * • Prismari Charm deals 1 damage to each of one or two targets.
 * • Return target nonland permanent to its owner's hand.
 */
class PrismariCharmTest : FunSpec({

    val BigCreature = CardDefinition.creature(
        name = "Prismari Ox",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = emptySet(),
        power = 3,
        toughness = 3
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PrismariCharm, BigCreature))
        return driver
    }

    fun castAndChooseMode(driver: GameTestDriver, caster: EntityId, modePrefix: String) {
        driver.giveMana(caster, Color.BLUE, 1)
        driver.giveMana(caster, Color.RED, 1)
        val charm = driver.putCardInHand(caster, "Prismari Charm")
        driver.castSpell(caster, charm)
        val modeDecision = driver.pendingDecision
        modeDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val idx = modeDecision.options.indexOfFirst { it.startsWith(modePrefix) }
        require(idx >= 0) { "Mode '$modePrefix' not offered; options=${modeDecision.options}" }
        driver.submitDecision(caster, OptionChosenResponse(modeDecision.id, idx))
    }

    test("mode 1 - surveil 2 then draw a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Seed a known library top so surveil presents two cards.
        driver.putCardOnTopOfLibrary(me, "Island")
        driver.putCardOnTopOfLibrary(me, "Mountain")

        val handBefore = driver.getHandSize(me)
        castAndChooseMode(driver, me, "Surveil 2")
        driver.bothPass()

        // Surveil 2 pauses for the keep/graveyard choice — keep both on top (bin none).
        val select = driver.pendingDecision as SelectCardsDecision
        select.options.size shouldBe 2
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = emptyList()))

        // Then order the kept cards back on top.
        val reorder = driver.pendingDecision as ReorderLibraryDecision
        driver.submitOrderedResponse(me, reorder.cards)

        // Net effect of surveil-then-draw: hand grows by one (the drawn card).
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("mode 2 - deals 1 damage to each of two targets") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ox = driver.putCreatureOnBattlefield(opp, "Prismari Ox") // 3/3

        castAndChooseMode(driver, me, "Prismari Charm deals")
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        // Two targets: the opponent's ox and the opponent themselves.
        driver.submitDecision(me, TargetsResponse(targetDecision.id, mapOf(0 to listOf(ox, opp))))
        driver.bothPass()

        driver.state.getEntity(ox)?.get<DamageComponent>()?.amount shouldBe 1
        driver.getLifeTotal(opp) shouldBe 19
    }

    test("mode 3 - return target nonland permanent to its owner's hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ox = driver.putCreatureOnBattlefield(opp, "Prismari Ox")

        castAndChooseMode(driver, me, "Return target nonland")
        val targetDecision = driver.pendingDecision
        targetDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targetDecision.legalTargets[0]!!.contains(ox) shouldBe true
        driver.submitDecision(me, TargetsResponse(targetDecision.id, mapOf(0 to listOf(ox))))
        driver.bothPass()

        driver.findPermanent(opp, "Prismari Ox") shouldBe null
        driver.findCardInHand(opp, "Prismari Ox") shouldBe ox
    }
})
