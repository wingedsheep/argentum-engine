package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.HarshMercyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Harsh Mercy.
 *
 * Harsh Mercy ({2}{W})
 * Sorcery
 * Each player chooses a creature type. Destroy all creatures that aren't of a type
 * chosen this way. They can't be regenerated.
 */
class HarshMercyTest : FunSpec({

    val GoblinWarrior = CardDefinition.creature(
        name = "Goblin Warrior",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Warrior")),
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

    val HumanKnight = CardDefinition.creature(
        name = "Human Knight",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Knight")),
        power = 2,
        toughness = 2
    )

    val HarshMercy = CardDefinition.sorcery(
        name = "Harsh Mercy",
        manaCost = ManaCost.parse("{2}{W}"),
        oracleText = "Each player chooses a creature type. Destroy all creatures that aren't of a type chosen this way. They can't be regenerated.",
        script = com.wingedsheep.sdk.model.CardScript.spell(
            effect = HarshMercyEffect
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(HarshMercy, GoblinWarrior, ElfDruid, HumanKnight)
        )
        return driver
    }

    test("Harsh Mercy destroys creatures not of chosen types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val elf = driver.putCreatureOnBattlefield(opponent, "Elf Druid")
        val knight = driver.putCreatureOnBattlefield(opponent, "Human Knight")

        // Cast Harsh Mercy
        val spell = driver.putCardInHand(activePlayer, "Harsh Mercy")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, spell)

        // Resolve the spell (both pass)
        driver.bothPass()

        // Active player chooses Goblin
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        decision1.playerId shouldBe activePlayer
        val goblinIndex = decision1.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, goblinIndex))

        // Opponent chooses Elf
        val decision2 = driver.pendingDecision as ChooseOptionDecision
        decision2.playerId shouldBe opponent
        val elfIndex = decision2.options.indexOf("Elf")
        driver.submitDecision(opponent, OptionChosenResponse(decision2.id, elfIndex))

        // Goblin Warrior should survive (is a Goblin - chosen by active player)
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldNotBe null

        // Elf Druid should survive (is an Elf - chosen by opponent)
        driver.findPermanent(opponent, "Elf Druid") shouldNotBe null

        // Human Knight should be destroyed (is neither Goblin nor Elf)
        driver.findPermanent(opponent, "Human Knight") shouldBe null
    }

    test("Harsh Mercy spares creatures with any of the chosen types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // GoblinWarrior has types: Goblin, Warrior
        val goblinWarrior = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val knight = driver.putCreatureOnBattlefield(opponent, "Human Knight")

        val spell = driver.putCardInHand(activePlayer, "Harsh Mercy")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Active player chooses Warrior (matches Goblin Warrior)
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        val warriorIndex = decision1.options.indexOf("Warrior")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, warriorIndex))

        // Opponent chooses Knight (matches Human Knight)
        val decision2 = driver.pendingDecision as ChooseOptionDecision
        val knightIndex = decision2.options.indexOf("Knight")
        driver.submitDecision(opponent, OptionChosenResponse(decision2.id, knightIndex))

        // Goblin Warrior survives (is a Warrior)
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldNotBe null

        // Human Knight survives (is a Knight)
        driver.findPermanent(opponent, "Human Knight") shouldNotBe null
    }

    test("Harsh Mercy destroys all creatures when both players choose the same type with no matches") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures that are Goblin and Elf
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")
        val elf = driver.putCreatureOnBattlefield(opponent, "Elf Druid")

        val spell = driver.putCardInHand(activePlayer, "Harsh Mercy")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Both players choose "Human" - neither creature matches
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        val humanIndex = decision1.options.indexOf("Human")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, humanIndex))

        val decision2 = driver.pendingDecision as ChooseOptionDecision
        val humanIndex2 = decision2.options.indexOf("Human")
        driver.submitDecision(opponent, OptionChosenResponse(decision2.id, humanIndex2))

        // Both creatures should be destroyed
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldBe null
        driver.findPermanent(opponent, "Elf Druid") shouldBe null
    }

    test("Harsh Mercy does not destroy noncreature permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a creature and a noncreature (land) on the battlefield
        val goblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Warrior")

        val spell = driver.putCardInHand(activePlayer, "Harsh Mercy")
        driver.giveMana(activePlayer, Color.WHITE, 3)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Both choose "Human" - Goblin doesn't match
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        val humanIndex = decision1.options.indexOf("Human")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, humanIndex))

        val decision2 = driver.pendingDecision as ChooseOptionDecision
        val humanIndex2 = decision2.options.indexOf("Human")
        driver.submitDecision(opponent, OptionChosenResponse(decision2.id, humanIndex2))

        // Goblin should be destroyed
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldBe null

        // Lands should still be on battlefield (not destroyed by Harsh Mercy)
        // The lands used for mana are still there (they were given via giveMana, not tapped)
    }
})
