package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.SearchDestination
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Weathered Wayfarer.
 *
 * Weathered Wayfarer: {W}
 * Creature — Human Nomad Cleric
 * 1/1
 * {W}, {T}: Search your library for a land card, reveal it, put it into your hand,
 * then shuffle. Activate only if an opponent controls more lands than you.
 */
class WeatheredWayfarerTest : FunSpec({

    val WeatheredWayfarer = card("Weathered Wayfarer") {
        manaCost = "{W}"
        typeLine = "Creature — Human Nomad Cleric"
        power = 1
        toughness = 1

        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
            effect = Effects.SearchLibrary(
                filter = Filters.Land,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true
            )
            restrictions = listOf(
                ActivationRestriction.OnlyIfCondition(Conditions.OpponentControlsMoreLands)
            )
        }
    }

    val wayfarerAbilityId = WeatheredWayfarer.activatedAbilities.first().id

    val TestNonbasicLand = CardDefinition(
        name = "Test Nonbasic Land",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(cardTypes = setOf(CardType.LAND)),
        oracleText = "{T}: Add {C}."
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WeatheredWayfarer)
        driver.registerCard(TestNonbasicLand)
        return driver
    }

    test("can activate when opponent controls more lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Weathered Wayfarer on battlefield and remove summoning sickness
        val wayfarer = driver.putCreatureOnBattlefield(activePlayer, "Weathered Wayfarer")
        driver.removeSummoningSickness(wayfarer)

        // Opponent has 2 lands, active player has 1 land (opponent controls more)
        driver.putLandOnBattlefield(activePlayer, "Plains")
        driver.putLandOnBattlefield(opponent, "Plains")
        driver.putLandOnBattlefield(opponent, "Forest")

        // Put a Forest in the library so it can be found
        driver.putCardOnTopOfLibrary(activePlayer, "Forest")

        // Give mana to activate
        driver.giveMana(activePlayer, Color.WHITE, 1)

        // Activate the ability
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wayfarer,
                abilityId = wayfarerAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Wayfarer should be tapped
        driver.isTapped(wayfarer) shouldBe true

        // Both pass to resolve the ability
        driver.bothPass()

        // Should have a search library decision with exactly the 1 land in the library
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SearchLibraryDecision>()
        decision.options.size shouldBe 1

        // Select the Forest from the library
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decision.id, decision.options)
        )

        // The Forest should be in hand now
        driver.findCardInHand(activePlayer, "Forest") shouldNotBe null
    }

    test("cannot activate when opponent does not control more lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Weathered Wayfarer on battlefield and remove summoning sickness
        val wayfarer = driver.putCreatureOnBattlefield(activePlayer, "Weathered Wayfarer")
        driver.removeSummoningSickness(wayfarer)

        // Both players have 1 land (equal, not "more")
        driver.putLandOnBattlefield(activePlayer, "Plains")
        driver.putLandOnBattlefield(opponent, "Plains")

        // Give mana to activate
        driver.giveMana(activePlayer, Color.WHITE, 1)

        // Attempt to activate - should fail because opponent doesn't control MORE lands
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wayfarer,
                abilityId = wayfarerAbilityId
            )
        )
        result.isSuccess shouldBe false

        // Wayfarer should still be untapped
        driver.isTapped(wayfarer) shouldBe false
    }

    test("cannot activate when you control more lands than opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Weathered Wayfarer on battlefield and remove summoning sickness
        val wayfarer = driver.putCreatureOnBattlefield(activePlayer, "Weathered Wayfarer")
        driver.removeSummoningSickness(wayfarer)

        // Active player has 2 lands, opponent has 1
        driver.putLandOnBattlefield(activePlayer, "Plains")
        driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(opponent, "Plains")

        // Give mana to activate
        driver.giveMana(activePlayer, Color.WHITE, 1)

        // Should fail since active player controls more lands
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wayfarer,
                abilityId = wayfarerAbilityId
            )
        )
        result.isSuccess shouldBe false
    }

    test("can find any land card, not just basic lands") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wayfarer = driver.putCreatureOnBattlefield(activePlayer, "Weathered Wayfarer")
        driver.removeSummoningSickness(wayfarer)

        // Opponent has more lands
        driver.putLandOnBattlefield(activePlayer, "Plains")
        driver.putLandOnBattlefield(opponent, "Plains")
        driver.putLandOnBattlefield(opponent, "Forest")

        // Put a nonbasic land in the library
        driver.putCardOnTopOfLibrary(activePlayer, "Test Nonbasic Land")

        driver.giveMana(activePlayer, Color.WHITE, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wayfarer,
                abilityId = wayfarerAbilityId
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SearchLibraryDecision>()
        decision.options.size shouldBe 1

        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decision.id, decision.options)
        )

        // Nonbasic land should be in hand
        driver.findCardInHand(activePlayer, "Test Nonbasic Land") shouldNotBe null
    }

    test("can fail to find even when matching cards exist") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wayfarer = driver.putCreatureOnBattlefield(activePlayer, "Weathered Wayfarer")
        driver.removeSummoningSickness(wayfarer)

        driver.putLandOnBattlefield(activePlayer, "Plains")
        driver.putLandOnBattlefield(opponent, "Plains")
        driver.putLandOnBattlefield(opponent, "Forest")

        driver.putCardOnTopOfLibrary(activePlayer, "Forest")

        driver.giveMana(activePlayer, Color.WHITE, 1)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wayfarer,
                abilityId = wayfarerAbilityId
            )
        )

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SearchLibraryDecision>()

        // Select nothing (fail to find)
        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decision.id, emptyList())
        )

        // Forest should NOT be in hand
        driver.findCardInHand(activePlayer, "Forest") shouldBe null
    }
})
