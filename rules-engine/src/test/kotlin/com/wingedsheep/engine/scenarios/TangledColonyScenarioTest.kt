package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.TangledColony
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tangled Colony (WOE #113): {1}{B} 3/2 Rat
 *
 * "This creature can't block.
 *  When this creature dies, create X 1/1 black Rat creature tokens with 'This token can't block,'
 *  where X is the amount of damage dealt to it this turn."
 *
 * Covers the new `DynamicAmount.LastKnownDamageDealtToSource` node: the per-turn damage tally is
 * captured onto the `ZoneChangeEvent` when the creature leaves the battlefield, so the dies trigger
 * can still read it after the entity is gone.
 *
 * Tests:
 * 1. Lethal damage in excess of toughness is not capped — a Lightning Bolt on a 3/2 makes 3 Rats.
 * 2. Non-lethal damage the creature survived earlier in the turn still counts toward X.
 * 3. Dying without having been dealt damage makes no tokens at all (X = 0).
 * 4. Tangled Colony itself can't block.
 */
class TangledColonyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TangledColony))
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.ratTokens(playerId: EntityId): List<EntityId> =
        getCreatures(playerId).filter { getCardName(it) == "Rat Token" }

    test("excess damage counts — 3 damage to a 3/2 makes three Rat tokens") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val colony = driver.putCreatureOnBattlefield(player, "Tangled Colony")

        driver.giveMana(player, Color.RED, 1)
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.castSpellWithTargets(player, bolt, listOf(ChosenTarget.Permanent(colony)))
        driver.bothPass() // bolt resolves -> 3 damage -> Colony dies, queuing the dies trigger
        driver.bothPass() // dies trigger resolves

        val tokens = driver.ratTokens(player)
        tokens.size shouldBe 3
        tokens.forEach { token ->
            driver.state.projectedState.getPower(token) shouldBe 1
            driver.state.projectedState.getToughness(token) shouldBe 1
            driver.state.projectedState.cantBlock(token) shouldBe true
        }
    }

    test("non-lethal damage dealt earlier in the turn accumulates into X") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val colony = driver.putCreatureOnBattlefield(player, "Tangled Colony")

        // Pump to 6/5 so the first Bolt is survivable and its damage stays marked.
        driver.giveMana(player, Color.GREEN, 1)
        val growth = driver.putCardInHand(player, "Giant Growth")
        driver.castSpellWithTargets(player, growth, listOf(ChosenTarget.Permanent(colony)))
        driver.bothPass()

        driver.giveMana(player, Color.RED, 1)
        val bolt1 = driver.putCardInHand(player, "Lightning Bolt")
        driver.castSpellWithTargets(player, bolt1, listOf(ChosenTarget.Permanent(colony)))
        driver.bothPass() // 3 damage marked; the 6/5 survives
        driver.getCreatures(player).contains(colony) shouldBe true

        driver.giveMana(player, Color.RED, 1)
        val bolt2 = driver.putCardInHand(player, "Lightning Bolt")
        driver.castSpellWithTargets(player, bolt2, listOf(ChosenTarget.Permanent(colony)))
        driver.bothPass() // 6 total >= toughness 5 -> dies
        driver.bothPass() // dies trigger resolves

        driver.ratTokens(player).size shouldBe 6
    }

    test("dying without being dealt damage creates no tokens") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val colony = driver.putCreatureOnBattlefield(player, "Tangled Colony")

        driver.giveMana(player, Color.BLACK, 2)
        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.castSpellWithTargets(player, doomBlade, listOf(ChosenTarget.Permanent(colony)))
        driver.bothPass() // Doom Blade resolves -> Colony is destroyed, no damage dealt
        driver.bothPass() // dies trigger resolves with X = 0

        driver.ratTokens(player).size shouldBe 0
    }

    test("Tangled Colony can't block") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val colony = driver.putCreatureOnBattlefield(player, "Tangled Colony")
        driver.state.projectedState.cantBlock(colony) shouldBe true
    }
})
