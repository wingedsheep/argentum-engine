package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.MobilizerMech
import com.wingedsheep.mtg.sets.definitions.xln.cards.SleekSchooner
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MobilizerMechScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MobilizerMech, SleekSchooner))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("becoming crewed animates another Vehicle you control with its printed stats until end of turn") {
        val game = driver()
        val mech = game.putPermanentOnBattlefield(game.player1, "Mobilizer Mech")
        val schooner = game.putPermanentOnBattlefield(game.player1, "Sleek Schooner")
        val crew = listOf(
            game.putCreatureOnBattlefield(game.player1, "Grizzly Bears"),
            game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        )

        game.submitSuccess(CrewVehicle(game.player1, mech, crew))
        game.pendingDecision shouldBe null // activation alone doesn't make it crewed
        game.bothPass()

        game.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        game.submitTargetSelection(game.player1, listOf(schooner)).error shouldBe null
        game.bothPass()

        game.state.projectedState.isCreature(mech) shouldBe true
        game.state.projectedState.isCreature(schooner) shouldBe true
        game.state.projectedState.getPower(schooner) shouldBe 4
        game.state.projectedState.getToughness(schooner) shouldBe 3

        game.passPriorityUntil(Step.UPKEEP)
        game.state.projectedState.isCreature(schooner) shouldBe false
    }

    test("only another Vehicle you control is a legal target, and choosing none animates nothing else") {
        val game = driver()
        val mech = game.putPermanentOnBattlefield(game.player1, "Mobilizer Mech")
        val friendly = game.putPermanentOnBattlefield(game.player1, "Sleek Schooner")
        val opposing = game.putPermanentOnBattlefield(game.player2, "Sleek Schooner")
        val crew = listOf(
            game.putCreatureOnBattlefield(game.player1, "Grizzly Bears"),
            game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        )

        game.submitSuccess(CrewVehicle(game.player1, mech, crew))
        game.bothPass()

        val decision = game.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets.values.flatten()
        legal shouldContain friendly
        legal shouldNotContain mech
        legal shouldNotContain opposing

        game.submitTargetSelection(game.player1, emptyList()).error shouldBe null
        if (game.stackSize > 0) game.bothPass()

        game.state.projectedState.isCreature(mech) shouldBe true
        game.state.projectedState.isCreature(friendly) shouldBe false
        game.state.projectedState.isCreature(opposing) shouldBe false
    }
})
