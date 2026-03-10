package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.CleverImpersonator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Clever Impersonator.
 *
 * Clever Impersonator: {2}{U}{U}
 * Creature — Shapeshifter
 * 0/0
 * You may have Clever Impersonator enter the battlefield as a copy of any nonland permanent
 * on the battlefield.
 */
class CleverImpersonatorTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CleverImpersonator)
        return driver
    }

    test("Clever Impersonator can copy a creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val warrior = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        val impersonator = driver.putCardInHand(activePlayer, "Clever Impersonator")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, impersonator)
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision as SelectCardsDecision
        decision.options shouldContain warrior
        driver.submitCardSelection(activePlayer, listOf(warrior))

        val card = driver.state.getEntity(impersonator)?.get<CardComponent>()
        card shouldNotBe null
        card!!.name shouldBe "Elvish Warrior"
        projector.getProjectedPower(driver.state, impersonator) shouldBe 2
        projector.getProjectedToughness(driver.state, impersonator) shouldBe 3

        val copyOf = driver.state.getEntity(impersonator)?.get<CopyOfComponent>()
        copyOf shouldNotBe null
        copyOf!!.originalCardDefinitionId shouldBe "Clever Impersonator"
    }

    test("Clever Impersonator can copy a non-creature permanent (enchantment)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val enchantment = driver.putPermanentOnBattlefield(activePlayer, "Test Enchantment")
        val impersonator = driver.putCardInHand(activePlayer, "Clever Impersonator")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, impersonator)
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision as SelectCardsDecision
        decision.options shouldContain enchantment
        driver.submitCardSelection(activePlayer, listOf(enchantment))

        val card = driver.state.getEntity(impersonator)?.get<CardComponent>()
        card shouldNotBe null
        card!!.name shouldBe "Test Enchantment"
        card.typeLine.isEnchantment shouldBe true

        val copyOf = driver.state.getEntity(impersonator)?.get<CopyOfComponent>()
        copyOf shouldNotBe null
        copyOf!!.originalCardDefinitionId shouldBe "Clever Impersonator"
        copyOf.copiedCardDefinitionId shouldBe "Test Enchantment"
    }

    test("Clever Impersonator cannot copy lands") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only a land on the battlefield — no valid targets
        // (lands are already on battlefield from game init, but let's be explicit)
        // No non-land permanents should mean no decision

        val impersonator = driver.putCardInHand(activePlayer, "Clever Impersonator")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, impersonator)
        driver.bothPass()

        // Should enter as 0/0 and die — no decision presented since only lands exist
        driver.state.getBattlefield() shouldNotContain impersonator
    }

    test("Clever Impersonator selection shows non-creature permanents but not lands") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val enchantment = driver.putPermanentOnBattlefield(activePlayer, "Test Enchantment")
        val creature = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")

        val impersonator = driver.putCardInHand(activePlayer, "Clever Impersonator")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, impersonator)
        driver.bothPass()

        val decision = driver.pendingDecision as SelectCardsDecision
        // Both enchantment and creature should be selectable
        decision.options shouldContain enchantment
        decision.options shouldContain creature

        // Lands should NOT be in the options
        val lands = driver.state.getBattlefield().filter { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isLand == true
        }
        lands.forEach { landId ->
            decision.options shouldNotContain landId
        }

        driver.submitCardSelection(activePlayer, listOf(enchantment))

        val card = driver.state.getEntity(impersonator)?.get<CardComponent>()
        card!!.name shouldBe "Test Enchantment"
    }
})
