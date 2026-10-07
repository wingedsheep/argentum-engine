package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.PeacewalkerColossus
import com.wingedsheep.mtg.sets.definitions.xln.cards.SleekSchooner
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PeacewalkerColossusScenarioTest : FunSpec({
    val reshapeVehicle = card("Test Reshaping Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 7
        toughness = 7
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.BecomeCreature(EffectTarget.Self, power = 1, toughness = 2)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(PeacewalkerColossus, SleekSchooner, reshapeVehicle))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player1, Color.WHITE, 4)
    }

    test("animates another Vehicle with its printed stats until cleanup and it can crew the Colossus") {
        val game = driver()
        val colossus = game.putPermanentOnBattlefield(game.player1, "Peacewalker Colossus")
        val vehicle = game.putPermanentOnBattlefield(game.player1, "Test Reshaping Vehicle")
        game.state.projectedState.isCreature(vehicle) shouldBe false
        game.submit(ActivateAbility(game.player1, colossus,
            PeacewalkerColossus.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Permanent(vehicle)))).error shouldBe null
        game.bothPass()
        game.state.projectedState.isCreature(vehicle) shouldBe true
        game.state.projectedState.getPower(vehicle) shouldBe 7
        game.state.projectedState.getToughness(vehicle) shouldBe 7
        game.submit(CrewVehicle(game.player1, colossus, listOf(vehicle))).error shouldBe null
        game.bothPass()
        game.state.projectedState.isCreature(colossus) shouldBe true
        game.state.projectedState.getPower(colossus) shouldBe 6
        game.passPriorityUntil(Step.UPKEEP)
        game.state.projectedState.isCreature(vehicle) shouldBe false
        game.state.projectedState.isCreature(colossus) shouldBe false
    }

    test("does not overwrite an earlier base power and toughness setting effect") {
        val game = driver()
        val colossus = game.putPermanentOnBattlefield(game.player1, "Peacewalker Colossus")
        val vehicle = game.putPermanentOnBattlefield(game.player1, "Test Reshaping Vehicle")
        game.submit(ActivateAbility(game.player1, vehicle,
            reshapeVehicle.activatedAbilities.first().id)).error shouldBe null
        game.bothPass()
        game.submit(ActivateAbility(game.player1, colossus,
            PeacewalkerColossus.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Permanent(vehicle)))).error shouldBe null
        game.bothPass()
        game.state.projectedState.getPower(vehicle) shouldBe 1
        game.state.projectedState.getToughness(vehicle) shouldBe 2
    }

    test("cannot target itself an opposing Vehicle or a non-Vehicle") {
        val game = driver()
        val colossus = game.putPermanentOnBattlefield(game.player1, "Peacewalker Colossus")
        val opposing = game.putPermanentOnBattlefield(game.player2, "Sleek Schooner")
        val creature = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        for (invalidTarget in listOf(colossus, opposing, creature)) {
            game.submitExpectFailure(ActivateAbility(game.player1, colossus,
                PeacewalkerColossus.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(invalidTarget))))
        }
    }
})
