package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.TheFallOfKroog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.sdk.model.Deck

class TheFallOfKroogScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + TheFallOfKroog)
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player1, Color.RED, 6)
    }

    test("offer binds the land to the selected opponent and resolves every instruction") {
        val d = setup()
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        val mine = d.putLandOnBattlefield(d.player1, "Mountain")
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        val myElf = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
        val offer = d.legalActions(d.player1).single { (it.action as? CastSpell)?.cardId == spell }
        offer.targetRequirements!![0].validTargets shouldBe listOf(d.player2)
        offer.targetRequirements!![1].validTargetsByPrefix shouldBe mapOf(d.player2.toString() to listOf(land))
        d.castSpell(d.player1, spell, listOf(d.player2, land)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
        d.getLifeTotal(d.player1) shouldBe 20
        d.assertInGraveyard(d.player2, "Forest")
        d.assertInGraveyard(d.player2, "Llanowar Elves")
        d.state.getBattlefield().containsAll(listOf(mine, myElf)) shouldBe true
    }

    test("cannot cast with own land or own player as the opponent target") {
        val d = setup()
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        val land = d.putLandOnBattlefield(d.player1, "Forest")
        (d.castSpell(d.player1, spell, listOf(d.player2, land)).error != null) shouldBe true
        (d.castSpell(d.player1, spell, listOf(d.player1, land)).error != null) shouldBe true
        d.getHand(d.player1).contains(spell) shouldBe true
    }

    test("no offer when opponent has no land even if caster does") {
        val d = setup()
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        d.putLandOnBattlefield(d.player1, "Forest")
        d.legalActions(d.player1).any { (it.action as? CastSpell)?.cardId == spell } shouldBe false
    }

    test("land leaving before resolution does not stop the player and creature damage") {
        val d = setup()
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        d.castSpell(d.player1, spell, listOf(d.player2, land)).outcome shouldBe Outcome.Done
        d.moveToGraveyard(land)
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
        d.assertInGraveyard(d.player2, "Llanowar Elves")
    }

    test("land changing control becomes illegal but damage still happens") {
        val d = setup()
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        d.castSpell(d.player1, spell, listOf(d.player2, land)).outcome shouldBe Outcome.Done
        d.replaceState(d.state.updateEntity(land) { it.with(ControllerComponent(d.player1)) })
        d.bothPass()
        d.state.getBattlefield().contains(land) shouldBe true
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("illegal player target takes no damage and supplies no creature group but legal land is destroyed") {
        val d = setup()
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        val elf = d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        d.castSpell(d.player1, spell, listOf(d.player2, land)).outcome shouldBe Outcome.Done
        d.replaceState(d.state.updateEntity(d.player2) { it.with(PlayerShroudComponent()) })
        d.bothPass()
        d.assertInGraveyard(d.player2, "Forest")
        d.getLifeTotal(d.player2) shouldBe 20
        d.state.getBattlefield().contains(elf) shouldBe true
    }

    test("all targets illegal leaves every creature untouched") {
        val d = setup()
        val spell = d.putCardInHand(d.player1, "The Fall of Kroog")
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        val elf = d.putCreatureOnBattlefield(d.player2, "Llanowar Elves")
        d.castSpell(d.player1, spell, listOf(d.player2, land)).outcome shouldBe Outcome.Done
        d.moveToGraveyard(land)
        d.replaceState(d.state.updateEntity(d.player2) { it.with(PlayerShroudComponent()) })
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 20
        d.state.getBattlefield().contains(elf) shouldBe true
        d.assertInGraveyard(d.player1, "The Fall of Kroog")
    }
})
