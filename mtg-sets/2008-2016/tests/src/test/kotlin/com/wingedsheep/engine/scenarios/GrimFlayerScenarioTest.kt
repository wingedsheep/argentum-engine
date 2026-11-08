package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grim Flayer (EMN #184) — {B}{G} Creature — Human Warrior, 2/2.
 *
 *   Trample
 *   Whenever this creature deals combat damage to a player, surveil 3.
 *   Delirium — This creature gets +2/+2 as long as there are four or more card types among cards
 *   in your graveyard.
 *
 * The surveil is the delirium enabler: binning an artifact and a land on top of a creature, an
 * instant and nothing else turns the 2/2 into a 4/4.
 */
class GrimFlayerScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grim Flayer")
        .withCardInLibrary(1, "Ornithopter")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInGraveyard(1, "Lightning Bolt")  // instant
        .withCardInGraveyard(1, "Lava Spike")      // sorcery
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackAndAwaitSurveil(): SelectCardsDecision {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grim Flayer" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var guard = 0
        while (state.pendingDecision !is SelectCardsDecision && guard++ < 20) resolveStack()
        return state.pendingDecision as? SelectCardsDecision
            ?: error("expected the surveil prompt; got ${state.pendingDecision}")
    }

    init {
        test("combat damage to a player surveils 3, and filling the graveyard turns on delirium") {
            val game = board()
            val flayer = game.findPermanent("Grim Flayer")!!
            game.state.projectedState.getPower(flayer) shouldBe 2

            val surveil = game.attackAndAwaitSurveil()
            game.getLifeTotal(2) shouldBe 18
            withClue("surveil 3 looks at exactly the top three cards") {
                surveil.options.toSet() shouldBe (
                    game.findCardsInLibrary(1, "Ornithopter") +
                        game.findCardsInLibrary(1, "Forest") +
                        game.findCardsInLibrary(1, "Grizzly Bears")
                    ).toSet()
            }

            val toGraveyard = game.findCardsInLibrary(1, "Ornithopter") + game.findCardsInLibrary(1, "Forest")
            game.selectCards(toGraveyard).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.isInGraveyard(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            withClue("artifact, land, instant, sorcery — delirium makes it a 4/4") {
                game.state.projectedState.getPower(flayer) shouldBe 4
                game.state.projectedState.getToughness(flayer) shouldBe 4
            }
        }

        test("a blocked Grim Flayer that deals no damage to the player doesn't surveil") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grim Flayer")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grim Flayer" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grim Flayer"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 20
            game.librarySize(1) shouldBe 1
            game.isInGraveyard(1, "Grim Flayer") shouldBe true
        }
    }
}
