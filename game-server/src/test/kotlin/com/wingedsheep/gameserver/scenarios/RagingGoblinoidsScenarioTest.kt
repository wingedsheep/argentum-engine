package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Raging Goblinoids.
 *
 * Card reference:
 * - Raging Goblinoids ({4}{R}): Creature — Goblin Berserker Villain, 5/4
 *   "Haste"
 *   "Mayhem — You may cast this spell from your graveyard for {2}{R}
 *    if you discarded it this turn."
 */
class RagingGoblinoidsScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Raging Goblinoids cast for mana cost") {

            test("enters battlefield as a 5/4 Haste Goblin Berserker Villain when cast for {4}{R}") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Raging Goblinoids")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Raging Goblinoids")
                withClue("Casting Raging Goblinoids for {4}{R} should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Raging Goblinoids should be on the battlefield") {
                    game.isOnBattlefield("Raging Goblinoids") shouldBe true
                }

                val goblinoidsId = game.findPermanent("Raging Goblinoids")!!

                withClue("Raging Goblinoids should be a Goblin Berserker Villain") {
                    game.state.getEntity(goblinoidsId)?.get<CardComponent>()?.typeLine?.toString() shouldBe
                        "Creature — Goblin Berserker Villain"
                }

                withClue("Raging Goblinoids should be a 5/4") {
                    stateProjector.getProjectedPower(game.state, goblinoidsId) shouldBe 5
                    stateProjector.getProjectedToughness(game.state, goblinoidsId) shouldBe 4
                }

                withClue("Raging Goblinoids should have Haste") {
                    game.state.projectedState.hasKeyword(goblinoidsId, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Raging Goblinoids" to 2))
                withClue("Raging Goblinoids should be able to attack the turn it enters (Haste)") {
                    attackResult.error shouldBe null
                }
            }
        }
    }
}
