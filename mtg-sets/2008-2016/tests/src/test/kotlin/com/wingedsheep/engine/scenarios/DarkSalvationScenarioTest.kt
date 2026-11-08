package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dark Salvation (EMN #87) — {X}{X}{B} Sorcery. "Target player creates X 2/2 black Zombie
 * creature tokens, then up to one target creature gets -1/-1 until end of turn for each Zombie
 * that player controls."
 *
 * Two things the script's shape alone doesn't prove: the Zombie count is read *after* the tokens
 * exist (the "then"), and it counts the *targeted* player's Zombies, not the caster's.
 */
class DarkSalvationScenarioTest : ScenarioTestBase() {

    init {
        context("Dark Salvation") {

            test("counts the target player's Zombies after the new tokens are created") {
                // Caster already controls one Zombie; X = 2 brings it to three, enough to kill a
                // 3/3. Counted before the tokens it would be only -1/-1.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dark Salvation")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Wailing Ghoul")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                cast(game, xValue = 2, player = game.player1Id, creature = giant)

                withClue("Player1 created two Zombie tokens") {
                    zombieTokens(game, game.player1Id).size shouldBe 2
                }
                withClue("Hill Giant got -3/-3 (Wailing Ghoul + two tokens) and died") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("targeting an opponent gives them the tokens and counts only their Zombies") {
                // The caster's two Zombies don't count: the opponent controls just the one token
                // X = 1 made, so Hill Giant gets only -1/-1.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dark Salvation")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Wailing Ghoul")
                    .withCardOnBattlefield(1, "Wailing Ghoul")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                cast(game, xValue = 1, player = game.player2Id, creature = giant)

                withClue("the opponent created the token; the caster created none") {
                    zombieTokens(game, game.player2Id).size shouldBe 1
                    zombieTokens(game, game.player1Id).size shouldBe 0
                }
                withClue("Hill Giant is a 2/2 for the turn") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }
            }

            test("with no creature targeted, the player just gets the tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dark Salvation")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                cast(game, xValue = 2, player = game.player1Id, creature = null)

                withClue("two 2/2 Zombie tokens for Player1") {
                    val tokens = zombieTokens(game, game.player1Id)
                    tokens.size shouldBe 2
                    tokens.forEach {
                        game.state.projectedState.getPower(it) shouldBe 2
                        game.state.projectedState.getToughness(it) shouldBe 2
                    }
                }
                withClue("Hill Giant is untouched") {
                    game.state.projectedState.getPower(giant) shouldBe 3
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }
        }
    }

    private fun cast(game: TestGame, xValue: Int, player: EntityId, creature: EntityId?) {
        val cardId = game.state.getHand(game.player1Id).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Dark Salvation"
        }
        val targets = listOfNotNull(ChosenTarget.Player(player), creature?.let { ChosenTarget.Permanent(it) })
        val result = game.execute(CastSpell(game.player1Id, cardId, targets, xValue = xValue))
        withClue("casting Dark Salvation for X=$xValue should succeed: ${result.error}") {
            result.error shouldBe null
        }
        game.resolveStack()
    }

    private fun zombieTokens(game: TestGame, playerId: EntityId): List<EntityId> =
        game.state.projectedState.getBattlefieldControlledBy(playerId).filter { id ->
            val entity = game.state.getEntity(id)
            entity?.has<TokenComponent>() == true &&
                game.state.projectedState.hasSubtype(id, "Zombie")
        }
}
