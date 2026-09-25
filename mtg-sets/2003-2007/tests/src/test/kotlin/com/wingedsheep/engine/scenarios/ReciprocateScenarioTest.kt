package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.Frostwielder
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reciprocate (CHK #40) — "Exile target creature that dealt damage to you this turn."
 *
 * The damage may be combat or noncombat, must have been dealt to *you* (not another player), and
 * must have been dealt *this* turn.
 */
class ReciprocateScenarioTest : ScenarioTestBase() {

    private val pingAbility = Frostwielder.activatedAbilities.single().id

    init {
        // Player 2's turn: their Frostwielder pings someone, then Player 1 answers at instant speed.
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(2, "Frostwielder")
            .withCardInHand(1, "Reciprocate")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.pingPlayer(playerId: EntityId) {
            execute(
                ActivateAbility(
                    playerId = player2Id,
                    sourceId = findPermanent("Frostwielder")!!,
                    abilityId = pingAbility,
                    targets = listOf(ChosenTarget.Player(playerId)),
                )
            ).error shouldBe null
            resolveStack()
            // Player 2 passes with an empty stack, handing priority to Player 1.
            passPriority()
            state.priorityPlayerId shouldBe player1Id
        }

        context("Reciprocate") {
            test("exiles a creature that dealt noncombat damage to you this turn") {
                val game = base().build()
                game.pingPlayer(game.player1Id)
                game.getLifeTotal(1) shouldBe 19

                game.castSpell(1, "Reciprocate", game.findPermanent("Frostwielder")!!).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Frostwielder") shouldBe true
            }

            test("exiles a creature that dealt combat damage to you this turn") {
                val game = scenario().withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Reciprocate")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                withClue("the Bears connected") { game.getLifeTotal(1) shouldBe 18 }
                game.passPriority()
                game.state.priorityPlayerId shouldBe game.player1Id

                game.castSpell(1, "Reciprocate", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
            }

            test("can't target a creature that dealt damage only to another player") {
                val game = base().build()
                game.pingPlayer(game.player2Id)

                game.castSpell(1, "Reciprocate", game.findPermanent("Frostwielder")!!).error shouldNotBe null
                game.isOnBattlefield("Frostwielder") shouldBe true
            }

            test("can't target a creature whose damage to you was dealt on an earlier turn") {
                val game = base().build()
                game.pingPlayer(game.player1Id)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("now Player 1's turn") { game.state.activePlayerId shouldBe game.player1Id }

                game.castSpell(1, "Reciprocate", game.findPermanent("Frostwielder")!!).error shouldNotBe null
                game.isOnBattlefield("Frostwielder") shouldBe true
            }
        }
    }
}
