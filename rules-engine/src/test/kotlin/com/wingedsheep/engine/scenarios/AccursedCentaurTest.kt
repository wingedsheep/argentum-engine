package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.SacrificeEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for SacrificeEffect via Accursed Centaur.
 *
 * Accursed Centaur: {B} 2/2 Creature — Zombie Centaur
 * "When Accursed Centaur enters the battlefield, sacrifice a creature."
 *
 * The sacrifice is mandatory - the player must sacrifice a creature they control.
 * Since the Centaur is on the battlefield when the trigger resolves, it is a
 * valid sacrifice target (the player can sacrifice the Centaur itself).
 */
class AccursedCentaurTest : FunSpec({

    val AccursedCentaur = CardDefinition(
        name = "Accursed Centaur",
        manaCost = ManaCost.parse("{B}"),
        typeLine = TypeLine.creature(setOf(Subtype("Zombie"), Subtype("Centaur"))),
        oracleText = "When Accursed Centaur enters the battlefield, sacrifice a creature.",
        creatureStats = CreatureStats(2, 2),
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = OnEnterBattlefield(),
                effect = SacrificeEffect(GameObjectFilter.Creature)
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AccursedCentaur)
        return driver
    }

    test("Accursed Centaur ETB requires sacrificing a creature when only the Centaur is on battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give the player Accursed Centaur and mana to cast it
        val centaur = driver.putCardInHand(activePlayer, "Accursed Centaur")
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Cast Accursed Centaur
        val castResult = driver.castSpell(activePlayer, centaur)
        castResult.isSuccess shouldBe true

        // Resolve the creature spell
        driver.bothPass()

        // Centaur should be on the battlefield
        val centaurOnBattlefield = driver.findPermanent(activePlayer, "Accursed Centaur")
        centaurOnBattlefield shouldNotBe null

        // ETB trigger should be on the stack
        driver.stackSize shouldBe 1

        // Resolve the ETB trigger
        driver.bothPass()

        // Only one creature on battlefield (the Centaur itself), so it's auto-sacrificed
        driver.pendingDecision shouldBe null
        driver.findPermanent(activePlayer, "Accursed Centaur") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldBe listOf("Accursed Centaur")
    }

    test("Accursed Centaur ETB prompts sacrifice choice when multiple creatures exist") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put another creature on the battlefield first
        val otherCreature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Give the player Accursed Centaur and mana to cast it
        val centaur = driver.putCardInHand(activePlayer, "Accursed Centaur")
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Cast and resolve Accursed Centaur
        driver.castSpell(activePlayer, centaur)
        driver.bothPass()

        // ETB trigger on stack
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Should have a pending decision to choose which creature to sacrifice
        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        (decision as SelectCardsDecision).minSelections shouldBe 1
        decision.maxSelections shouldBe 1

        // Choose to sacrifice the other creature (keep the Centaur)
        driver.submitCardSelection(activePlayer, listOf(otherCreature))

        // Centaur should remain, other creature should be in graveyard
        driver.findPermanent(activePlayer, "Accursed Centaur") shouldNotBe null
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(activePlayer) shouldBe listOf("Grizzly Bears")
    }

    test("Accursed Centaur ETB allows sacrificing the Centaur itself when other creatures exist") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put another creature on the battlefield first
        driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Give the player Accursed Centaur and mana to cast it
        val centaur = driver.putCardInHand(activePlayer, "Accursed Centaur")
        driver.giveMana(activePlayer, Color.BLACK, 1)

        // Cast and resolve
        driver.castSpell(activePlayer, centaur)
        driver.bothPass()

        // Resolve ETB trigger
        driver.stackSize shouldBe 1
        driver.bothPass()

        // Find the Centaur on the battlefield to sacrifice it
        val centaurOnBattlefield = driver.findPermanent(activePlayer, "Accursed Centaur")
        centaurOnBattlefield shouldNotBe null

        // Choose to sacrifice the Centaur itself
        driver.submitCardSelection(activePlayer, listOf(centaurOnBattlefield!!))

        // Centaur should be gone, other creature should remain
        driver.findPermanent(activePlayer, "Accursed Centaur") shouldBe null
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null
        driver.getGraveyardCardNames(activePlayer) shouldBe listOf("Accursed Centaur")
    }
})
