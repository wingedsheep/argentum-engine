package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Lurker.
 *
 * The card is a *spells-only* restriction, so the two halves that matter are opposite signs: a
 * spell can't target the idle Lurker, and an ability can. Modelling this as shroud would pass the
 * first and fail the second, which is the whole reason the engine now distinguishes the two.
 *
 * The third case is the "unless": once it has blocked, a spell targets it fine — which also
 * exercises the turn-scoped blocked marker, since the check happens after combat has ended.
 */
class LurkerScenarioTest : ScenarioTestBase() {

    init {
        context("Lurker") {

            test("a spell can't target it while it has neither attacked nor blocked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Lurker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurker = game.findPermanent("Lurker")!!
                game.castSpell(1, "Lightning Bolt", lurker).error shouldNotBe null
            }

            test("an ability can target it — this is not shroud") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Samite Healer")
                    .withCardOnBattlefield(2, "Lurker")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurker = game.findPermanent("Lurker")!!
                val healer = game.findPermanent("Samite Healer")!!
                val abilityId = com.wingedsheep.mtg.sets.definitions.lea.cards.SamiteHealer
                    .activatedAbilities.first().id

                val result = game.execute(
                    com.wingedsheep.engine.core.ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = healer,
                        abilityId = abilityId,
                        targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(lurker)),
                    )
                )
                withClue("abilities are unaffected by a spells-only restriction: ${result.error}") {
                    result.error shouldBe null
                }
            }

            test("once it has blocked, a spell can target it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Lurker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurker = game.findPermanent("Lurker")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Lurker" to listOf("Grizzly Bears"))).error shouldBe null

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("it blocked this turn, so the restriction is off — and the marker is turn-scoped, not per-combat") {
                    game.castSpell(1, "Lightning Bolt", lurker).error shouldBe null
                }
            }
        }
    }
}
