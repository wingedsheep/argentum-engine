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
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Peer Pressure.
 *
 * Peer Pressure ({3}{U})
 * Sorcery
 * Choose a creature type. If you control more creatures of that type than each other player,
 * you gain control of all creatures of that type.
 */
class PeerPressureTest : FunSpec({

    val projector = StateProjector()

    val GoblinWarrior = CardDefinition.creature(
        name = "Goblin Warrior",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Warrior")),
        power = 1,
        toughness = 1
    )

    val GoblinScout = CardDefinition.creature(
        name = "Goblin Scout",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Scout")),
        power = 1,
        toughness = 1
    )

    val GoblinRogue = CardDefinition.creature(
        name = "Goblin Rogue",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Rogue")),
        power = 1,
        toughness = 1
    )

    val ElfDruid = CardDefinition.creature(
        name = "Elf Druid",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Druid")),
        power = 1,
        toughness = 1
    )

    val PeerPressure = CardDefinition.sorcery(
        name = "Peer Pressure",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Choose a creature type. If you control more creatures of that type than each other player, you gain control of all creatures of that type.",
        script = com.wingedsheep.sdk.model.CardScript.spell(
            effect = com.wingedsheep.sdk.scripting.PeerPressureEffect
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(PeerPressure, GoblinWarrior, GoblinScout, GoblinRogue, ElfDruid)
        )
        return driver
    }

    test("Peer Pressure gains control when controller has more of chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has 2 Goblins, opponent has 1
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Scout")
        val opponentGoblin = driver.putCreatureOnBattlefield(opponent, "Goblin Rogue")

        // Cast Peer Pressure
        val spell = driver.putCardInHand(activePlayer, "Peer Pressure")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Choose Goblin
        val decision = driver.pendingDecision as ChooseOptionDecision
        decision.playerId shouldBe activePlayer
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Active player should now control the opponent's Goblin Rogue
        val projected = projector.project(driver.state)
        projected.getController(opponentGoblin) shouldBe activePlayer
    }

    test("Peer Pressure does not gain control when tied") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Both players have 1 Goblin each (tied)
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val opponentGoblin = driver.putCreatureOnBattlefield(opponent, "Goblin Scout")

        val spell = driver.putCardInHand(activePlayer, "Peer Pressure")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Choose Goblin
        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // No control change - each player keeps their Goblin
        val projected = projector.project(driver.state)
        projected.getController(opponentGoblin) shouldBe opponent
    }

    test("Peer Pressure does not gain control when controller has fewer") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player has 1 Goblin, opponent has 2
        driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val opponentGoblin1 = driver.putCreatureOnBattlefield(opponent, "Goblin Scout")
        val opponentGoblin2 = driver.putCreatureOnBattlefield(opponent, "Goblin Rogue")

        val spell = driver.putCardInHand(activePlayer, "Peer Pressure")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Choose Goblin
        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // No control change - opponent has more Goblins
        val projected = projector.project(driver.state)
        projected.getController(opponentGoblin1) shouldBe opponent
        projected.getController(opponentGoblin2) shouldBe opponent
    }

    test("Peer Pressure does nothing when no creatures of chosen type exist") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only Elves on the battlefield
        val activeElf = driver.putCreatureOnBattlefield(activePlayer, "Elf Druid")
        val opponentElf = driver.putCreatureOnBattlefield(opponent, "Elf Druid")

        val spell = driver.putCardInHand(activePlayer, "Peer Pressure")
        driver.giveMana(activePlayer, Color.BLUE, 4)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Choose Goblin (no Goblins exist)
        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Nothing changes
        val projected = projector.project(driver.state)
        projected.getController(activeElf) shouldBe activePlayer
        projected.getController(opponentElf) shouldBe opponent
    }
})
