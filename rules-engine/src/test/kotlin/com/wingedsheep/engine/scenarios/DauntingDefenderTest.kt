package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Daunting Defender (ONS #21).
 *
 * Daunting Defender: {4}{W}
 * Creature — Human Cleric
 * 3/3
 * If a source would deal damage to a Cleric creature you control, prevent 1 of that damage.
 */
class DauntingDefenderTest : FunSpec({

    val DauntingDefender = CardDefinition.creature(
        name = "Daunting Defender",
        manaCost = ManaCost.parse("{4}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 3,
        toughness = 3,
        oracleText = "If a source would deal damage to a Cleric creature you control, prevent 1 of that damage.",
        script = CardScript.permanent(
            replacementEffects = listOf(
                PreventDamage(
                    amount = 1,
                    appliesTo = GameEvent.DamageEvent(
                        recipient = RecipientFilter.Matching(
                            GameObjectFilter.Creature.withSubtype(Subtype("Cleric")).youControl()
                        )
                    )
                )
            )
        )
    )

    val TestCleric = CardDefinition.creature(
        name = "Test Cleric",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 2,
        toughness = 4,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DauntingDefender, TestCleric))
        return driver
    }

    test("basic prevention - Lightning Bolt deals 2 damage to Cleric instead of 3") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Daunting Defender and a target Cleric on the battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Daunting Defender")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")

        // Deal 3 damage to the Cleric via Lightning Bolt
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(cleric)))
        driver.bothPass()

        // 3 damage - 1 prevented = 2 damage
        val damage = driver.state.getEntity(cleric)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 2
    }

    test("non-Cleric creature is not protected") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Daunting Defender and a non-Cleric creature (5/5 survives Lightning Bolt)
        driver.putCreatureOnBattlefield(activePlayer, "Daunting Defender")
        val nonCleric = driver.putCreatureOnBattlefield(activePlayer, "Force of Nature")

        // Deal 3 damage to the non-Cleric
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(nonCleric)))
        driver.bothPass()

        // Full 3 damage, no prevention (not a Cleric)
        val damage = driver.state.getEntity(nonCleric)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 3
    }

    test("two Daunting Defenders stack - prevent 2 damage total") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two Daunting Defenders and a target Cleric
        driver.putCreatureOnBattlefield(activePlayer, "Daunting Defender")
        driver.putCreatureOnBattlefield(activePlayer, "Daunting Defender")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")

        // Deal 3 damage to the Cleric
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(cleric)))
        driver.bothPass()

        // 3 damage - 2 prevented (1 per Defender) = 1 damage
        val damage = driver.state.getEntity(cleric)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 1
    }

    test("protects itself - Daunting Defender is a Cleric") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Daunting Defender on the battlefield
        val defender = driver.putCreatureOnBattlefield(activePlayer, "Daunting Defender")

        // Deal 3 damage to Daunting Defender itself
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(defender)))
        driver.bothPass()

        // 3 damage - 1 prevented = 2 damage (it's a Cleric, so it protects itself)
        val damage = driver.state.getEntity(defender)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 2
    }

    test("opponent's Clerics are not protected") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Our Daunting Defender
        driver.putCreatureOnBattlefield(activePlayer, "Daunting Defender")
        // Opponent's Cleric
        val opponentCleric = driver.putCreatureOnBattlefield(opponent, "Test Cleric")

        // Deal 3 damage to opponent's Cleric
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(opponentCleric)))
        driver.bothPass()

        // Full 3 damage - our Daunting Defender doesn't protect opponent's Clerics
        val damage = driver.state.getEntity(opponentCleric)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 3
    }

    test("prevents 1 combat damage to attacking Cleric") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Our Daunting Defender + a Cleric attacker
        driver.putCreatureOnBattlefield(activePlayer, "Daunting Defender")
        val cleric = driver.putCreatureOnBattlefield(activePlayer, "Test Cleric")
        driver.removeSummoningSickness(cleric)

        // Opponent has a 3/3 blocker
        val blocker = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Move to combat
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with our Cleric (2/4)
        driver.declareAttackers(activePlayer, listOf(cleric), opponent)
        driver.bothPass()

        // Opponent blocks with Centaur Courser (3/3)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(cleric)))
        driver.bothPass()

        // First strike damage step (no first strikers)
        driver.bothPass()

        // Combat damage: 3/3 deals 3 to our 2/4 Cleric, but 1 is prevented = 2 damage
        driver.bothPass()

        val clericDamage = driver.state.getEntity(cleric)?.get<DamageComponent>()?.amount ?: 0
        clericDamage shouldBe 2
    }
})
