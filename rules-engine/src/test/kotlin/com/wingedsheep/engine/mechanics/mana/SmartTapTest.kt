package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for smart mana auto-tapping logic in ManaSolver.
 *
 * The smart tapping algorithm prioritizes (tap first to last):
 * 1. Basic lands
 * 2. Non-basic single-color lands without abilities
 * 3. Dual/tri-lands without abilities
 * 4. Utility lands with non-mana abilities
 * 5. Pain lands
 * 6. Mana creatures that can attack
 * 7. Five-color lands
 *
 * It also considers cards in hand to preserve colors needed for future spells.
 */
class SmartTapTest : FunSpec({

    // =========================================================================
    // Test Card Definitions
    // =========================================================================

    // Dual land (non-basic, produces two colors via subtypes)
    val BreedingPool = CardDefinition(
        name = "Breeding Pool",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype.FOREST, Subtype.ISLAND)
        ),
        oracleText = "({T}: Add {G} or {U}.)"
    )

    // Utility land with non-mana ability
    val KessigWolfRun = CardDefinition(
        name = "Kessig Wolf Run",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(cardTypes = setOf(CardType.LAND)),
        oracleText = "{T}: Add {C}.\n{X}{R}{G}, {T}: Target creature gets +X/+0 and gains trample until end of turn.",
        script = CardScript(
            activatedAbilities = listOf(
                // Mana ability
                ActivatedAbility(
                    id = AbilityId(UUID.randomUUID().toString()),
                    cost = AbilityCost.Tap,
                    effect = AddColorlessManaEffect(1),
                    isManaAbility = true
                ),
                // Non-mana ability (pump)
                ActivatedAbility(
                    id = AbilityId(UUID.randomUUID().toString()),
                    cost = AbilityCost.Tap, // Simplified cost
                    effect = ModifyStatsEffect(3, 0, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
                    isManaAbility = false
                )
            )
        )
    )

    // Five-color land (produces any color)
    val ManaConfluence = CardDefinition(
        name = "Mana Confluence",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(cardTypes = setOf(CardType.LAND)),
        oracleText = "{T}, Pay 1 life: Add one mana of any color.",
        script = CardScript.permanent(
            ActivatedAbility(
                id = AbilityId(UUID.randomUUID().toString()),
                cost = AbilityCost.Composite(listOf(AbilityCost.Tap, AbilityCost.PayLife(1))),
                effect = AddAnyColorManaEffect(1),
                isManaAbility = true
            )
        )
    )

    // Spell cards for testing hand-awareness
    val WrathOfGod = CardDefinition.sorcery(
        name = "Wrath of God",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        oracleText = "Destroy all creatures."
    )

    val allTestCards = listOf(
        TestCards.Forest,
        TestCards.Mountain,
        TestCards.Plains,
        TestCards.Island,
        TestCards.LlanowarElves,
        TestCards.RagavanNimblePilferer,
        TestCards.BirdsOfParadise,
        TestCards.GrizzlyBears,
        TestCards.LightningBolt,
        BreedingPool,
        KessigWolfRun,
        ManaConfluence,
        WrathOfGod
    )

    // =========================================================================
    // Helper Functions
    // =========================================================================

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allTestCards)
        return driver
    }

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(allTestCards)
        return registry
    }

    fun getSolvedSourceNames(solution: ManaSolution?): List<String> {
        return solution?.sources?.map { it.name } ?: emptyList()
    }

    // =========================================================================
    // Basic Priority Tests
    // =========================================================================

    test("basic land is tapped before dual land for single color cost") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put Forest and Breeding Pool on battlefield
        driver.putLandOnBattlefield(playerId, "Forest")
        driver.putLandOnBattlefield(playerId, "Breeding Pool")

        // Create solver with the driver's registry
        val solver = ManaSolver(createRegistry())

        // Solve for {G}
        val cost = ManaCost.parse("{G}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 1

        // Should tap Forest (basic) before Breeding Pool (non-basic dual)
        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames shouldContain "Forest"
        tappedSourceNames shouldNotContain "Breeding Pool"
    }

    test("utility land is preserved when basic land is available") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put Forest and Kessig Wolf Run on battlefield
        driver.putLandOnBattlefield(playerId, "Forest")
        driver.putLandOnBattlefield(playerId, "Kessig Wolf Run")

        val solver = ManaSolver(createRegistry())

        // Solve for {1} - both can produce this
        val cost = ManaCost.parse("{1}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 1

        // Should tap Forest (no utility) before Kessig Wolf Run (has pump ability)
        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames shouldContain "Forest"
        tappedSourceNames shouldNotContain "Kessig Wolf Run"
    }

    test("mana creature that can attack is preserved when land is available") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put Forest and Ragavan (hasty mana dork) on battlefield
        driver.putLandOnBattlefield(playerId, "Forest")
        val ragavanId = driver.putCreatureOnBattlefield(playerId, "Ragavan, Nimble Pilferer")

        // Ragavan has haste so can attack immediately - verify this is detected
        val solver = ManaSolver(createRegistry())

        // Solve for {1} - both can produce mana
        val cost = ManaCost.parse("{1}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 1

        // Should tap Forest (land) before Ragavan (creature that can attack)
        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames shouldContain "Forest"
        tappedSourceNames shouldNotContain "Ragavan, Nimble Pilferer"
    }

    test("five-color land is tapped last for generic costs") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put Forest and Mana Confluence on battlefield
        driver.putLandOnBattlefield(playerId, "Forest")
        driver.putLandOnBattlefield(playerId, "Mana Confluence")

        val solver = ManaSolver(createRegistry())

        // Solve for {1}
        val cost = ManaCost.parse("{1}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 1

        // Should tap Forest before Mana Confluence (five-color + pain)
        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames shouldContain "Forest"
        tappedSourceNames shouldNotContain "Mana Confluence"
    }

    test("multi-source ordering follows priority") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put multiple sources on battlefield
        driver.putLandOnBattlefield(playerId, "Forest")
        driver.putLandOnBattlefield(playerId, "Breeding Pool")
        driver.putLandOnBattlefield(playerId, "Kessig Wolf Run")

        val solver = ManaSolver(createRegistry())

        // Solve for {2}{G}
        val cost = ManaCost.parse("{2}{G}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 3

        // All three should be tapped
        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames shouldContain "Forest"
        tappedSourceNames shouldContain "Breeding Pool"
        tappedSourceNames shouldContain "Kessig Wolf Run"
    }

    test("mana dork with summoning sickness cannot be used for mana") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put Forest and Llanowar Elves on battlefield
        // Llanowar Elves has summoning sickness by default
        driver.putLandOnBattlefield(playerId, "Forest")
        driver.putCreatureOnBattlefield(playerId, "Llanowar Elves")

        val solver = ManaSolver(createRegistry())

        // Solve for {G}{G} - need 2 green sources
        val cost = ManaCost.parse("{G}{G}")
        val solution = solver.solve(driver.state, playerId, cost)

        // Should fail because Llanowar Elves has summoning sickness
        // and can't tap for mana
        solution shouldBe null
    }

    test("mana dork without summoning sickness can be used") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put Forest and Llanowar Elves on battlefield
        driver.putLandOnBattlefield(playerId, "Forest")
        val elvesId = driver.putCreatureOnBattlefield(playerId, "Llanowar Elves")

        // Remove summoning sickness
        driver.removeSummoningSickness(elvesId)

        val solver = ManaSolver(createRegistry())

        // Solve for {G}{G} - need 2 green sources
        val cost = ManaCost.parse("{G}{G}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 2

        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames shouldContain "Forest"
        tappedSourceNames shouldContain "Llanowar Elves"
    }

    // =========================================================================
    // Hand-Awareness Tests
    // =========================================================================

    test("preserves colors needed for cards in hand") {
        val driver = createDriver()
        // Create a deck with Wrath of God in it (costs {2}{W}{W})
        driver.initMirrorMatch(
            deck = Deck.of(
                "Plains" to 20,
                "Forest" to 10,
                "Wrath of God" to 10
            ),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put 2 Plains and 1 Forest on battlefield
        driver.putLandOnBattlefield(playerId, "Plains")
        driver.putLandOnBattlefield(playerId, "Plains")
        driver.putLandOnBattlefield(playerId, "Forest")

        // Ensure Wrath of God is in hand (needs {W}{W})
        driver.putCardInHand(playerId, "Wrath of God")
        // When casting a {1} spell, prefer tapping Forest to preserve white for Wrath

        val solver = ManaSolver(createRegistry())
        val cost = ManaCost.parse("{1}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 1

        // Should prefer Forest since Plains are needed for {W}{W} in hand
        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames shouldContain "Forest"
    }

    test("taps excess colors when more sources than needed") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 20,
                "Lightning Bolt" to 20  // Costs {R}
            ),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put 3 Mountains on battlefield
        driver.putLandOnBattlefield(playerId, "Mountain")
        driver.putLandOnBattlefield(playerId, "Mountain")
        driver.putLandOnBattlefield(playerId, "Mountain")

        // Hand has Lightning Bolt (needs only 1 red)
        // Casting a {2} spell can tap 2 mountains, keeping 1 for the hand card

        val solver = ManaSolver(createRegistry())
        val cost = ManaCost.parse("{2}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 2

        // Should tap 2 of the 3 Mountains
        val tappedSourceNames = getSolvedSourceNames(solution)
        tappedSourceNames.count { it == "Mountain" } shouldBe 2
    }

    test("falls back to needed color when necessary for current spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Grizzly Bears" to 20  // Costs {1}{G}
            ),
            skipMulligans = true
        )
        val playerId = driver.player1

        // Put only 1 Forest on battlefield
        driver.putLandOnBattlefield(playerId, "Forest")

        // Hand has Grizzly Bears (needs {G})
        // Casting a {G} spell must tap the only Forest, even though hand needs it

        val solver = ManaSolver(createRegistry())
        val cost = ManaCost.parse("{G}")
        val solution = solver.solve(driver.state, playerId, cost)

        solution.shouldNotBeNull()
        solution.sources shouldHaveSize 1
        solution.sources[0].name shouldBe "Forest"
    }
})
