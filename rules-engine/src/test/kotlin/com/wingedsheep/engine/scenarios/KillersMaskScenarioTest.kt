package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Killer's Mask (DSK #104) — {2}{B} Artifact — Equipment.
 *
 * "When this Equipment enters, manifest dread, then attach this Equipment to that creature."
 * "Equipped creature has menace." Equip {2}.
 *
 * Same manifest-dread-then-attach shape as Conductive Machete; here we additionally assert the
 * equipped (manifested) creature gains menace through the projector.
 */
class KillersMaskScenarioTest : ScenarioTestBase() {

    init {
        context("Killer's Mask") {
            test("ETB manifests dread and attaches to the manifested creature, granting menace") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Killer's Mask")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Killer's Mask")
                withClue("Casting Killer's Mask should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                var guard = 0
                while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) {
                    game.resolveStack()
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision for manifest dread; got ${game.getPendingDecision()}")
                val manifestPick = decision.options.first()
                game.submitDecision(CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(manifestPick)))
                game.resolveStack()

                withClue("The chosen card is a manifested 2/2 on the battlefield") {
                    game.state.getEntity(manifestPick)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
                    game.state.projectedState.getPower(manifestPick) shouldBe 2
                    game.state.projectedState.getToughness(manifestPick) shouldBe 2
                }

                val maskId = game.findPermanent("Killer's Mask")!!
                val attachedTo = game.state.getEntity(maskId)?.get<AttachedToComponent>()
                attachedTo.shouldNotBeNull()
                attachedTo.targetId shouldBe manifestPick

                withClue("Equipped creature has menace") {
                    game.state.projectedState.hasKeyword(manifestPick, Keyword.MENACE) shouldBe true
                }
            }
        }
    }
}
