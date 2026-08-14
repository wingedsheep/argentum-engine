package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SpiderManNoMore
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Spider-Man No More
 * {1}{U} Enchantment — Aura
 * Enchant creature
 * Enchanted creature is a Citizen with base power and toughness 1/1. It has defender and loses
 * all other abilities. (It also loses all other creature types.)
 *
 * Unlike Witness Protection this aura does NOT recolor or rename the creature — it only sets the
 * subtype to Citizen, sets base P/T 1/1, grants defender, and strips every other ability.
 */
class SpiderManNoMoreScenarioTest : FunSpec({

    val projector = StateProjector()
    val allCards = TestCards.all + listOf(SpiderManNoMore)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("enchanted creature becomes a 1/1 Citizen with defender, keeps its color, and loses all other abilities and types") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Test Hasty Prospector: legendary 2/1 red Monkey Pirate with Haste + a mana ability.
        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Spider-Man No More")
        driver.giveMana(player, Color.BLUE, 2)
        driver.castSpell(player, aura, listOf(ragavan))
        driver.bothPass()

        val projected = driver.state.projectedState

        // Base power/toughness set to 1/1 (Layer 7b) — overwrites the printed 2/1.
        projector.getProjectedPower(driver.state, ragavan) shouldBe 1
        projector.getProjectedToughness(driver.state, ragavan) shouldBe 1

        // Loses all other abilities (Layer 6) — Haste and the mana ability are gone.
        projected.hasLostAllAbilities(ragavan) shouldBe true
        projected.hasKeyword(ragavan, Keyword.HASTE) shouldBe false

        // "It has defender" survives the "loses all other abilities" (granted after the loss).
        projected.hasKeyword(ragavan, Keyword.DEFENDER) shouldBe true

        // Becomes a Citizen, losing its other creature types (Layer 4); keeps CREATURE.
        projected.hasType(ragavan, "CREATURE") shouldBe true
        projected.hasSubtype(ragavan, "Citizen") shouldBe true
        projected.hasSubtype(ragavan, "Monkey") shouldBe false
        projected.hasSubtype(ragavan, "Pirate") shouldBe false

        // Color is NOT changed by this aura — Ragavan stays red.
        projected.hasColor(ragavan, Color.RED) shouldBe true

        // Name is NOT changed — no projected override.
        projected.getName(ragavan) shouldBe null

        // Still legendary: changing creature types doesn't touch supertypes (CR 205.4b).
        projected.isLegendary(ragavan) shouldBe true
    }

    test("destroying the Aura reverts the enchanted creature to its original characteristics") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ragavan = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Spider-Man No More")
        driver.giveMana(player, Color.BLUE, 2)
        driver.castSpell(player, aura, listOf(ragavan))
        driver.bothPass()

        // Sanity: the transform is active while the Aura is attached.
        driver.state.projectedState.hasSubtype(ragavan, "Citizen") shouldBe true
        driver.state.projectedState.hasKeyword(ragavan, Keyword.DEFENDER) shouldBe true

        // The transform comes from static abilities on the Aura permanent itself; once it leaves
        // the battlefield the continuous effect ends and every characteristic reverts.
        val auraOnBattlefield = driver.getPermanents(player).first { driver.getCardName(it) == "Spider-Man No More" }
        driver.moveToGraveyard(auraOnBattlefield)

        val projected = driver.state.projectedState
        projector.getProjectedPower(driver.state, ragavan) shouldBe 2
        projector.getProjectedToughness(driver.state, ragavan) shouldBe 1
        projected.hasLostAllAbilities(ragavan) shouldBe false
        projected.hasKeyword(ragavan, Keyword.HASTE) shouldBe true
        projected.hasKeyword(ragavan, Keyword.DEFENDER) shouldBe false
        projected.hasSubtype(ragavan, "Monkey") shouldBe true
        projected.hasSubtype(ragavan, "Citizen") shouldBe false
        driver.getCardName(ragavan) shouldBe "Test Hasty Prospector"
    }
})
