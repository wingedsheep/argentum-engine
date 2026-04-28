package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Bloodline Bidding.
 *
 * Bloodline Bidding ({6}{B}{B})
 * Sorcery — Convoke
 * Choose a creature type. Return all creature cards of the chosen type from your
 * graveyard to the battlefield.
 */
class BloodlineBiddingTest : FunSpec({

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

    // Changeling has all creature types (Rule 702.73), even in the graveyard
    val MistformShifter = CardDefinition.creature(
        name = "Mistform Shifter",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Shapeshifter")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.CHANGELING)
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoblinWarrior, ElfDruid, MistformShifter))
        return driver
    }

    test("returns changeling creatures from graveyard regardless of chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A Goblin and a Changeling sit in the active player's graveyard;
        // an Elf in the same graveyard should NOT come back when "Goblin" is chosen.
        driver.putCardInGraveyard(activePlayer, "Goblin Warrior")
        driver.putCardInGraveyard(activePlayer, "Mistform Shifter")
        driver.putCardInGraveyard(activePlayer, "Elf Druid")

        val spell = driver.putCardInHand(activePlayer, "Bloodline Bidding")
        driver.giveMana(activePlayer, Color.BLACK, 8)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Goblin Warrior comes back (matches chosen type directly)
        driver.findPermanent(activePlayer, "Goblin Warrior") shouldNotBe null
        // Mistform Shifter comes back (Changeling matches every creature type)
        driver.findPermanent(activePlayer, "Mistform Shifter") shouldNotBe null
        // Elf Druid stays in the graveyard (not a Goblin and not a Changeling)
        driver.findPermanent(activePlayer, "Elf Druid") shouldBe null
    }

    test("only returns creatures from the active player's graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInGraveyard(activePlayer, "Goblin Warrior")
        driver.putCardInGraveyard(opponent, "Mistform Shifter")

        val spell = driver.putCardInHand(activePlayer, "Bloodline Bidding")
        driver.giveMana(activePlayer, Color.BLACK, 8)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        driver.findPermanent(activePlayer, "Goblin Warrior") shouldNotBe null
        // Opponent's Changeling stays put — Bloodline Bidding only affects your graveyard
        driver.findPermanent(opponent, "Mistform Shifter") shouldBe null
    }
})
