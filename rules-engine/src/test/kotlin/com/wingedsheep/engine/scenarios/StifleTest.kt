package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.CarrionFeeder
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Stifle.
 *
 * Stifle: {U}
 * Instant
 * Counter target activated or triggered ability.
 * (Mana abilities can't be targeted.)
 */
class StifleTest : FunSpec({

    // Simple test creature with an ETB "gain 3 life" trigger
    val LifeGainCreature = card("Life Gain Creature") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GainLife(3)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LifeGainCreature))
        return driver
    }

    test("counter an activated ability") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 has Stifle in hand and Carrion Feeder + fodder creature on P2's side
        val feeder = driver.putCreatureOnBattlefield(player2, "Carrion Feeder")
        driver.removeSummoningSickness(feeder)
        val fodder = driver.putCreatureOnBattlefield(player2, "Grizzly Bears")

        val stifle = driver.putCardInHand(player1, "Stifle")
        driver.giveMana(player1, Color.BLUE, 1)

        // Pass priority to Player 2
        driver.passPriority(player1)

        // Player 2 activates Carrion Feeder, sacrificing Grizzly Bears
        val abilityId = CarrionFeeder.activatedAbilities[0].id
        val result = driver.submit(
            ActivateAbility(
                playerId = player2,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(fodder)
                )
            )
        )
        result.isSuccess shouldBe true

        val abilityOnStack = driver.getTopOfStack()!!

        // Player 2 passes priority, Player 1 responds with Stifle
        driver.passPriority(player2)
        driver.castSpellWithTargets(player1, stifle, listOf(ChosenTarget.Spell(abilityOnStack)))

        // Resolve Stifle (both pass)
        driver.bothPass()

        // The activated ability should have been countered
        // Carrion Feeder should NOT have any +1/+1 counters
        driver.stackSize shouldBe 0
        val feederCounters = driver.state.getEntity(feeder)?.get<CountersComponent>()
        feederCounters shouldBe null

        // The sacrificed creature is still gone (sacrifice was a cost, not part of the effect)
        driver.findPermanent(player2, "Grizzly Bears") shouldBe null
    }

    test("counter a triggered ability") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 casts Life Gain Creature (ETB: gain 3 life)
        val creature = driver.putCardInHand(player1, "Life Gain Creature")
        driver.giveMana(player1, Color.GREEN, 2)
        driver.castSpell(player1, creature)

        // Both pass — creature resolves, ETB trigger goes on stack
        driver.bothPass()

        // Triggered ability should be on the stack
        driver.stackSize shouldBe 1
        val triggeredAbilityOnStack = driver.getTopOfStack()!!

        // Give P1 Stifle and mana after creature resolves
        val stifle = driver.putCardInHand(player1, "Stifle")
        driver.giveMana(player1, Color.BLUE, 1)

        // Player 1 casts Stifle targeting the triggered ability
        val castResult = driver.castSpellWithTargets(player1, stifle, listOf(ChosenTarget.Spell(triggeredAbilityOnStack)))
        castResult.isSuccess shouldBe true

        // Resolve Stifle (both pass)
        driver.bothPass()

        // The triggered ability should have been countered
        driver.stackSize shouldBe 0

        // Player 1 should NOT have gained 3 life
        driver.getLifeTotal(player1) shouldBe 20
    }
})
