package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.ktk.cards.UginsNexus
import com.wingedsheep.mtg.sets.definitions.por.cards.LastChance
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

    test("Ugin's Nexus skips the extra turn from Last Chance when it would begin, so the caster doesn't lose") {
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

        // The extra turn is created normally (rulings: not prevented, skipped as it would begin)
        driver.state.getEntity(caster)?.get<ExtraTurnsComponent>()?.pending?.size shouldBe 1

        // End the turn: the extra turn would begin, is skipped, and the opponent's turn follows.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe opponent
        driver.state.getEntity(caster)?.has<LoseAtEndStepComponent>() shouldBe false
        driver.state.getEntity(caster)?.get<ExtraTurnsComponent>()?.begun shouldBe 0
    }

    test("an extra turn created while Ugin's Nexus is out happens if it has left before the turn would begin") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nexusId = driver.putPermanentOnBattlefield(caster, "Ugin's Nexus")
        val lastChance = driver.putCardInHand(caster, "Last Chance")
        driver.giveMana(caster, Color.RED, 2)
        driver.castSpell(caster, lastChance, emptyList())
        driver.bothPass()

        // Shatter the Nexus: it is exiled and its own extra turn is created; both turns now happen.
        val shatter = driver.putCardInHand(caster, "Shatter")
        driver.giveMana(caster, Color.RED, 2)
        driver.castSpell(caster, shatter, listOf(nexusId))
        driver.bothPass()
        driver.state.getEntity(caster)?.get<ExtraTurnsComponent>()?.pending?.size shouldBe 2

        fun endTurn() {
            val result = driver.services.turnManager.endTurn(driver.state)
            result.error shouldBe null
            driver.replaceState(result.state)
        }
        endTurn()
        // The extra turn created last (the Nexus's own) goes first, then Last Chance's.
        driver.activePlayer shouldBe caster
        driver.state.getEntity(caster)?.has<LoseAtEndStepComponent>() shouldBe false
        endTurn()
        driver.activePlayer shouldBe caster
        driver.state.getEntity(caster)?.has<LoseAtEndStepComponent>() shouldBe true
        driver.state.getEntity(caster)?.get<ExtraTurnsComponent>()?.begun shouldBe 2
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
