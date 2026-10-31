package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Blow Your House Down (ELD #114) — {2}{R} Sorcery
 *
 *   Up to three target creatures can't block this turn. Destroy any of them that are Walls.
 *
 * The Wall clause is a per-target `EntityMatches(ContextTarget(0), Wall)` gate inside a
 * `ForEachTarget` loop: a targeted Wall dies, a targeted non-Wall only loses blocking, and a Wall
 * that wasn't targeted is untouched.
 */
class BlowYourHouseDownScenarioTest : ScenarioTestBase() {

    init {
        context("Blow Your House Down") {

            test("targeted Walls are destroyed, other targets can't block, untargeted creatures are unaffected") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Blow Your House Down")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
                    .withCardOnBattlefield(2, "Wall of Air", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    // Untargeted controls: a Wall that must survive, and a creature that may still block.
                    .withCardOnBattlefield(2, "Wall of Swords", summoningSickness = false)
                    .withCardOnBattlefield(2, "Llanowar Elves", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wallOfAir = game.findPermanent("Wall of Air")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val spell = game.findCardsInHand(1, "Blow Your House Down").first()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(
                            entityIdToChosenTarget(game.state, wallOfAir),
                            entityIdToChosenTarget(game.state, bears)
                        )
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the targeted Wall is destroyed") {
                    game.isOnBattlefield("Wall of Air") shouldBe false
                    game.isInGraveyard(2, "Wall of Air") shouldBe true
                }
                withClue("the targeted non-Wall survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("a Wall that wasn't targeted is not destroyed") {
                    game.isOnBattlefield("Wall of Swords") shouldBe true
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the surviving target can't block this turn") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser")))
                        .error shouldNotBe null
                }
                withClue("an untargeted creature blocks normally") {
                    game.declareBlockers(mapOf("Llanowar Elves" to listOf("Centaur Courser")))
                        .error shouldBe null
                }
            }
        }
    }
}
