package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Arboria (LEG) — {2}{G}{G} World Enchantment: "Creatures can't attack a player unless that player
 * cast a spell or put a nontoken permanent onto the battlefield during their last turn."
 *
 * Each test starts in the opponent's (player 2's) main phase, lets something happen (or nothing),
 * then plays on to player 1's next declare-attackers step and tries to attack player 2.
 */
class ArboriaScenarioTest : ScenarioTestBase() {

    private fun opponentsTurn(extra: ScenarioBuilder.() -> Unit = {}): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Arboria")
            .withCardOnBattlefield(1, "Llanowar Elves")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        builder.extra()
        return builder.build()
    }

    /** From the opponent's turn, play on to player 1's next declare-attackers step. */
    private fun TestGame.toPlayersAttack() {
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        state.activePlayerId shouldBe player1Id
    }

    init {
        context("Arboria") {
            test("a player who did nothing during their last turn can't be attacked") {
                val game = opponentsTurn()
                game.toPlayersAttack()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldNotBeNull()
            }

            test("playing a land during their last turn opens them to attack") {
                val game = opponentsTurn { withCardInHand(2, "Forest") }
                val forest = game.findCardsInHand(2, "Forest").first()
                game.execute(PlayLand(game.player2Id, forest)).error.shouldBeNull()
                game.toPlayersAttack()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldBeNull()
            }

            test("casting a spell during their last turn opens them to attack") {
                val game = opponentsTurn {
                    withCardOnBattlefield(1, "Grizzly Bears")
                    withCardInHand(2, "Lightning Bolt")
                    withLandsOnBattlefield(2, "Mountain", 1)
                }
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error.shouldBeNull()
                game.resolveStack()
                game.toPlayersAttack()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldBeNull()
            }

            test("a spell cast during another player's turn isn't their last turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arboria")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                // Player 2 bolts during player 1's turn, then does nothing on their own turn.
                game.passPriority()
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error.shouldBeNull()
                game.resolveStack()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.toPlayersAttack()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldNotBeNull()
            }

            test("a permanent another player's spell returns under their control isn't theirs to count") {
                // Player 1 flickers player 2's creature on player 2's turn: Flicker of Fate's
                // controller put it onto the battlefield, not player 2 (Arboria ruling).
                val game = opponentsTurn {
                    withCardOnBattlefield(2, "Grizzly Bears")
                    withCardInHand(1, "Flicker of Fate")
                    withLandsOnBattlefield(1, "Plains", 2)
                }
                val theirBears = game.findPermanent("Grizzly Bears")!!
                game.passPriority()
                game.castSpell(1, "Flicker of Fate", theirBears).error.shouldBeNull()
                game.resolveStack()
                game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.toPlayersAttack()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldNotBeNull()
            }

            test("a delayed trigger another player controls returning their creature doesn't count either") {
                val game = opponentsTurn {
                    withCardOnBattlefield(2, "Grizzly Bears")
                    withCardInHand(1, "Otherworldly Journey")
                    withLandsOnBattlefield(1, "Plains", 2)
                }
                val theirBears = game.findPermanent("Grizzly Bears")!!
                game.passPriority()
                game.castSpell(1, "Otherworldly Journey", theirBears).error.shouldBeNull()
                game.resolveStack()
                // Returns at player 2's end step, under player 2's control, by player 1's trigger.
                game.toPlayersAttack()
                game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldNotBeNull()
            }

            test("a turn where they only created a token doesn't open them to attack") {
                // Master of the Wild Hunt makes a Wolf at player 2's upkeep: a token's creation is
                // not putting a nontoken permanent onto the battlefield.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arboria")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Master of the Wild Hunt")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()
                game.state.activePlayerId shouldBe game.player2Id
                game.state.getBattlefield().count {
                    game.state.getEntity(it)?.has<TokenComponent>() == true
                } shouldBe 1
                game.toPlayersAttack()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldNotBeNull()
            }

            test("a quiet turn replaces an earlier active one") {
                val game = opponentsTurn {
                    withCardInHand(2, "Forest")
                    withCardInLibrary(1, "Forest")
                    withCardInLibrary(2, "Forest")
                }
                val forest = game.findCardsInHand(2, "Forest").first()
                game.execute(PlayLand(game.player2Id, forest)).error.shouldBeNull()
                game.toPlayersAttack()
                // Player 1 skips this attack; player 2 then takes a turn doing nothing.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.toPlayersAttack()

                game.declareAttackers(mapOf("Llanowar Elves" to 2)).error.shouldNotBeNull()
            }

            test("planeswalkers stay attackable whatever their controller did") {
                val game = opponentsTurn { withCardOnBattlefield(2, "Liliana Vess") }
                game.toPlayersAttack()

                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Llanowar Elves" to "Liliana Vess")
                ).error.shouldBeNull()
            }
        }
    }
}
