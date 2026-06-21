package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Job select (Final Fantasy) — the shared Equipment shell wired by `jobSelect()`:
 *
 *   "Job select (When this Equipment enters, create a 1/1 colorless Hero creature token,
 *    then attach this to it.)"
 *
 * Proven through Monk's Fist (FIN #265, {2} Equipment, "+1/+0 and is a Monk").
 *
 * The shell composes two existing primitives through the token pipeline: a
 * `CreateTokenEffect` that publishes the new token's id into the `createdTokens` pipeline
 * slot, followed by an `AttachEquipmentEffect` reading `PipelineTarget(CREATED_TOKENS, 0)`
 * so the source Equipment attaches to the creature it just made — in a single ETB
 * resolution with no declared target.
 */
class JobSelectScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Job select — Monk's Fist makes its own bearer on ETB") {

            test("ETB creates a 1/1 colorless Hero token and attaches the Equipment to it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Monk's Fist")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Monk's Fist")
                withClue("Casting Monk's Fist should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // (1) Exactly one Hero token was created.
                withClue("Job select should create exactly one Hero token") {
                    game.findPermanents("Hero Token").size shouldBe 1
                }
                val heroToken = game.findPermanent("Hero Token")!!
                val tokenCard = game.state.getEntity(heroToken)!!.get<CardComponent>()!!

                // (2) The token is a real token, colorless, a Hero creature. (Its 1/1 base is
                //     proven by the projected 2/1 below: toughness stays 1 under a +1/+0 buff.)
                withClue("Hero token should be a token entity") {
                    (game.state.getEntity(heroToken)!!.get<TokenComponent>() != null) shouldBe true
                }
                withClue("Hero token should be colorless (no colors): ${tokenCard.colors}") {
                    tokenCard.colors.isEmpty() shouldBe true
                }

                val projectedBefore = stateProjector.project(game.state)
                withClue("Hero token should be a creature with subtype Hero") {
                    projectedBefore.isCreature(heroToken) shouldBe true
                    projectedBefore.hasSubtype(heroToken, "Hero") shouldBe true
                }

                // (3) The Equipment auto-attached to the freshly-created token (no target chosen).
                val monksFist = game.findPermanent("Monk's Fist")
                withClue("Monk's Fist should be on the battlefield after resolving") {
                    (monksFist != null) shouldBe true
                }
                withClue(
                    "Monk's Fist should be attached to the Hero token " +
                        "(observed: ${game.state.getEntity(monksFist!!)?.get<AttachedToComponent>()?.targetId})"
                ) {
                    game.state.getEntity(monksFist)?.get<AttachedToComponent>()?.targetId shouldBe heroToken
                }
                withClue("Hero token should list Monk's Fist among its attachments") {
                    game.state.getEntity(heroToken)?.get<AttachmentsComponent>()?.attachedIds shouldBe listOf(monksFist)
                }

                // (4) The equipped-creature static abilities apply through the attachment:
                //     1/1 base + (+1/+0) = 2/1, and it gains the Monk subtype.
                val projected = stateProjector.project(game.state)
                withClue("Equipped Hero token should be 2/1 (base 1/1 + Monk's Fist +1/+0)") {
                    projected.getPower(heroToken) shouldBe 2
                    projected.getToughness(heroToken) shouldBe 1
                }
                withClue("Equipped Hero token should be a Monk in addition to its other types") {
                    projected.hasSubtype(heroToken, "Monk") shouldBe true
                    projected.hasSubtype(heroToken, "Hero") shouldBe true
                }

                // (5) The buff is conditional on the attachment — Monk's Fist itself is not a
                //     creature and gets no +1/+0, and the bonus doesn't leak to other permanents.
                withClue("Monk's Fist itself should not be a creature") {
                    projected.isCreature(monksFist) shouldBe false
                }
            }
        }
    }
}
