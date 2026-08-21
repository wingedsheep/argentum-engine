package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.KyloxsVoltstrider
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kylox's Voltstrider (MKM #215) — {1}{U}{R} Artifact — Vehicle 4/4.
 *
 * "Collect evidence 6: This Vehicle becomes an artifact creature until end of turn.
 *  Whenever this Vehicle attacks, you may cast an instant or sorcery spell from among cards exiled
 *  with it. If that spell would be put into a graveyard, put it on the bottom of its owner's
 *  library instead.
 *  Crew 2"
 *
 * Two pieces of engine vocabulary meet on this card, and every test below pins one of them:
 *
 * - **`Costs.CollectEvidence(n, linkToSource = true)`.** Collect evidence normally exiles and
 *   forgets. The link is what turns the payment into a durable pile the Vehicle owns, so the tests
 *   assert on [LinkedExileComponent] directly as well as on what the trigger later offers.
 * - **`insteadOfGraveyard = BOTTOM_OF_LIBRARY`.** The rider must move the spell on *resolution*,
 *   must be scoped to the one card cast (a declined pick keeps no marker), and must not fire for
 *   a copy of the card cast by any other route.
 *
 * The pile is cumulative across activations and prunes itself: a card that leaves exile is dropped
 * from every linked-exile pile, which is how a spell already cast and bottomed disappears from the
 * pool without this card doing any bookkeeping.
 */
