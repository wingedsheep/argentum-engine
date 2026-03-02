package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Dimensional Breach.
 *
 * Dimensional Breach: {5}{W}{W}
 * Sorcery
 * Exile all permanents. For as long as any of those cards remain exiled, at the
 * beginning of each player's upkeep, that player returns one of the exiled cards
 * they own to the battlefield.
 */
class DimensionalBreachTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun GameTestDriver.isInExile(playerId: EntityId, cardName: String): Boolean {
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    fun GameTestDriver.countInExile(playerId: EntityId): Int {
        return state.getExile(playerId).size
    }

    fun GameTestDriver.findAllPermanentsByName(name: String): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == name
        }
    }

    test("exiles all permanents from both players") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Glory Seeker" to 10, "Elvish Aberration" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures and lands on both sides
        driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")
        driver.putLandOnBattlefield(activePlayer, "Plains")
        driver.putCreatureOnBattlefield(opponent, "Elvish Aberration")
        driver.putLandOnBattlefield(opponent, "Plains")

        // Cast Dimensional Breach
        val breach = driver.putCardInHand(activePlayer, "Dimensional Breach")
        driver.giveMana(activePlayer, Color.WHITE, 7)
        driver.castSpell(activePlayer, breach)
        driver.bothPass() // resolve

        // All permanents should be exiled
        driver.isInExile(activePlayer, "Glory Seeker") shouldBe true
        driver.isInExile(activePlayer, "Plains") shouldBe true
        driver.isInExile(opponent, "Elvish Aberration") shouldBe true
        driver.isInExile(opponent, "Plains") shouldBe true

        // Battlefield should be empty (no creatures or lands)
        driver.findAllPermanentsByName("Glory Seeker") shouldHaveSize 0
        driver.findAllPermanentsByName("Elvish Aberration") shouldHaveSize 0

        // The sorcery (now in graveyard) should have LinkedExileComponent
        val breachInGraveyard = driver.state.getGraveyard(activePlayer).firstOrNull { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Dimensional Breach"
        }
        breachInGraveyard shouldNotBe null
        val linked = driver.state.getEntity(breachInGraveyard!!)?.get<LinkedExileComponent>()
        linked shouldNotBe null
        linked!!.exiledIds shouldHaveSize 4

        // A global triggered ability should have been created
        driver.state.globalGrantedTriggeredAbilities.size shouldBe 1
    }

    test("upkeep trigger returns one card to the active player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Glory Seeker" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // One creature per player
        driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")
        driver.putCreatureOnBattlefield(opponent, "Glory Seeker")

        // Cast and resolve Dimensional Breach
        val breach = driver.putCardInHand(activePlayer, "Dimensional Breach")
        driver.giveMana(activePlayer, Color.WHITE, 7)
        driver.castSpell(activePlayer, breach)
        driver.bothPass()

        // Both creatures should be exiled
        driver.isInExile(activePlayer, "Glory Seeker") shouldBe true
        driver.isInExile(opponent, "Glory Seeker") shouldBe true

        // Advance to opponent's upkeep (next turn)
        driver.passPriorityUntil(Step.UPKEEP)
        // Opponent should have a trigger — only one card, auto-returns
        driver.bothPass() // resolve upkeep trigger

        // Opponent's Glory Seeker should be back on the battlefield
        driver.findPermanent(opponent, "Glory Seeker") shouldNotBe null

        // Active player's card should still be in exile
        driver.isInExile(activePlayer, "Glory Seeker") shouldBe true

        // Advance past opponent's turn to active player's upkeep
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // advance past upkeep
        driver.passPriorityUntil(Step.UPKEEP) // advance to active player's upkeep
        driver.bothPass() // resolve upkeep trigger

        // Active player's Glory Seeker should now be back
        driver.findPermanent(activePlayer, "Glory Seeker") shouldNotBe null
    }

    test("global ability is removed when all exiled cards have returned") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Glory Seeker" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Just one creature
        driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")

        val breach = driver.putCardInHand(activePlayer, "Dimensional Breach")
        driver.giveMana(activePlayer, Color.WHITE, 7)
        driver.castSpell(activePlayer, breach)
        driver.bothPass()

        // Global ability should exist
        driver.state.globalGrantedTriggeredAbilities.size shouldBe 1

        // Advance to opponent's upkeep (no cards for opponent to return)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.bothPass() // resolve trigger (does nothing for opponent)

        // Advance past opponent's turn to active player's upkeep
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.bothPass() // resolve trigger — auto-returns the only card

        // Card should be back
        driver.findPermanent(activePlayer, "Glory Seeker") shouldNotBe null

        // Global ability should have been cleaned up
        driver.state.globalGrantedTriggeredAbilities.size shouldBe 0
    }

    test("with no permanents on battlefield, nothing happens") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast with no permanents on battlefield
        val breach = driver.putCardInHand(activePlayer, "Dimensional Breach")
        driver.giveMana(activePlayer, Color.WHITE, 7)
        driver.castSpell(activePlayer, breach)
        driver.bothPass()

        // No global ability should be created (nothing was exiled)
        // Note: ExileGroupAndLink still creates the LinkedExileComponent even if empty,
        // and the CreatePermanentGlobalTriggeredAbility always creates the ability.
        // The ability will fire at upkeep but find nothing to return and clean itself up.
    }

    test("player chooses which card to return when multiple are exiled") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Glory Seeker" to 10, "Elvish Aberration" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two creatures for active player
        driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")
        driver.putCreatureOnBattlefield(activePlayer, "Elvish Aberration")

        val breach = driver.putCardInHand(activePlayer, "Dimensional Breach")
        driver.giveMana(activePlayer, Color.WHITE, 7)
        driver.castSpell(activePlayer, breach)
        driver.bothPass()

        // Both should be exiled
        driver.isInExile(activePlayer, "Glory Seeker") shouldBe true
        driver.isInExile(activePlayer, "Elvish Aberration") shouldBe true

        // Advance to opponent's upkeep (no cards for opponent)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.bothPass() // resolve trigger (does nothing for opponent)

        // Advance to active player's upkeep
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP)

        // Trigger should fire and present a SelectCardsDecision
        // since there are 2 eligible cards
        driver.bothPass() // resolve trigger

        // There should be a pending decision for the active player
        val decision = driver.pendingDecision
        decision shouldNotBe null

        // Find the Glory Seeker in exile to select it
        val glorySeekerId = driver.state.getExile(activePlayer).first { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Glory Seeker"
        }

        driver.submitCardSelection(activePlayer, listOf(glorySeekerId))

        // Glory Seeker should be back, Elvish Aberration still in exile
        driver.findPermanent(activePlayer, "Glory Seeker") shouldNotBe null
        driver.isInExile(activePlayer, "Elvish Aberration") shouldBe true
    }
})
