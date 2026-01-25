package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.SacrificeUnlessSacrificePermanentEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for "Sacrifice [this] unless you sacrifice [N permanents]" effects.
 *
 * Cards using this pattern:
 * - Plant Elemental: "sacrifice it unless you sacrifice a Forest"
 * - Primeval Force: "sacrifice it unless you sacrifice three Forests"
 * - Thing from the Deep: "Whenever ~ attacks, sacrifice it unless you sacrifice an Island"
 */
class SacrificeUnlessSacrificeTest : FunSpec({

    // Test card: Plant Elemental (3/4 for 1G, sacrifice unless you sacrifice a Forest)
    val PlantElemental = CardDefinition(
        name = "Plant Elemental",
        manaCost = ManaCost.parse("{1}{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Plant"), Subtype("Elemental"))),
        oracleText = "When Plant Elemental enters the battlefield, sacrifice it unless you sacrifice a Forest.",
        creatureStats = CreatureStats(3, 4),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = SacrificeUnlessSacrificePermanentEffect(permanentType = "Forest")
            )
        )
    )

    // Test card: Big creature requiring multiple lands
    val PrimevalForce = CardDefinition(
        name = "Primeval Force",
        manaCost = ManaCost.parse("{2}{G}{G}{G}"),
        typeLine = TypeLine.creature(setOf(Subtype("Elemental"))),
        oracleText = "When Primeval Force enters the battlefield, sacrifice it unless you sacrifice three Forests.",
        creatureStats = CreatureStats(8, 8),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = SacrificeUnlessSacrificePermanentEffect(permanentType = "Forest", count = 3)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PlantElemental)
        driver.registerCard(PrimevalForce)
        return driver
    }

    test("Plant Elemental ETB prompts to sacrifice a Forest when player has one") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Forest on the battlefield
        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")

        // Give the player Plant Elemental in hand and mana to cast it
        val plantElemental = driver.putCardInHand(activePlayer, "Plant Elemental")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        // Cast Plant Elemental
        val castResult = driver.castSpell(activePlayer, plantElemental)
        castResult.isSuccess shouldBe true

        // Let the spell resolve
        driver.bothPass()

        // Now the ETB trigger should be on the stack, resolve it
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Should have a pending decision to select a Forest
        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        (decision as SelectCardsDecision).options shouldContain forest
    }

    test("Plant Elemental stays if player sacrifices a Forest") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put two Forests on the battlefield
        val forest1 = driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Forest")

        // Give the player Plant Elemental and mana
        val plantElemental = driver.putCardInHand(activePlayer, "Plant Elemental")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        // Cast and resolve Plant Elemental
        driver.castSpell(activePlayer, plantElemental)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Select one Forest to sacrifice
        driver.submitCardSelection(activePlayer, listOf(forest1))

        // Plant Elemental should be on the battlefield
        driver.findPermanent(activePlayer, "Plant Elemental") shouldNotBe null

        // Forest1 should be in graveyard
        driver.getGraveyardCardNames(activePlayer) shouldContain "Forest"
    }

    test("Plant Elemental is sacrificed if player declines to sacrifice a Forest") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Forest on the battlefield
        driver.putLandOnBattlefield(activePlayer, "Forest")

        // Give the player Plant Elemental and mana
        val plantElemental = driver.putCardInHand(activePlayer, "Plant Elemental")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        // Cast and resolve Plant Elemental
        driver.castSpell(activePlayer, plantElemental)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Select 0 Forests (decline to pay)
        driver.submitCardSelection(activePlayer, emptyList())

        // Plant Elemental should NOT be on the battlefield
        driver.findPermanent(activePlayer, "Plant Elemental") shouldBe null

        // Plant Elemental should be in graveyard
        driver.getGraveyardCardNames(activePlayer) shouldContain "Plant Elemental"
    }

    test("Plant Elemental is sacrificed when player has no Forests") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No Forests on the battlefield - only Plains
        driver.putLandOnBattlefield(activePlayer, "Plains")

        // Give the player Plant Elemental and mana (cheating - using Plains but giving green mana)
        val plantElemental = driver.putCardInHand(activePlayer, "Plant Elemental")
        driver.giveMana(activePlayer, Color.GREEN, 2)

        // Cast and resolve Plant Elemental
        driver.castSpell(activePlayer, plantElemental)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger - should auto-sacrifice since no Forests available
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // No decision needed - should have been auto-sacrificed
        driver.pendingDecision shouldBe null

        // Plant Elemental should NOT be on the battlefield
        driver.findPermanent(activePlayer, "Plant Elemental") shouldBe null

        // Plant Elemental should be in graveyard
        driver.getGraveyardCardNames(activePlayer) shouldContain "Plant Elemental"
    }

    test("Primeval Force requires exactly three Forests") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put 4 Forests on the battlefield
        val forest1 = driver.putLandOnBattlefield(activePlayer, "Forest")
        val forest2 = driver.putLandOnBattlefield(activePlayer, "Forest")
        val forest3 = driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Forest")

        // Give the player Primeval Force and mana
        val primevalForce = driver.putCardInHand(activePlayer, "Primeval Force")
        driver.giveMana(activePlayer, Color.GREEN, 5)

        // Cast and resolve Primeval Force
        driver.castSpell(activePlayer, primevalForce)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Decision should allow selecting up to 3 Forests
        val decision = driver.pendingDecision as? SelectCardsDecision
        decision shouldNotBe null
        decision!!.maxSelections shouldBe 3
        decision.minSelections shouldBe 0

        // Select three Forests to sacrifice
        driver.submitCardSelection(activePlayer, listOf(forest1, forest2, forest3))

        // Primeval Force should be on the battlefield
        driver.findPermanent(activePlayer, "Primeval Force") shouldNotBe null

        // The graveyard should have 3 Forests
        driver.getGraveyardCardNames(activePlayer).count { it == "Forest" } shouldBe 3
    }

    test("Primeval Force is sacrificed if player has fewer than three Forests") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only put 2 Forests on the battlefield
        driver.putLandOnBattlefield(activePlayer, "Forest")
        driver.putLandOnBattlefield(activePlayer, "Forest")

        // Give the player Primeval Force and mana
        val primevalForce = driver.putCardInHand(activePlayer, "Primeval Force")
        driver.giveMana(activePlayer, Color.GREEN, 5)

        // Cast and resolve Primeval Force
        driver.castSpell(activePlayer, primevalForce)
        driver.bothPass() // Resolve creature

        // Resolve ETB trigger - should auto-sacrifice
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // No decision needed - auto-sacrificed due to insufficient Forests
        driver.pendingDecision shouldBe null

        // Primeval Force should NOT be on the battlefield
        driver.findPermanent(activePlayer, "Primeval Force") shouldBe null

        // Primeval Force should be in graveyard
        driver.getGraveyardCardNames(activePlayer) shouldContain "Primeval Force"
    }
})
