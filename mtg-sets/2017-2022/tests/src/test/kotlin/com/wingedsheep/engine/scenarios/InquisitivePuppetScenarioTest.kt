package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.InquisitivePuppet
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Inquisitive Puppet (ELD #223) — {1} Artifact Creature — Construct 0/2.
 *
 * "When this creature enters, scry 1. Exile this creature: Create a 1/1 white Human creature token."
 */
class InquisitivePuppetScenarioTest : ScenarioTestBase() {

    init {
        context("Inquisitive Puppet") {

            test("ETB scry 1 can bottom the top card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Inquisitive Puppet")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Inquisitive Puppet").error shouldBe null
                game.resolveStack()

                val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                scry.options.size shouldBe 1
                game.selectCards(scry.options).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                game.state.getLibrary(game.player1Id).last() shouldBe scry.options.single()
            }

            test("exiling it creates a 1/1 white Human token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Inquisitive Puppet")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Inquisitive Puppet")!!,
                        abilityId = InquisitivePuppet.activatedAbilities.single().id,
                    ),
                ).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Inquisitive Puppet") shouldBe true
                val humans = game.findPermanents("Human Token")
                humans.size shouldBe 1
                game.state.projectedState.getPower(humans.single()) shouldBe 1
                game.state.projectedState.getToughness(humans.single()) shouldBe 1
            }
        }
    }
}
