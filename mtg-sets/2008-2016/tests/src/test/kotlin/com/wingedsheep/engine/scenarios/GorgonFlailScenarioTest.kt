package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m10.cards.GorgonFlail
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gorgon Flail (M10 #211) — {2} Artifact — Equipment.
 * "Equipped creature gets +1/+1 and has deathtouch. Equip {2}"
 */
class GorgonFlailScenarioTest : ScenarioTestBase() {
    init {
        test("equip {2} gives the creature +1/+1 and deathtouch") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gorgon Flail")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Gorgon Flail")!!,
                    abilityId = GorgonFlail.activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
        }

        test("the equipped creature's damage destroys a bigger blocker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Gorgon Flail", "Grizzly Bears")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Craw Wurm" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("3 deathtouch damage destroys the 6/4 Craw Wurm") {
                game.isInGraveyard(2, "Craw Wurm") shouldBe true
            }
        }
    }
}
