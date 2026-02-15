package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Elvish Guidance.
 *
 * Elvish Guidance: {2}{G}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land is tapped for mana, its controller adds an additional {G}
 * for each Elf on the battlefield.
 */
class ElvishGuidanceTest : FunSpec({

    // Forest with explicit mana ability (needed for ActivateAbility in tests)
    val TestForest = basicLand("Forest") {}

    val ElvishGuidance = CardDefinition.aura(
        name = "Elvish Guidance",
        manaCost = ManaCost.parse("{2}{G}"),
        oracleText = "Enchant land\nWhenever enchanted land is tapped for mana, its controller adds an additional {G} for each Elf on the battlefield.",
        script = CardScript(
            auraTarget = TargetPermanent(filter = TargetFilter.Land),
            staticAbilities = listOf(
                AdditionalManaOnTap(
                    color = Color.GREEN,
                    amount = DynamicAmounts.creaturesWithSubtype(Subtype("Elf"))
                )
            )
        )
    )

    val TestElf = CardDefinition.creature(
        name = "Test Elf",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1
    )

    val TestGoblin = CardDefinition.creature(
        name = "Test Goblin",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    val manaAbilityId = TestForest.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ElvishGuidance, TestElf, TestGoblin, TestForest))
        return driver
    }

    test("Tapping enchanted land with 2 Elves on battlefield adds 2 additional green mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a forest on the battlefield
        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // Put 2 Elves on the battlefield
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")

        // Cast Elvish Guidance on the Forest
        val guidance = driver.putCardInHand(activePlayer, "Elvish Guidance")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, guidance, listOf(forest))
        driver.bothPass()

        // After casting (cost {2}{G}), mana pool should be empty.
        // Now tap the Forest - should produce 1G + 2G additional = 3G
        // Tap the Forest for mana

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = forest,
                abilityId = manaAbilityId
            )
        )

        result.isSuccess shouldBe true

        // Forest produces 1G base + 2G from Elvish Guidance (2 Elves) = 3G total
        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()!!
        pool.green shouldBe 3
    }

    test("No additional mana when no Elves on battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // Put a non-Elf creature
        driver.putCreatureOnBattlefield(activePlayer, "Test Goblin")

        // Cast Elvish Guidance on the Forest
        val guidance = driver.putCardInHand(activePlayer, "Elvish Guidance")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, guidance, listOf(forest))
        driver.bothPass()

        // Tap the forest
        // Tap the Forest for mana

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = forest,
                abilityId = manaAbilityId
            )
        )

        result.isSuccess shouldBe true

        // Forest produces only 1G (no Elves = no additional mana)
        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()!!
        pool.green shouldBe 1
    }

    test("Counts Elves on both sides of the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponentPlayer = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")

        // 1 Elf controlled by active player
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")
        // 1 Elf controlled by opponent
        driver.putCreatureOnBattlefield(opponentPlayer, "Test Elf")

        // Cast Elvish Guidance on the Forest
        val guidance = driver.putCardInHand(activePlayer, "Elvish Guidance")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, guidance, listOf(forest))
        driver.bothPass()

        // Tap the forest
        // Tap the Forest for mana

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = forest,
                abilityId = manaAbilityId
            )
        )

        result.isSuccess shouldBe true

        // Forest produces 1G base + 2G from Elvish Guidance (2 Elves total) = 3G
        val pool = driver.state.getEntity(activePlayer)?.get<ManaPoolComponent>()!!
        pool.green shouldBe 3
    }
})
