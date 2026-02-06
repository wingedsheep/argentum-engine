package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Aggravated Assault.
 *
 * Card reference:
 * - Aggravated Assault ({2}{R}): Enchantment
 *   "{3}{R}{R}: Untap all creatures you control. After this main phase, there is
 *   an additional combat phase followed by an additional main phase. Activate only
 *   as a sorcery."
 */
class AggravatedAssaultScenarioTest : ScenarioTestBase() {

    private fun activateAggravatedAssault(game: TestGame) {
        val assaultId = game.findPermanent("Aggravated Assault")!!
        val cardDef = cardRegistry.getCard("Aggravated Assault")!!
        val ability = cardDef.script.activatedAbilities.first()

        val result = game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = assaultId,
                abilityId = ability.id
            )
        )
        withClue("Activation should succeed: ${result.error}") {
            result.error shouldBe null
        }
    }

    private fun addMana(game: TestGame, red: Int = 0, colorless: Int = 0) {
        game.state = game.state.updateEntity(game.player1Id) { container ->
            container.with(ManaPoolComponent(red = red, colorless = colorless))
        }
    }

    private fun isTapped(game: TestGame, cardName: String): Boolean {
        val entityId = game.findPermanent(cardName)!!
        return game.state.getEntity(entityId)?.has<TappedComponent>() == true
    }

    init {
        context("Aggravated Assault activated ability") {

            test("untaps all creatures you control on resolution") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aggravated Assault")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                    .withCardOnBattlefield(2, "Devoted Hero", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                addMana(game, red = 2, colorless = 3)
                activateAggravatedAssault(game)
                game.resolveStack()

                withClue("Grizzly Bears should be untapped") {
                    isTapped(game, "Grizzly Bears") shouldBe false
                }
                withClue("Hill Giant should be untapped") {
                    isTapped(game, "Hill Giant") shouldBe false
                }
                withClue("Opponent's Devoted Hero should still be tapped") {
                    isTapped(game, "Devoted Hero") shouldBe true
                }
            }

            test("creates additional combat phase after postcombat main") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aggravated Assault")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                addMana(game, red = 2, colorless = 3)
                activateAggravatedAssault(game)
                game.resolveStack()

                // Pass priority from postcombat main - should go to additional combat
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                withClue("Should be in additional combat phase") {
                    game.state.phase shouldBe Phase.COMBAT
                    game.state.step shouldBe Step.BEGIN_COMBAT
                }
            }

            test("can attack in additional combat phase") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aggravated Assault")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                addMana(game, red = 2, colorless = 3)
                activateAggravatedAssault(game)
                game.resolveStack()

                withClue("Grizzly Bears should be untapped after ability resolves") {
                    isTapped(game, "Grizzly Bears") shouldBe false
                }

                // Advance to the additional combat's declare attackers step
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Declare Grizzly Bears as attacker
                val attackResult = game.declareAttackers(mapOf("Grizzly Bears" to 2))
                withClue("Should be able to attack in additional combat: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance through combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Pass through combat damage to the additional postcombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Should reach additional postcombat main phase") {
                    game.state.phase shouldBe Phase.POSTCOMBAT_MAIN
                }

                // Opponent should have taken 2 damage from Grizzly Bears
                withClue("Opponent should have taken 2 damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("additional main phase transitions to end step normally") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aggravated Assault")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                addMana(game, red = 2, colorless = 3)
                activateAggravatedAssault(game)
                game.resolveStack()

                // Advance through additional combat
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Pass from the additional postcombat main - should go to END step normally
                game.passUntilPhase(Phase.ENDING, Step.END)

                withClue("Should reach end step after additional main phase") {
                    game.state.phase shouldBe Phase.ENDING
                    game.state.step shouldBe Step.END
                }
            }

            test("auto-tapping lands for activation cost properly consumes mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aggravated Assault")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardInHand(1, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                // Activate Aggravated Assault, which costs {3}{R}{R} = 5 mana
                // Player has 7 mountains, so 5 should be tapped, leaving 2 untapped
                activateAggravatedAssault(game)

                // Verify mana pool is empty after paying the cost
                val poolAfterActivation = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Mana pool should be empty after paying activation cost") {
                    poolAfterActivation?.red shouldBe 0
                    poolAfterActivation?.colorless shouldBe 0
                }

                // Count untapped mountains - should be exactly 2 (7 - 5)
                val untappedMountains = game.state.getBattlefield().count { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@count false
                    val card = container.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                    card?.name == "Mountain" && !container.has<TappedComponent>()
                }
                withClue("Should have 2 untapped mountains remaining (7 - 5 = 2)") {
                    untappedMountains shouldBe 2
                }

                // Resolve the ability
                game.resolveStack()

                // Now try to cast Battering Craghorn ({2}{R}{R} = 4 mana)
                // With only 2 untapped mountains, this should fail
                val castResult = game.castSpell(1, "Battering Craghorn")
                withClue("Should not be able to cast Battering Craghorn with only 2 untapped lands") {
                    castResult.error shouldNotBe null
                }
            }

            test("cannot activate at instant speed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aggravated Assault")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                addMana(game, red = 2, colorless = 3)

                val assaultId = game.findPermanent("Aggravated Assault")!!
                val cardDef = cardRegistry.getCard("Aggravated Assault")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = assaultId,
                        abilityId = ability.id
                    )
                )

                withClue("Should not be able to activate during combat") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
