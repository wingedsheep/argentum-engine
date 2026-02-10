package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for face-down morph creature rules (Rule 707.2).
 *
 * A face-down creature is a 2/2 colorless creature with no name, no creature types,
 * and no abilities. It can only be turned face-up by paying its morph cost.
 *
 * Cards used:
 * - Whipcorder ({W}{W}, 2/2 Creature — Human Soldier Rebel, "{W}, {T}: Tap target creature.", Morph {W})
 * - Akroma's Blessing ({2}{W}, Instant, "Choose a color. Creatures you control gain protection from the chosen color until end of turn.")
 * - Smother ({1}{B}, destroy target creature with CMC ≤ 3) — black removal spell
 * - Grizzly Bears (2/2, green creature)
 */
class MorphFaceDownRulesTest : ScenarioTestBase() {

    init {
        context("Face-down creatures and group effects (Rule 707.2)") {

            test("Akroma's Blessing grants protection to face-down morph creature") {
                // Face-down creatures are creatures, so "Creatures you control gain protection"
                // should include them.
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")       // Has morph {W}
                    .withCardInHand(1, "Akroma's Blessing") // Choose color, creatures you control gain protection
                    .withLandsOnBattlefield(1, "Plains", 6) // Enough mana for both
                    .withCardInHand(2, "Smother")           // {1}{B} destroy target creature CMC ≤ 3
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Whipcorder face-down for {3}
                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val castMorphResult = game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castMorphResult.error shouldBe null
                }
                game.resolveStack()

                // Verify face-down creature is on the battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Now cast Akroma's Blessing
                val castBlessingResult = game.castSpell(1, "Akroma's Blessing")
                withClue("Akroma's Blessing should be cast successfully: ${castBlessingResult.error}") {
                    castBlessingResult.error shouldBe null
                }

                // Resolve - should prompt for color choice
                game.resolveStack()
                withClue("Should have pending color choice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose black
                val decisionId = game.getPendingDecision()!!.id
                game.submitDecision(ColorChosenResponse(decisionId, Color.BLACK))

                // Pass priority to opponent
                game.passPriority()

                // Opponent tries to Smother the face-down creature - should fail
                // because it now has protection from black
                val smotherResult = game.castSpell(2, "Smother", faceDownId!!)
                withClue("Black spell should not target face-down creature with protection from black") {
                    smotherResult.error shouldNotBe null
                }
            }
        }

        context("Face-down creatures have no abilities (Rule 707.2)") {

            test("face-down Whipcorder cannot use its tap ability") {
                // Whipcorder has "{W}, {T}: Tap target creature" but face-down it has no abilities.
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withLandsOnBattlefield(1, "Plains", 4) // Enough for morph {3} + {W} for ability
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Whipcorder face-down
                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val castMorphResult = game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castMorphResult.error shouldBe null
                }
                game.resolveStack()

                // Verify face-down creature is on the battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Get the ability ID from the card definition
                val cardDef = cardRegistry.getCard("Whipcorder")!!
                val abilityId = cardDef.script.activatedAbilities.first().id

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Try to activate Whipcorder's tap ability while face-down
                val activateResult = game.execute(
                    ActivateAbility(game.player1Id, faceDownId, abilityId, targets = listOf(
                        com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bearsId)
                    ))
                )
                withClue("Face-down creature should not be able to activate abilities") {
                    activateResult.error shouldNotBe null
                }
            }
        }

    }
}
