package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ghalta and Mavren — "Whenever you attack, choose one — • Create a tapped and attacking X/X
 * green Dinosaur token with trample, X = greatest power among other attacking creatures.
 * • Create X 1/1 white Vampire tokens with lifelink, X = number of other attacking creatures."
 */
class GhaltaAndMavrenScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveToModeChoice(): ChooseOptionDecision {
        var guard = 0
        while (getPendingDecision() !is ChooseOptionDecision && guard++ < 20) resolveStack()
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(prefix: String) {
        val decision = resolveToModeChoice()
        val index = decision.options.indexOfFirst { it.startsWith(prefix) }
        check(index >= 0) { "Mode '$prefix' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
        resolveStack()
    }

    private fun TestGame.tokens(name: String) =
        state.getBattlefield().filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Ghalta and Mavren", tapped = false, summoningSickness = false)
        .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("Dinosaur mode: tapped and attacking X/X trample, X = greatest power among OTHER attackers") {
            val game = board().build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Ghalta and Mavren" to 2, "Hill Giant" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null

            game.chooseMode("Create a tapped and attacking")

            val dinos = game.tokens("Dinosaur Token")
            dinos.size shouldBe 1
            val dino = dinos.single()
            // Hill Giant (3) is the greatest among the others — Ghalta's own 12 is excluded.
            game.state.projectedState.getPower(dino) shouldBe 3
            game.state.projectedState.getToughness(dino) shouldBe 3
            game.state.projectedState.hasKeyword(dino, Keyword.TRAMPLE) shouldBe true
            game.state.getEntity(dino)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(dino)!!.has<AttackingComponent>() shouldBe true
        }

        test("Vampire mode: X = number of other attackers, untapped lifelink 1/1s") {
            val game = board().build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Ghalta and Mavren" to 2, "Hill Giant" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null

            game.chooseMode("Create X 1/1 white Vampire")

            val vampires = game.tokens("Vampire Token")
            vampires.size shouldBe 2
            vampires.forEach { v ->
                game.state.projectedState.hasKeyword(v, Keyword.LIFELINK) shouldBe true
                game.state.getEntity(v)!!.has<AttackingComponent>() shouldBe false
            }
        }

        test("Vampire mode with no other attackers creates nothing") {
            val game = board().build()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Ghalta and Mavren" to 2)).error shouldBe null

            game.chooseMode("Create X 1/1 white Vampire")

            game.tokens("Vampire Token").size shouldBe 0
        }
    }
}
