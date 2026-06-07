package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Guildpact (GPT) Gruul cards.
 *
 * These cards reuse existing SDK primitives (mana abilities, stat modification, keyword
 * grant, card draw, X-damage-to-each-creature), so the tests just confirm the composed
 * behaviour resolves as the oracle text reads.
 */
class GptGruulCardsScenarioTest : ScenarioTestBase() {

    private fun ManaPoolComponent.total() =
        white + blue + black + red + green + colorless

    init {
        context("Gruul Signet — {1}, {T}: Add {R}{G}") {
            test("produces one red and one green when activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gruul Signet", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1) // pays the {1} activation cost
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val signet = game.findPermanent("Gruul Signet")!!
                val abilityId = cardRegistry.getCard("Gruul Signet")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = signet, abilityId = abilityId)
                )
                withClue("Activating Gruul Signet should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should add exactly one red and one green") {
                    pool.red shouldBe 1
                    pool.green shouldBe 1
                }
            }
        }

        context("Wild Cantor — Sacrifice this creature: Add one mana of any color") {
            test("sacrifices itself and adds one mana of the chosen color") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wild Cantor", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cantor = game.findPermanent("Wild Cantor")!!
                val abilityId = cardRegistry.getCard("Wild Cantor")!!.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = cantor, abilityId = abilityId)
                )

                // Pauses for a color choice; Wild Cantor is already sacrificed (cost paid).
                withClue("Wild Cantor should leave the battlefield (sacrificed as a cost)") {
                    game.isOnBattlefield("Wild Cantor") shouldBe false
                }

                val decision = game.getPendingDecision()
                    ?: error("Expected a color-choice decision after activating Wild Cantor")
                game.submitDecision(ColorChosenResponse(decision.id, Color.BLUE))

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Should add exactly one mana of the chosen color (blue)") {
                    pool.blue shouldBe 1
                    pool.total() shouldBe 1
                }
            }
        }

        context("Wildsize — target creature gets +2/+2 and gains trample; draw a card") {
            test("buffs the target, grants trample, and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wildsize")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withLandsOnBattlefield(1, "Forest", 3) // {2}{G}
                    .withCardInLibrary(1, "Mountain") // something to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                val result = game.castSpell(1, "Wildsize", bears)
                withClue("Casting Wildsize should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears should be 4/4 after +2/+2") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                withClue("Grizzly Bears should have trample") {
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
                withClue("Casting Wildsize ({2}{G}) and drawing should net +0 cards in hand (spent Wildsize, drew one)") {
                    // started with Wildsize in hand (handBefore includes it); after cast it leaves
                    // hand and we draw 1 -> hand size returns to handBefore.
                    game.handSize(1) shouldBe handBefore
                }
            }
        }

        context("Streetbreaker Wurm — vanilla 6/4") {
            test("enters as a 6/4 Wurm with no abilities") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Streetbreaker Wurm", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Streetbreaker Wurm")!!
                withClue("Streetbreaker Wurm is a 6/4") {
                    game.state.projectedState.getPower(wurm) shouldBe 6
                    game.state.projectedState.getToughness(wurm) shouldBe 4
                }
            }
        }

        context("Savage Twister — deals X damage to each creature") {
            test("X=2 kills 2-toughness creatures on both sides") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Savage Twister")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(2, "Glory Seeker", summoningSickness = false)  // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 4) // {X=2}{R}{G}
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castXSpell(1, "Savage Twister", xValue = 2)
                withClue("Casting Savage Twister should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Both 2/2 creatures should be destroyed by 2 damage each") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }
        }
    }
}
