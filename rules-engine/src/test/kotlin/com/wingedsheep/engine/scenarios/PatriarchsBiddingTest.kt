package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.PatriarchsBidding
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Patriarch's Bidding.
 *
 * Patriarch's Bidding ({3}{B}{B})
 * Sorcery
 * Each player chooses a creature type. Each player returns all creature cards of a type
 * chosen this way from their graveyard to the battlefield.
 */
class PatriarchsBiddingTest : FunSpec({

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

    val GoblinShaman = CardDefinition.creature(
        name = "Goblin Shaman",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Shaman")),
        power = 2,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(GoblinWarrior, ElfDruid, HumanKnight, GoblinShaman)
        )
        return driver
    }

    test("returns creature cards of chosen types from all graveyards to the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures in graveyards
        driver.putCardInGraveyard(activePlayer, "Goblin Warrior")
        driver.putCardInGraveyard(activePlayer, "Human Knight")
        driver.putCardInGraveyard(opponent, "Elf Druid")
        driver.putCardInGraveyard(opponent, "Goblin Shaman")

        // Cast Patriarch's Bidding
        val spell = driver.putCardInHand(activePlayer, "Patriarch's Bidding")
        driver.giveMana(activePlayer, Color.BLACK, 5)
        driver.castSpell(activePlayer, spell)
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

        // Goblin Warrior should be on active player's battlefield (is a Goblin)
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldNotBe null

        // Human Knight should NOT be on battlefield (is neither Goblin nor Elf)
        driver.findPermanent(activePlayer, "Human Knight") shouldBe null

        // Elf Druid should be on opponent's battlefield (is an Elf)
        driver.findPermanent(opponent, "Elf Druid") shouldNotBe null

        // Goblin Shaman should be on opponent's battlefield (is a Goblin - chosen by active player)
        driver.findPermanent(opponent, "Goblin Shaman") shouldNotBe null
    }

    test("creatures with multiple types match if any type was chosen") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Goblin Warrior has subtypes: Goblin, Warrior
        driver.putCardInGraveyard(activePlayer, "Goblin Warrior")

        val spell = driver.putCardInHand(activePlayer, "Patriarch's Bidding")
        driver.giveMana(activePlayer, Color.BLACK, 5)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Active player chooses Warrior (matches Goblin Warrior)
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        val warriorIndex = decision1.options.indexOf("Warrior")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, warriorIndex))

        // Opponent chooses Human (no match)
        val decision2 = driver.pendingDecision as ChooseOptionDecision
        val humanIndex = decision2.options.indexOf("Human")
        driver.submitDecision(opponent, OptionChosenResponse(decision2.id, humanIndex))

        // Goblin Warrior should be on battlefield (is a Warrior, chosen by active player)
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldNotBe null
    }

    test("does nothing when no creatures in graveyards match chosen types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Goblin in graveyard
        driver.putCardInGraveyard(activePlayer, "Goblin Warrior")

        val spell = driver.putCardInHand(activePlayer, "Patriarch's Bidding")
        driver.giveMana(activePlayer, Color.BLACK, 5)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Both players choose types that don't match anything in graveyards
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        val humanIndex = decision1.options.indexOf("Human")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, humanIndex))

        val decision2 = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision2.options.indexOf("Elf")
        driver.submitDecision(opponent, OptionChosenResponse(decision2.id, elfIndex))

        // Goblin Warrior should still be in graveyard (is neither Human nor Elf)
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldBe null
    }

    test("returns creatures from both players' graveyards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Both players have Goblins in graveyards
        driver.putCardInGraveyard(activePlayer, "Goblin Warrior")
        driver.putCardInGraveyard(opponent, "Goblin Shaman")

        val spell = driver.putCardInHand(activePlayer, "Patriarch's Bidding")
        driver.giveMana(activePlayer, Color.BLACK, 5)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Active player chooses Goblin
        val decision1 = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision1.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision1.id, goblinIndex))

        // Opponent also chooses Goblin (duplicate is fine)
        val decision2 = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex2 = decision2.options.indexOf("Goblin")
        driver.submitDecision(opponent, OptionChosenResponse(decision2.id, goblinIndex2))

        // Both Goblins should be on battlefield under their respective owners
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldNotBe null
        driver.findPermanent(opponent, "Goblin Shaman") shouldNotBe null
    }
})
