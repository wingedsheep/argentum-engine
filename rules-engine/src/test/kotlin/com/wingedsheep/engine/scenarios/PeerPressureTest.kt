package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeGainControlEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Peer Pressure.
 *
 * Peer Pressure: {3}{U}
 * Sorcery
 * Choose a creature type. If you control more creatures of that type than each
 * other player, you gain control of all creatures of that type.
 * (This effect lasts indefinitely.)
 */
class PeerPressureTest : FunSpec({

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

    val PeerPressure = CardDefinition.sorcery(
        name = "Peer Pressure",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Choose a creature type. If you control more creatures of that type than each other player, you gain control of all creatures of that type.",
        script = CardScript.spell(
            effect = ChooseCreatureTypeGainControlEffect()
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PeerPressure, TestElf, TestGoblin))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )
        return driver
    }

    fun castPeerPressureAndChooseType(driver: GameTestDriver, caster: EntityId, chosenType: String) {
        val cardId = driver.putCardInHand(caster, "Peer Pressure")
        driver.giveMana(caster, Color.BLUE, 1)
        driver.giveMana(caster, Color.BLUE, 3) // generic mana via blue
        val castResult = driver.castSpell(caster, cardId)
        castResult.isSuccess shouldBe true

        // Resolve the spell (opponent passes priority)
        driver.bothPass()

        // Should present a creature type choice decision
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val chooseDecision = decision as ChooseOptionDecision
        val typeIndex = chooseDecision.options.indexOf(chosenType)
        driver.submitDecision(caster, OptionChosenResponse(chooseDecision.id, typeIndex))
    }

    test("gain control of opponent's creatures when you control more of the chosen type") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Caster has 2 Elves, opponent has 1 Elf
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        val opponentElf = driver.putCreatureOnBattlefield(opponent, "Test Elf")

        castPeerPressureAndChooseType(driver, caster, "Elf")

        // Caster should now control the opponent's Elf
        val projected = projector.project(driver.state)
        projected.getController(opponentElf) shouldBe caster
    }

    test("no control change when opponent has more creatures of the chosen type") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Caster has 1 Elf, opponent has 2 Elves
        val casterElf = driver.putCreatureOnBattlefield(caster, "Test Elf")
        driver.putCreatureOnBattlefield(opponent, "Test Elf")
        driver.putCreatureOnBattlefield(opponent, "Test Elf")

        castPeerPressureAndChooseType(driver, caster, "Elf")

        // Caster should still control their own Elf (no change)
        val projected = projector.project(driver.state)
        projected.getController(casterElf) shouldBe caster
    }

    test("no control change when counts are tied") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Both have 2 Elves (tied)
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        val opponentElf = driver.putCreatureOnBattlefield(opponent, "Test Elf")
        driver.putCreatureOnBattlefield(opponent, "Test Elf")

        castPeerPressureAndChooseType(driver, caster, "Elf")

        // No control change (tied)
        val projected = projector.project(driver.state)
        projected.getController(opponentElf) shouldBe opponent
    }

    test("only creatures of the chosen type are affected") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Caster has 2 Elves, opponent has 1 Elf and 1 Goblin
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        val opponentElf = driver.putCreatureOnBattlefield(opponent, "Test Elf")
        val opponentGoblin = driver.putCreatureOnBattlefield(opponent, "Test Goblin")

        castPeerPressureAndChooseType(driver, caster, "Elf")

        val projected = projector.project(driver.state)
        // Opponent's Elf should change control
        projected.getController(opponentElf) shouldBe caster
        // Opponent's Goblin should NOT change control
        projected.getController(opponentGoblin) shouldBe opponent
    }

    test("no control change when no creatures of the chosen type exist") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Only Goblins on the battlefield, choose Elf
        val opponentGoblin = driver.putCreatureOnBattlefield(opponent, "Test Goblin")

        castPeerPressureAndChooseType(driver, caster, "Elf")

        // No changes
        val projected = projector.project(driver.state)
        projected.getController(opponentGoblin) shouldBe opponent
    }

    test("control change is permanent") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Caster has 2 Elves, opponent has 1 Elf
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        driver.putCreatureOnBattlefield(caster, "Test Elf")
        val opponentElf = driver.putCreatureOnBattlefield(opponent, "Test Elf")

        castPeerPressureAndChooseType(driver, caster, "Elf")

        // Advance to next turn
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)

        // Control should still be changed (permanent effect)
        val projected = projector.project(driver.state)
        projected.getController(opponentElf) shouldBe caster
    }
})
