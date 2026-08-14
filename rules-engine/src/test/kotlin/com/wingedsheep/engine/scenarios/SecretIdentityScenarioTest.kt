package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SecretIdentity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Secret Identity (SPM #43)
 * {U} Instant
 * Choose one —
 * • Conceal — Until end of turn, target creature you control becomes a Citizen with base power
 *   and toughness 1/1 and gains hexproof.
 * • Reveal — Until end of turn, target creature you control becomes a Hero with base power and
 *   toughness 3/4 and gains flying and vigilance.
 *
 * Each mode is a one-shot, end-of-turn become-creature transform (subtype + base P/T + keywords),
 * so both modes are verified while active AND after they revert at end of turn.
 */
class SecretIdentityScenarioTest : FunSpec({

    val projector = StateProjector()
    val allCards = TestCards.all + listOf(SecretIdentity)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("Conceal — target becomes a 1/1 Citizen with hexproof until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Grizzly Bears: printed 2/2 Bear.
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Secret Identity")
        driver.giveMana(player, Color.BLUE, 1)

        // Cast choosing mode 0 (Conceal); the engine then pauses for the mode's target.
        val cast = driver.submit(
            CastSpell(playerId = player, cardId = spell, chosenModes = listOf(0))
        )
        cast.error shouldBe null
        driver.submitTargetSelection(player, listOf(bear))
        driver.bothPass()

        val projected = driver.state.projectedState

        // Base P/T set to 1/1 (Layer 7b), overwriting the printed 2/2.
        projector.getProjectedPower(driver.state, bear) shouldBe 1
        projector.getProjectedToughness(driver.state, bear) shouldBe 1

        // Becomes a Citizen (Layer 4), losing its other creature types; keeps CREATURE.
        projected.hasType(bear, "CREATURE") shouldBe true
        projected.hasSubtype(bear, "Citizen") shouldBe true
        projected.hasSubtype(bear, "Bear") shouldBe false

        // Gains hexproof (Layer 6); the two Reveal keywords are NOT granted.
        projected.hasKeyword(bear, Keyword.HEXPROOF) shouldBe true
        projected.hasKeyword(bear, Keyword.FLYING) shouldBe false
        projected.hasKeyword(bear, Keyword.VIGILANCE) shouldBe false

        // Advance past end of turn — every part of the transform reverts.
        driver.passPriorityUntil(Step.CLEANUP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // opponent's turn

        val reverted = driver.state.projectedState
        projector.getProjectedPower(driver.state, bear) shouldBe 2
        projector.getProjectedToughness(driver.state, bear) shouldBe 2
        reverted.hasSubtype(bear, "Bear") shouldBe true
        reverted.hasSubtype(bear, "Citizen") shouldBe false
        reverted.hasKeyword(bear, Keyword.HEXPROOF) shouldBe false
    }

    test("Reveal — target becomes a 3/4 Hero with flying and vigilance until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Secret Identity")
        driver.giveMana(player, Color.BLUE, 1)

        // Cast choosing mode 1 (Reveal); the engine then pauses for the mode's target.
        val cast = driver.submit(
            CastSpell(playerId = player, cardId = spell, chosenModes = listOf(1))
        )
        cast.error shouldBe null
        driver.submitTargetSelection(player, listOf(bear))
        driver.bothPass()

        val projected = driver.state.projectedState

        // Base P/T set to 3/4 (Layer 7b).
        projector.getProjectedPower(driver.state, bear) shouldBe 3
        projector.getProjectedToughness(driver.state, bear) shouldBe 4

        // Becomes a Hero (Layer 4), losing its other creature types; keeps CREATURE.
        projected.hasType(bear, "CREATURE") shouldBe true
        projected.hasSubtype(bear, "Hero") shouldBe true
        projected.hasSubtype(bear, "Bear") shouldBe false

        // Gains flying and vigilance (Layer 6); hexproof (the Conceal keyword) is NOT granted.
        projected.hasKeyword(bear, Keyword.FLYING) shouldBe true
        projected.hasKeyword(bear, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(bear, Keyword.HEXPROOF) shouldBe false

        // Advance past end of turn — every part of the transform reverts.
        driver.passPriorityUntil(Step.CLEANUP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // opponent's turn

        val reverted = driver.state.projectedState
        projector.getProjectedPower(driver.state, bear) shouldBe 2
        projector.getProjectedToughness(driver.state, bear) shouldBe 2
        reverted.hasSubtype(bear, "Bear") shouldBe true
        reverted.hasSubtype(bear, "Hero") shouldBe false
        reverted.hasKeyword(bear, Keyword.FLYING) shouldBe false
        reverted.hasKeyword(bear, Keyword.VIGILANCE) shouldBe false
    }
})
