package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.KefkaCourtMage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kefka, Court Mage // Kefka, Ruler of Ruin (FIN #231).
 *
 * Pins the card-specific assembly, and in particular the new
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.DistinctCardTypesInCollections] dynamic
 * amount that drives the front's "draw a card for each card type among cards discarded this way":
 *  - Two different card types discarded (an instant + a creature) → the controller draws 2.
 *  - Two cards of the same type discarded (two creatures) → distinct types collapse to 1 → draws 1.
 *  - The {8} sorcery-speed ability edicts each opponent and transforms Kefka to its back face.
 *
 * The discard/draw pipeline discards the controller's card plus one opponent's card (two-player
 * exact; single-opponent in multiplayer, matching Syphon Mind's precedent).
 */
class KefkaCourtMageScenarioTest : FunSpec({

    val eightManaAbilityId = KefkaCourtMage.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        // startingPlayer = 0 → player1 is the active player, so player1 is "you" for the enter
        // trigger and discards first (active player before non-active — CR 101.4).
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Walk the pending stack/decisions to resolution. For each discard decision, submit the card
     * [discards] maps that player to (the planted card of a known type); for any other decision,
     * pass priority.
     */
    fun resolve(driver: GameTestDriver, discards: Map<EntityId, EntityId>) {
        var guard = 0
        while (guard++ < 80 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            val decision = driver.pendingDecision
            if (decision is SelectCardsDecision) {
                val planned = discards[decision.playerId]
                if (planned != null && planned in decision.options) {
                    driver.submitCardSelection(decision.playerId, listOf(planned))
                } else {
                    // Fall back to the first offered card (only the opponent's exact choice is pinned).
                    driver.submitCardSelection(decision.playerId, listOf(decision.options.first()))
                }
            } else {
                driver.bothPass()
            }
        }
    }

    fun giveKefkaMana(driver: GameTestDriver, player: EntityId) {
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveMana(player, Color.RED, 3) // {R} + two generic
    }

    test("front: draws one card per distinct card type discarded (instant + creature → draw 2)") {
        val driver = createDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)

        val yourInstant = driver.putCardInHand(you, "Lightning Bolt")
        val oppCreature = driver.putCardInHand(opponent, "Savannah Lions")

        giveKefkaMana(driver, you)
        val kefka = driver.putCardInHand(you, "Kefka, Court Mage")
        driver.castSpell(you, kefka).isSuccess shouldBe true

        val eventsBefore = driver.events.size
        resolve(driver, mapOf(you to yourInstant, opponent to oppCreature))

        // instant + creature = 2 distinct card types → you draw 2.
        val drawn = driver.events.drop(eventsBefore)
            .filterIsInstance<CardsDrawnEvent>()
            .filter { it.playerId == you }
            .sumOf { it.count }
        drawn shouldBe 2
    }

    test("front: identical discarded card types collapse (two creatures → draw 1)") {
        val driver = createDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)

        val yourCreature = driver.putCardInHand(you, "Savannah Lions")
        val oppCreature = driver.putCardInHand(opponent, "Centaur Courser")

        giveKefkaMana(driver, you)
        val kefka = driver.putCardInHand(you, "Kefka, Court Mage")
        driver.castSpell(you, kefka).isSuccess shouldBe true

        val eventsBefore = driver.events.size
        resolve(driver, mapOf(you to yourCreature, opponent to oppCreature))

        // creature + creature = 1 distinct card type → you draw 1.
        val drawn = driver.events.drop(eventsBefore)
            .filterIsInstance<CardsDrawnEvent>()
            .filter { it.playerId == you }
            .sumOf { it.count }
        drawn shouldBe 1
    }

    test("{8} ability: each opponent sacrifices a permanent and Kefka transforms") {
        val driver = createDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)

        val kefka = driver.putCreatureOnBattlefield(you, "Kefka, Court Mage")
        val victim = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        driver.giveMana(you, Color.RED, 8)

        driver.submit(ActivateAbility(playerId = you, sourceId = kefka, abilityId = eightManaAbilityId))
            .isSuccess shouldBe true
        resolve(driver, emptyMap())

        // The opponent's only permanent is sacrificed.
        driver.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD)).contains(victim) shouldBe true
        // Kefka has transformed to its back face.
        driver.state.getEntity(kefka)!!.get<CardComponent>()!!.name shouldBe "Kefka, Ruler of Ruin"
    }
})
