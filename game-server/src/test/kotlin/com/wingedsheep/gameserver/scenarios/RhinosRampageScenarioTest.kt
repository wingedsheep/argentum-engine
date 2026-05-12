package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rhino's Rampage.
 *
 * Card reference:
 * - Rhino's Rampage ({R/G}): Sorcery
 *   "Target creature you control gets +1/+0 until end of turn. It fights target creature
 *    an opponent controls. When excess damage is dealt to the opponent's creature this way,
 *    destroy up to one target noncreature artifact with mana value 3 or less."
 */
class RhinosRampageScenarioTest : ScenarioTestBase() {

    private val testArtifactMv2 = CardDefinition.artifact(
        name = "Test Artifact MV2",
        manaCost = ManaCost.parse("{2}")
    )

    init {
        cardRegistry.register(testArtifactMv2)

        context("Rhino's Rampage — pump + fight + excess-damage artifact destruction") {

            test("pump + fight + excess damage destroys artifact") {
                // GIVEN: Active player has Rhino's Rampage in hand,
                //        a 2/2 creature (Glory Seeker) on their side,
                //        opponent has a 1/1 creature (Raging Goblin) and a noncreature artifact MV 2,
                //        active player has {R} of mana and it is their main phase.
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardInHand(1, "Rhino's Rampage")
                    .withCardOnBattlefield(1, "Glory Seeker")        // 2/2 Human Soldier
                    .withCardOnBattlefield(2, "Raging Goblin")       // 1/1 Goblin
                    .withCardOnBattlefield(2, "Test Artifact MV2")   // noncreature artifact, MV 2
                    .withLandsOnBattlefield(1, "Mountain", 1)        // {R} satisfies hybrid {R/G}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Rhino's Rampage"
                }!!
                val myCreatureId = game.findPermanent("Glory Seeker")!!
                val opponentCreatureId = game.findPermanent("Raging Goblin")!!
                val artifactId = game.findPermanent("Test Artifact MV2")!!

                // WHEN: Active player casts Rhino's Rampage targeting their 2/2,
                //       the opponent's 1/1, and the opponent's artifact.
                val castResult = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(myCreatureId),
                            ChosenTarget.Permanent(opponentCreatureId),
                            ChosenTarget.Permanent(artifactId)
                        )
                    )
                )
                withClue("Casting Rhino's Rampage should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // THEN: The active player's creature gets +1/+0 until end of turn (2/2 → 3/2)
                val projected = game.state.projectedState
                withClue("Glory Seeker should be pumped to 3/2 by Rhino's Rampage") {
                    projected.getPower(myCreatureId) shouldBe 3
                    projected.getToughness(myCreatureId) shouldBe 2
                }

                // AND: The fight happens — opponent's 1/1 receives 3 damage and dies (excess = 2)
                withClue("Raging Goblin should be destroyed by fight damage") {
                    game.isOnBattlefield("Raging Goblin") shouldBe false
                    game.isInGraveyard(2, "Raging Goblin") shouldBe true
                }

                // AND: Excess damage (2 > 0) triggers conditional artifact destruction
                withClue("Test Artifact MV2 should be destroyed due to excess damage trigger") {
                    game.isOnBattlefield("Test Artifact MV2") shouldBe false
                    game.isInGraveyard(2, "Test Artifact MV2") shouldBe true
                }

                // AND: Rhino's Rampage is placed in the caster's graveyard after resolving
                withClue("Rhino's Rampage should be in its owner's graveyard after resolving") {
                    game.isInGraveyard(1, "Rhino's Rampage") shouldBe true
                }
            }
        }
    }
}
