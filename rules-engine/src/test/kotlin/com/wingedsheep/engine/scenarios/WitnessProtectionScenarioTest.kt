package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.snc.cards.WitnessProtection
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Witness Protection
 * {U} Enchantment — Aura
 * Enchant creature
 * Enchanted creature loses all abilities and is a green and white Citizen creature with base
 * power and toughness 1/1 named Legitimate Businessperson. (It loses all other colors, card
 * types, creature types, and names.)
 */
class WitnessProtectionScenarioTest : FunSpec({

    val projector = StateProjector()
    val allCards = TestCards.all + listOf(WitnessProtection)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("enchanted creature loses all abilities and becomes a 1/1 green/white Citizen named Legitimate Businessperson") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Test Hasty Prospector: legendary 2/1 red Monkey Pirate with Haste + a mana ability.
        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Witness Protection")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, aura, listOf(ragavan))
        driver.bothPass()

        val projected = driver.state.projectedState

        // Base power/toughness set to 1/1 (Layer 7b) — overwrites the printed 2/1.
        projector.getProjectedPower(driver.state, ragavan) shouldBe 1
        projector.getProjectedToughness(driver.state, ragavan) shouldBe 1

        // Loses all abilities (Layer 6) — Haste and the mana ability are gone.
        projected.hasLostAllAbilities(ragavan) shouldBe true
        projected.hasKeyword(ragavan, Keyword.HASTE) shouldBe false

        // Becomes a green/white Citizen creature, losing its other colors/types (Layers 4/5).
        projected.hasColor(ragavan, Color.GREEN) shouldBe true
        projected.hasColor(ragavan, Color.WHITE) shouldBe true
        projected.hasColor(ragavan, Color.RED) shouldBe false
        projected.hasType(ragavan, "CREATURE") shouldBe true
        projected.hasSubtype(ragavan, "Citizen") shouldBe true
        projected.hasSubtype(ragavan, "Monkey") shouldBe false
        projected.hasSubtype(ragavan, "Pirate") shouldBe false

        // Renamed (CR 612.8, Layer 3) — overwrites the printed name.
        projected.getName(ragavan) shouldBe "Legitimate Businessperson"

        // Still legendary: "loses all other card types" doesn't touch supertypes (CR 205.4a).
        projected.isLegendary(ragavan) shouldBe true

        // The rename is visible to the client, not just the rules-engine projection.
        val view = ClientStateTransformer(cardRegistry = driver.cardRegistry)
            .transform(driver.state, viewingPlayerId = player)
        view.cards[ragavan]?.name shouldBe "Legitimate Businessperson"
    }

    test("destroying the Aura reverts the enchanted creature to its original characteristics") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Witness Protection")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, aura, listOf(ragavan))
        driver.bothPass()

        // Sanity: the transform is active while the Aura is attached.
        driver.state.projectedState.getName(ragavan) shouldBe "Legitimate Businessperson"

        // The Aura's continuous effect comes from a static ability on the Aura permanent itself;
        // once it leaves the battlefield, the continuous effect ends and every characteristic the
        // Aura set reverts — there's no lingering "until end of turn" duration on Witness Protection.
        val auraOnBattlefield = driver.getPermanents(player).first { driver.getCardName(it) == "Witness Protection" }
        driver.moveToGraveyard(auraOnBattlefield)

        val projected = driver.state.projectedState
        projector.getProjectedPower(driver.state, ragavan) shouldBe 2
        projector.getProjectedToughness(driver.state, ragavan) shouldBe 1
        projected.hasLostAllAbilities(ragavan) shouldBe false
        projected.hasKeyword(ragavan, Keyword.HASTE) shouldBe true
        projected.hasColor(ragavan, Color.RED) shouldBe true
        projected.hasColor(ragavan, Color.GREEN) shouldBe false
        projected.hasSubtype(ragavan, "Monkey") shouldBe true
        projected.hasSubtype(ragavan, "Citizen") shouldBe false
        // No more projected override — name falls back to the base CardComponent.
        projected.getName(ragavan) shouldBe null
        driver.getCardName(ragavan) shouldBe "Test Hasty Prospector"
    }

    test("two different legendary creatures both renamed to Legitimate Businessperson trigger the legend rule") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two distinctly-named legendary creatures — the legend rule would NOT normally apply
        // (see LegendRuleTest's "different legendary names" case).
        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val ghalta = driver.putCreatureOnBattlefield(player, "Ghalta, Primal Hunger")

        // Enchant each with its own copy of Witness Protection.
        val aura1 = driver.putCardInHand(player, "Witness Protection")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, aura1, listOf(ragavan))
        driver.bothPass()

        val aura2 = driver.putCardInHand(player, "Witness Protection")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, aura2, listOf(ghalta))
        driver.bothPass()

        // Both are now legendary permanents sharing the projected name "Legitimate
        // Businessperson" (CR 612.8 + CR 704.5j) — the legend rule fires even though their
        // printed names differ.
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.prompt.contains("Legitimate Businessperson") shouldBe true
        decision.options.toSet() shouldBe setOf(ragavan, ghalta)
        decision.minSelections shouldBe 1
        decision.maxSelections shouldBe 1
        decision.context.phase shouldBe DecisionPhase.STATE_BASED

        driver.submitCardSelection(player, listOf(ragavan))

        val remaining = driver.getPermanents(player)
        remaining.any { it == ragavan } shouldBe true
        remaining.any { it == ghalta } shouldBe false
    }
})
