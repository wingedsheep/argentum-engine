package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.VoidmageProdigy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Voidmage Prodigy.
 *
 * Voidmage Prodigy: {U}{U}
 * Creature — Human Wizard
 * 2/1
 * {U}{U}, Sacrifice a Wizard: Counter target spell.
 * Morph {U}
 */
class VoidmageProdigyTest : FunSpec({

    val counterAbilityId = VoidmageProdigy.activatedAbilities[0].id

    // Another Wizard to sacrifice (not the Prodigy itself)
    val MerfolkWizard = CardDefinition.creature(
        name = "Merfolk Wizard",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Merfolk"), Subtype("Wizard")),
        power = 1,
        toughness = 1
    )

    // Non-Wizard creature for negative tests
    val GoblinScout = CardDefinition.creature(
        name = "Goblin Scout",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MerfolkWizard, GoblinScout))
        return driver
    }

    test("counter a spell by sacrificing another Wizard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 has Voidmage Prodigy and a Merfolk Wizard on battlefield
        val prodigy = driver.putCreatureOnBattlefield(player1, "Voidmage Prodigy")
        driver.removeSummoningSickness(prodigy)
        val wizard = driver.putCreatureOnBattlefield(player1, "Merfolk Wizard")

        // Player 2 casts Lightning Bolt targeting Player 1
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1) // Pass to player 2
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!

        // Player 2 passes priority back so player 1 can respond
        driver.passPriority(player2)

        // Player 1 activates Voidmage Prodigy, sacrificing the Merfolk Wizard
        driver.giveMana(player1, Color.BLUE, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = player1,
                sourceId = prodigy,
                abilityId = counterAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(wizard))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the counter ability
        driver.bothPass()

        // Lightning Bolt should be countered (in graveyard)
        driver.getGraveyardCardNames(player2) shouldContain "Lightning Bolt"

        // Merfolk Wizard should be sacrificed
        driver.findPermanent(player1, "Merfolk Wizard") shouldBe null

        // Voidmage Prodigy should still be on the battlefield
        driver.findPermanent(player1, "Voidmage Prodigy") shouldBe prodigy

        // Player 1 should not have taken damage
        driver.getLifeTotal(player1) shouldBe 20
    }

    test("can sacrifice itself to counter a spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 has only Voidmage Prodigy (a Wizard itself)
        val prodigy = driver.putCreatureOnBattlefield(player1, "Voidmage Prodigy")
        driver.removeSummoningSickness(prodigy)

        // Player 2 casts Lightning Bolt targeting Player 1
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(player2)

        // Player 1 sacrifices the Prodigy itself to counter
        driver.giveMana(player1, Color.BLUE, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = player1,
                sourceId = prodigy,
                abilityId = counterAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(prodigy))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the counter ability
        driver.bothPass()

        // Lightning Bolt should be countered
        driver.getGraveyardCardNames(player2) shouldContain "Lightning Bolt"

        // Prodigy should be in the graveyard
        driver.findPermanent(player1, "Voidmage Prodigy") shouldBe null
        driver.getGraveyardCardNames(player1) shouldContain "Voidmage Prodigy"

        // Player 1 should not have taken damage
        driver.getLifeTotal(player1) shouldBe 20
    }

    test("cannot sacrifice a non-Wizard creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val prodigy = driver.putCreatureOnBattlefield(player1, "Voidmage Prodigy")
        driver.removeSummoningSickness(prodigy)
        val goblin = driver.putCreatureOnBattlefield(player1, "Goblin Scout")

        // Player 2 casts Lightning Bolt
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(player2)

        // Trying to sacrifice a Goblin (not a Wizard) should fail
        driver.giveMana(player1, Color.BLUE, 2)
        val result = driver.submit(
            ActivateAbility(
                playerId = player1,
                sourceId = prodigy,
                abilityId = counterAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(goblin))
            )
        )
        result.isSuccess shouldBe false

        // Goblin should still be on the battlefield
        driver.findPermanent(player1, "Goblin Scout") shouldBe goblin
    }

    test("requires mana to activate") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val prodigy = driver.putCreatureOnBattlefield(player1, "Voidmage Prodigy")
        driver.removeSummoningSickness(prodigy)
        val wizard = driver.putCreatureOnBattlefield(player1, "Merfolk Wizard")

        // Player 2 casts Lightning Bolt
        val bolt = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.passPriority(player1)
        driver.castSpell(player2, bolt, listOf(player1))

        val spellOnStack = driver.getTopOfStack()!!
        driver.passPriority(player2)

        // Player 1 tries to activate without enough mana (only 1 blue, needs 2)
        driver.giveMana(player1, Color.BLUE, 1)
        val result = driver.submit(
            ActivateAbility(
                playerId = player1,
                sourceId = prodigy,
                abilityId = counterAbilityId,
                targets = listOf(ChosenTarget.Spell(spellOnStack)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(wizard))
            )
        )
        result.isSuccess shouldBe false

        // Wizard should still be on the battlefield (not sacrificed)
        driver.findPermanent(player1, "Merfolk Wizard") shouldBe wizard
    }
})
