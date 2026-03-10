package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.khans.cards.UginsNexus
import com.wingedsheep.mtg.sets.definitions.portal.cards.LastChance
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain

/**
 * Tests for Ugin's Nexus.
 *
 * Ability 1: "If a player would begin an extra turn, that player skips that turn instead."
 * Ability 2: "If Ugin's Nexus would be put into a graveyard from the battlefield,
 *             instead exile it and take an extra turn after this one."
 */
class UginsNexusTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UginsNexus)
        driver.registerCard(LastChance)
        driver.registerCard(Shatter)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("Ugin's Nexus prevents extra turns from Last Chance") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Ugin's Nexus on the battlefield
        driver.putPermanentOnBattlefield(caster, "Ugin's Nexus")

        // Cast Last Chance
        val lastChance = driver.putCardInHand(caster, "Last Chance")
        driver.giveMana(caster, Color.RED, 2)
        driver.castSpell(caster, lastChance, emptyList())
        driver.bothPass()

        // Opponent should NOT have SkipNextTurnComponent — extra turns are prevented
        driver.state.getEntity(opponent)?.has<SkipNextTurnComponent>() shouldBe false
    }

    test("destroying Ugin's Nexus exiles it and grants extra turn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Ugin's Nexus on the battlefield
        val nexusId = driver.putPermanentOnBattlefield(caster, "Ugin's Nexus")

        // Cast Shatter targeting Ugin's Nexus
        val shatter = driver.putCardInHand(caster, "Shatter")
        driver.giveMana(caster, Color.RED, 2)
        driver.castSpell(caster, shatter, listOf(nexusId))
        driver.bothPass()

        // Ugin's Nexus should be in exile (not graveyard)
        driver.getExileCardNames(caster) shouldContain "Ugin's Nexus"
        driver.getGraveyardCardNames(caster).contains("Ugin's Nexus") shouldBe false

        // Opponent should have SkipNextTurnComponent (extra turn for caster)
        // Ugin's Nexus is no longer on the battlefield, so its prevention doesn't apply
        driver.state.getEntity(opponent)?.has<SkipNextTurnComponent>() shouldBe true
    }

    test("extra turn from Nexus destruction actually gives caster another turn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nexusId = driver.putPermanentOnBattlefield(caster, "Ugin's Nexus")
        val shatter = driver.putCardInHand(caster, "Shatter")
        driver.giveMana(caster, Color.RED, 2)
        driver.castSpell(caster, shatter, listOf(nexusId))
        driver.bothPass()

        // End the current turn
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        // Caster should still be active player (opponent's turn was skipped)
        driver.activePlayer shouldBe caster
    }
})