class KyloxsVoltstriderScenarioTest : FunSpec({

    // Graveyard fodder, priced so a test can choose an exact collection rather than relying on the
    // resolver's auto-pick. Mana value 6 on its own reaches the threshold.
    val hulk = card("Test Evidence Hulk") {
        manaCost = "{6}"
        typeLine = "Artifact"
    }

    // The payoff: a targetless instant cheap enough to actually cast mid-combat, so the assertion
    // is about where the card ends up rather than about a target prompt.
    val jolt = card("Test Voltstrider Jolt") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(3) }
    }

    // A second castable card, so "choose up to one" has something to choose between.
    val surge = card("Test Voltstrider Surge") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    val animateAbilityId = KyloxsVoltstrider.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(KyloxsVoltstrider, hulk, jolt, surge))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.linkedPile(vehicleId: EntityId): List<String> =
        state.getEntity(vehicleId)?.get<LinkedExileComponent>()?.exiledIds
            .orEmpty()
            .mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    fun GameTestDriver.libraryNames(playerId: EntityId): List<String> =
        state.getZone(ZoneKey(playerId, Zone.LIBRARY))
            .mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    fun optionNames(driver: GameTestDriver, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { driver.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    /**
     * Put the Vehicle out, stock the graveyard, and pay "collect evidence 6" with exactly
     * [evidence]. Returns the Vehicle, animated and ready to attack.
     */
    fun GameTestDriver.animateWith(player: EntityId, evidence: List<EntityId>): EntityId {
        val vehicle = putPermanentOnBattlefield(player, "Kylox's Voltstrider")
        removeSummoningSickness(vehicle)
        submit(
            ActivateAbility(
                playerId = player,
                sourceId = vehicle,
                abilityId = animateAbilityId,
                costPayment = AdditionalCostPayment(exiledCards = evidence),
            )
        ).error shouldBe null
        bothPass()
        return vehicle
    }

    /** Attack with [attacker] and resolve until the trigger needs a decision. */
    fun GameTestDriver.attackAndResolve(you: EntityId, attacker: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(you, listOf(attacker), getOpponent(you)).error shouldBe null
        var guard = 0
        while (!isPaused && state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("collect evidence 6 exiles the chosen cards into the Vehicle's own pile and animates it") {
        val driver = newDriver()
        val you = driver.player1
        val fodder = driver.putCardInGraveyard(you, "Test Evidence Hulk")

        val vehicle = driver.animateWith(you, listOf(fodder))

        withClue("the paid cards left the graveyard for exile") {
            driver.getGraveyardCardNames(you).contains("Test Evidence Hulk") shouldBe false
            driver.getExile(you).contains(fodder) shouldBe true
        }
        withClue("linkToSource is what makes them 'cards exiled with it'") {
            driver.linkedPile(vehicle) shouldBe listOf("Test Evidence Hulk")
        }
        withClue("an uncrewed Vehicle is not a creature; the ability makes it one until end of turn") {
            driver.state.projectedState.isCreature(vehicle) shouldBe true
            driver.state.projectedState.getPower(vehicle) shouldBe 4
        }
    }

    test("the attack trigger offers only the instants and sorceries exiled with the Vehicle") {
        val driver = newDriver()
        val you = driver.player1
        val fodder = driver.putCardInGraveyard(you, "Test Evidence Hulk")
        val castable = driver.putCardInGraveyard(you, "Test Voltstrider Jolt")
        val alsoCastable = driver.putCardInGraveyard(you, "Test Voltstrider Surge")
        // Exiled by something else entirely — in exile, but not in the Vehicle's pile.
        driver.putCardInExile(you, "Test Voltstrider Surge")

        val vehicle = driver.animateWith(you, listOf(fodder, castable, alsoCastable))
        driver.attackAndResolve(you, vehicle)

        val decision = driver.pendingDecision as SelectCardsDecision
        withClue("the artifact is in the pile but isn't an instant or sorcery, and the separately-exiled card isn't in the pile at all") {
            decision.options.toSet() shouldBe setOf(castable, alsoCastable)
            optionNames(driver, decision) shouldBe setOf("Test Voltstrider Jolt", "Test Voltstrider Surge")
        }
        withClue("\"you may cast\" — declining is legal") {
            decision.minSelections shouldBe 0
            decision.maxSelections shouldBe 1
        }
    }

    test("the cast spell goes to the bottom of its owner's library, not the graveyard") {
        val driver = newDriver()
        val you = driver.player1
        val fodder = driver.putCardInGraveyard(you, "Test Evidence Hulk")
        val jolt = driver.putCardInGraveyard(you, "Test Voltstrider Jolt")

        val vehicle = driver.animateWith(you, listOf(fodder, jolt))
        val lifeBefore = driver.getLifeTotal(you)
        val librarySizeBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size

        driver.attackAndResolve(you, vehicle)
        // The cast pays the spell's normal mana cost — the card never says "without paying".
        driver.giveMana(you, Color.BLUE, 2)
        driver.submitCardSelection(you, listOf(jolt))

        var guard = 0
        while (guard++ < 20) {
            when {
                driver.isPaused -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }

        withClue("the instant resolved") {
            driver.getLifeTotal(you) shouldBe lifeBefore + 3
        }
        withClue("the rider redirects it out of the graveyard") {
            driver.getGraveyardCardNames(you).contains("Test Voltstrider Jolt") shouldBe false
            driver.getExile(you).contains(jolt) shouldBe false
        }
        withClue("...and onto the bottom of its owner's library") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).last() shouldBe jolt
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe librarySizeBefore + 1
        }
        withClue("a card that left exile is no longer 'exiled with it' — the pile prunes itself") {
            driver.linkedPile(vehicle) shouldBe listOf("Test Evidence Hulk")
            driver.getExile(you).contains(jolt) shouldBe false
        }
    }

    test("declining the cast leaves the card exiled and unmarked") {
        val driver = newDriver()
        val you = driver.player1
        val fodder = driver.putCardInGraveyard(you, "Test Evidence Hulk")
        val jolt = driver.putCardInGraveyard(you, "Test Voltstrider Jolt")

        val vehicle = driver.animateWith(you, listOf(fodder, jolt))
        driver.attackAndResolve(you, vehicle)
        driver.submitCardSelection(you, emptyList())

        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        withClue("nothing was cast; the pile is untouched and still available next attack") {
            driver.getExile(you).contains(jolt) shouldBe true
            driver.linkedPile(vehicle) shouldBe listOf("Test Evidence Hulk", "Test Voltstrider Jolt")
        }
        withClue("no destination rider is left behind on a card that wasn't cast") {
            driver.state.getEntity(jolt)
                ?.get<com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent>() shouldBe null
        }
    }

    test("a pile with nothing castable in it raises no decision at all") {
        val driver = newDriver()
        val you = driver.player1
        val fodder = driver.putCardInGraveyard(you, "Test Evidence Hulk")

        // Evidence 6 paid entirely with an artifact: the pile is non-empty, but nothing in it is
        // an instant or a sorcery, so "you may cast" has nothing to offer.
        val vehicle = driver.animateWith(you, listOf(fodder))
        driver.attackAndResolve(you, vehicle)

        withClue("an empty eligible set must not stall the trigger on a decision") {
            driver.isPaused shouldBe false
            driver.state.stack.isEmpty() shouldBe true
        }
        withClue("the artifact is still exiled with the Vehicle") {
            driver.linkedPile(vehicle) shouldBe listOf("Test Evidence Hulk")
        }
    }

    test("the printed crew keyword is on the definition") {
        KyloxsVoltstrider.keywordAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Numeric>()
            .single { it.keyword == com.wingedsheep.sdk.core.Keyword.CREW }
            .n shouldBe 2
    }
})
