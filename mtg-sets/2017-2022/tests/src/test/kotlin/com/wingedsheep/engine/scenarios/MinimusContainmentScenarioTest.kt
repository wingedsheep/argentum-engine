package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.MinimusContainment
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Minimus Containment
 * {2}{W} Enchantment — Aura
 * Enchant nonland permanent
 * Enchanted permanent is a Treasure artifact with "{T}, Sacrifice this artifact: Add one mana of
 * any color," and it loses all other abilities. (If it was a creature, it's no longer a creature.)
 *
 * Unlike Sugar Coat / Imprisoned in the Moon, this keeps the permanent's colors (ruling
 * 2021-07-23) and replaces its subtypes with Treasure.
 */
class MinimusContainmentScenarioTest : FunSpec({

    val treasureAbilityId = MinimusContainment.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().first().ability.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MinimusContainment))
        return driver
    }

    test("enchanted creature becomes a Treasure artifact that keeps its colors and supertypes " +
        "but loses its creature type, creature subtypes and abilities") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Test Hasty Prospector: legendary 2/1 red Monkey Pirate with Haste + a mana ability.
        val prospector = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Minimus Containment")
        driver.giveMana(player, Color.WHITE, 3)
        driver.castSpell(player, aura, listOf(prospector))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.isCreature(prospector) shouldBe false
        projected.hasType(prospector, "ARTIFACT") shouldBe true
        projected.hasSubtype(prospector, "Treasure") shouldBe true
        projected.hasSubtype(prospector, "Monkey") shouldBe false
        projected.hasSubtype(prospector, "Pirate") shouldBe false
        // Keeps its colors (ruling) and its legendary supertype (CR 205.4b).
        projected.hasColor(prospector, Color.RED) shouldBe true
        projected.isLegendary(prospector) shouldBe true
        // Loses all other abilities.
        projected.hasLostAllAbilities(prospector) shouldBe true
        projected.hasKeyword(prospector, Keyword.HASTE) shouldBe false
    }

    test("the granted Treasure ability sacrifices the permanent for one mana of the chosen color") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val prospector = driver.putCreatureOnBattlefield(player, "Test Hasty Prospector")
        val aura = driver.putCardInHand(player, "Minimus Containment")
        driver.giveMana(player, Color.WHITE, 3)
        driver.castSpell(player, aura, listOf(prospector))
        driver.bothPass()

        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = prospector,
                abilityId = treasureAbilityId,
                manaColorChoice = Color.GREEN
            )
        )
        result.outcome shouldBe Outcome.Done

        driver.state.getEntity(player)?.get<ManaPoolComponent>()?.green shouldBe 1
        driver.getGraveyardCardNames(player) shouldContain "Test Hasty Prospector"
    }
})
