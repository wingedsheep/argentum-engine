package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Undead Gladiator:
 * {1}{B}{B}
 * Creature — Zombie Barbarian
 * 3/1
 * {1}{B}, Discard a card: Return Undead Gladiator from your graveyard to your hand.
 *   Activate only during your upkeep.
 * Cycling {1}{B}
 */
class UndeadGladiatorTest : FunSpec({

    val graveyardAbilityId = AbilityId("undead-gladiator-graveyard")

    val UndeadGladiator = CardDefinition.creature(
        name = "Undead Gladiator",
        manaCost = ManaCost.parse("{1}{B}{B}"),
        subtypes = setOf(Subtype("Zombie"), Subtype("Barbarian")),
        power = 3,
        toughness = 1,
        oracleText = "{1}{B}, Discard a card: Return Undead Gladiator from your graveyard to your hand. Activate only during your upkeep.\nCycling {1}{B}",
        script = CardScript(
            activatedAbilities = listOf(
                ActivatedAbility(
                    id = graveyardAbilityId,
                    cost = AbilityCost.Composite(listOf(
                        AbilityCost.Mana(ManaCost.parse("{1}{B}")),
                        AbilityCost.Discard()
                    )),
                    effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND),
                    activateFromZone = Zone.GRAVEYARD,
                    restrictions = listOf(
                        ActivationRestriction.All(
                            ActivationRestriction.OnlyDuringYourTurn,
                            ActivationRestriction.DuringStep(Step.UPKEEP)
                        )
                    )
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UndeadGladiator))
        return driver
    }

    /**
     * Advance to targetPlayer's next upkeep from the current game position.
     */
    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: EntityId) {
        // First advance past the current upkeep if we're in one
        if (driver.currentStep == Step.UPKEEP) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        }
        // Advance to the next upkeep
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        // If it's the wrong player's upkeep, advance again
        if (driver.activePlayer != targetPlayer) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("return Undead Gladiator from graveyard to hand during upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        // Put Undead Gladiator in graveyard
        val gladiator = driver.putCardInGraveyard(activePlayer, "Undead Gladiator")

        // Put a card in hand to discard as cost
        val cardToDiscard = driver.putCardInHand(activePlayer, "Swamp")

        // Advance to upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Give mana to pay {1}{B}
        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Activate the graveyard ability with discard cost
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gladiator,
                abilityId = graveyardAbilityId,
                costPayment = AdditionalCostPayment(
                    discardedCards = listOf(cardToDiscard)
                )
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Undead Gladiator should now be in hand
        driver.getHand(activePlayer).contains(gladiator) shouldBe true

        // The discarded card should be in graveyard
        driver.state.getGraveyard(activePlayer).contains(cardToDiscard) shouldBe true

        // Gladiator should no longer be in graveyard
        driver.state.getGraveyard(activePlayer).contains(gladiator) shouldBe false
    }

    test("cannot activate graveyard ability outside of upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gladiator = driver.putCardInGraveyard(activePlayer, "Undead Gladiator")
        val cardToDiscard = driver.putCardInHand(activePlayer, "Swamp")

        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Try to activate during main phase - should fail
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gladiator,
                abilityId = graveyardAbilityId,
                costPayment = AdditionalCostPayment(
                    discardedCards = listOf(cardToDiscard)
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot activate graveyard ability during opponent's upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != activePlayer }

        val gladiator = driver.putCardInGraveyard(activePlayer, "Undead Gladiator")
        val cardToDiscard = driver.putCardInHand(activePlayer, "Swamp")

        // Advance to opponent's upkeep
        advanceToPlayerUpkeep(driver, opponent)

        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Try to activate during opponent's upkeep - should fail
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gladiator,
                abilityId = graveyardAbilityId,
                costPayment = AdditionalCostPayment(
                    discardedCards = listOf(cardToDiscard)
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("cannot activate graveyard ability without specifying a card to discard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val gladiator = driver.putCardInGraveyard(activePlayer, "Undead Gladiator")

        advanceToPlayerUpkeep(driver, activePlayer)
        driver.giveMana(activePlayer, Color.BLACK, 2)

        // Try to activate without specifying which card to discard - should fail
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gladiator,
                abilityId = graveyardAbilityId,
                costPayment = AdditionalCostPayment(
                    discardedCards = emptyList()
                )
            )
        )
        result.isSuccess shouldBe false
    }
})
