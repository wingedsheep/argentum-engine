package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.DawnElemental
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Dawn Elemental (SCG #7).
 *
 * Dawn Elemental: {W}{W}{W}{W}
 * Creature — Elemental
 * 3/3
 * Flying
 * Prevent all damage that would be dealt to Dawn Elemental.
 */
class DawnElementalTest : FunSpec({

    // A sturdy creature that survives 3 damage
    val SturdyCreature = CardDefinition.creature(
        name = "Sturdy Creature",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 3,
        toughness = 5,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DawnElemental, SturdyCreature))
        return driver
    }

    test("prevents combat damage to Dawn Elemental when blocking") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a 3/5 creature
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Sturdy Creature")
        driver.removeSummoningSickness(opponentCreature)

        // We have Dawn Elemental
        val dawnElemental = driver.putCreatureOnBattlefield(activePlayer, "Dawn Elemental")
        driver.removeSummoningSickness(dawnElemental)

        // Advance to opponent's turn
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        // Opponent's turn - advance to combat
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Opponent attacks with their ground creature
        driver.declareAttackers(opponent, listOf(opponentCreature), activePlayer)
        driver.bothPass()

        // We block with Dawn Elemental (flying can block ground creatures)
        driver.declareBlockers(activePlayer, mapOf(dawnElemental to listOf(opponentCreature)))
        driver.bothPass()

        // First strike step (no first strikers)
        driver.bothPass()

        // Combat damage
        driver.bothPass()

        // Dawn Elemental should take 0 damage (all prevented)
        val dawnDamage = driver.state.getEntity(dawnElemental)?.get<DamageComponent>()?.amount ?: 0
        dawnDamage shouldBe 0

        // Opponent's creature should take 3 damage from Dawn Elemental
        val opponentDamage = driver.state.getEntity(opponentCreature)?.get<DamageComponent>()?.amount ?: 0
        opponentDamage shouldBe 3
    }

    test("prevents non-combat (spell) damage to Dawn Elemental") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Dawn Elemental on battlefield
        val dawnElemental = driver.putCreatureOnBattlefield(activePlayer, "Dawn Elemental")

        // Deal 3 damage via Lightning Bolt
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(dawnElemental)))
        driver.bothPass()

        // All damage should be prevented
        val damage = driver.state.getEntity(dawnElemental)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 0
    }

    test("does not prevent damage to other creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Dawn Elemental and a sturdy creature on battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Dawn Elemental")
        val sturdy = driver.putCreatureOnBattlefield(activePlayer, "Sturdy Creature")

        // Deal 3 damage to the sturdy creature via Lightning Bolt (it survives at 3/5)
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(sturdy)))
        driver.bothPass()

        // Sturdy Creature should take full 3 damage - Dawn Elemental doesn't prevent it
        val damage = driver.state.getEntity(sturdy)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 3
    }
})
