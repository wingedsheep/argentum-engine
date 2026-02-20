package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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

    // =========================================================================
    // ManaSolver integration tests
    // =========================================================================

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all + listOf(ElvishGuidance, TestElf, TestGoblin, TestForest))
        return registry
    }

    test("ManaSolver canPay accounts for bonus mana from Elvish Guidance") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 1 Forest + Elvish Guidance + 2 Elves = 1G base + 2G bonus = 3G total
        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")

        val guidance = driver.putCardInHand(activePlayer, "Elvish Guidance")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, guidance, listOf(forest))
        driver.bothPass()

        val solver = ManaSolver(createRegistry())

        // {2}{G} costs 3 mana total (1G + 2 generic) - payable: tap Forest for 1G (base) + 2G (bonus)
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{2}{G}")) shouldBe true

        // {G}{G}{G} costs 3G - should be payable with 3G from one Forest
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{G}{G}{G}")) shouldBe true

        // {3}{G} costs 4 mana total - NOT payable with only 3G (1 base + 2 bonus)
        solver.canPay(driver.state, activePlayer, ManaCost.parse("{3}{G}")) shouldBe false
    }

    test("ManaSolver solve taps fewer lands with bonus mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // 2 Forests, one with Elvish Guidance, 2 Elves
        val forest1 = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")

        val guidance = driver.putCardInHand(activePlayer, "Elvish Guidance")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, guidance, listOf(forest1))
        driver.bothPass()

        val solver = ManaSolver(createRegistry())

        // {2}{G} costs 3 mana - enchanted Forest produces 3G, so only 1 source needed
        val solution = solver.solve(driver.state, activePlayer, ManaCost.parse("{2}{G}"))
        solution.shouldNotBeNull()
        solution.sources.size shouldBe 1
    }

    test("ManaSolver getAvailableManaCount includes bonus mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")
        driver.putCreatureOnBattlefield(activePlayer, "Test Elf")

        val guidance = driver.putCardInHand(activePlayer, "Elvish Guidance")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, guidance, listOf(forest))
        driver.bothPass()

        val solver = ManaSolver(createRegistry())

        // 1 Forest with 2 bonus = 3 total mana available
        solver.getAvailableManaCount(driver.state, activePlayer) shouldBe 3
    }
})
