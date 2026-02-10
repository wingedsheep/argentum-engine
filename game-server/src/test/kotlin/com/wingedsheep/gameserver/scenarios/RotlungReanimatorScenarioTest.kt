package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rotlung Reanimator.
 *
 * Card reference:
 * - Rotlung Reanimator ({2}{B}): Creature — Zombie Cleric, 2/2
 *   "Whenever Rotlung Reanimator or another Cleric dies, create a 2/2 black Zombie creature token."
 */
class RotlungReanimatorScenarioTest : ScenarioTestBase() {

    init {
        context("Rotlung Reanimator self-death trigger") {
            test("creates a Zombie token when Rotlung Reanimator itself dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")
                    .withCardInHand(2, "Shock") // 2 damage kills the 2/2
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Shock the Rotlung Reanimator
                val castResult = game.castSpell(2, "Shock", game.findPermanent("Rotlung Reanimator")!!)
                withClue("Shock should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve Shock -> Rotlung dies -> death trigger creates token
                game.resolveStack()

                withClue("Rotlung Reanimator should be in graveyard") {
                    game.isInGraveyard(1, "Rotlung Reanimator") shouldBe true
                }

                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create exactly 1 Zombie token") {
                    zombieTokens.size shouldBe 1
                }
            }
        }

        context("Rotlung Reanimator triggers on another Cleric dying") {
            test("creates a Zombie token when another Cleric dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")
                    .withCardOnBattlefield(1, "Battlefield Medic") // 1/1 Cleric
                    .withCardInHand(2, "Shock") // kills the 1/1 Medic
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Shock the Battlefield Medic (another Cleric)
                val castResult = game.castSpell(2, "Shock", game.findPermanent("Battlefield Medic")!!)
                withClue("Shock should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve -> Medic dies -> Rotlung trigger creates token
                game.resolveStack()

                withClue("Battlefield Medic should be in graveyard") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }

                withClue("Rotlung Reanimator should still be on the battlefield") {
                    game.isOnBattlefield("Rotlung Reanimator") shouldBe true
                }

                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create exactly 1 Zombie token from Cleric death") {
                    zombieTokens.size shouldBe 1
                }
            }
        }

        context("Rotlung Reanimator triggers on opponent's Cleric dying") {
            test("creates a Zombie token when an opponent's Cleric dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")
                    .withCardOnBattlefield(2, "Battlefield Medic") // Opponent's Cleric
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Shock the opponent's Battlefield Medic
                val castResult = game.castSpell(1, "Shock", game.findPermanent("Battlefield Medic")!!)
                withClue("Shock should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve -> Opponent's Medic dies -> Rotlung trigger creates token for P1
                game.resolveStack()

                withClue("Battlefield Medic should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Battlefield Medic") shouldBe true
                }

                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create exactly 1 Zombie token from opponent's Cleric death") {
                    zombieTokens.size shouldBe 1
                }
            }
        }

        context("Rotlung Reanimator triggers when a Cleric is sacrificed") {
            test("creates a Zombie token when Cabal Archon sacrifices a Cleric") {
                // Cabal Archon: {B}, Sacrifice a Cleric: Target player loses 2 life and you gain 2 life.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")  // Zombie Cleric 2/2
                    .withCardOnBattlefield(1, "Cabal Archon")        // Human Cleric 2/2
                    .withCardOnBattlefield(1, "Battlefield Medic")   // Human Cleric 1/1
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val archonId = game.findPermanent("Cabal Archon")!!
                val medicId = game.findPermanent("Battlefield Medic")!!
                val cardDef = cardRegistry.getCard("Cabal Archon")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Cabal Archon, sacrificing Battlefield Medic, targeting Player 2
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = archonId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(medicId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                withClue("Battlefield Medic should be in graveyard after sacrifice") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }

                // Resolve the stack (Rotlung trigger should be on stack above Archon ability)
                game.resolveStack()

                withClue("Should create a Zombie token from sacrificed Cleric") {
                    game.findAllPermanents("Zombie Token").size shouldBe 1
                }

                withClue("Player 2 should lose 2 life from Cabal Archon") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }

        context("Rotlung Reanimator triggers when a Cleric is sacrificed via ETB trigger") {
            test("creates a Zombie token when Accursed Centaur forces sacrifice of a Cleric") {
                // Accursed Centaur: When it enters, sacrifice a creature.
                // Setup: Rotlung + Battlefield Medic on field, then cast Accursed Centaur.
                // The ETB trigger forces sacrifice - sacrifice the Medic (a Cleric).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")  // Zombie Cleric 2/2
                    .withCardOnBattlefield(1, "Battlefield Medic")   // Human Cleric 1/1
                    .withCardInHand(1, "Accursed Centaur")           // Zombie Centaur 2/2
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Accursed Centaur
                val castResult = game.castSpell(1, "Accursed Centaur")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve: Centaur enters -> ETB trigger goes on stack
                game.resolveStack()

                // ETB trigger resolves -> sacrifice decision (3 creatures to choose from)
                withClue("Should have pending sacrifice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Sacrifice Battlefield Medic (a Cleric)
                val medicId = game.findPermanent("Battlefield Medic")
                    ?: error("Battlefield Medic not found")
                game.selectCards(listOf(medicId))

                withClue("Battlefield Medic should be in graveyard") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }

                // Rotlung should have triggered from the Cleric sacrifice
                // Resolve any remaining triggers
                game.resolveStack()

                withClue("Should create a Zombie token from sacrificed Cleric") {
                    game.findAllPermanents("Zombie Token").size shouldBe 1
                }
            }
        }

        context("Rotlung Reanimator self-sacrifice trigger") {
            test("creates a Zombie token when Rotlung itself is sacrificed as a cost") {
                // Nantuko Husk: Sacrifice a creature: Gets +2/+2
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")  // Zombie Cleric 2/2
                    .withCardOnBattlefield(1, "Nantuko Husk")        // Zombie Insect 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val huskId = game.findPermanent("Nantuko Husk")!!
                val rotlungId = game.findPermanent("Rotlung Reanimator")!!
                val cardDef = cardRegistry.getCard("Nantuko Husk")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Nantuko Husk, sacrificing Rotlung Reanimator
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = huskId,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(rotlungId)
                        )
                    )
                )

                withClue("Ability should activate: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                withClue("Rotlung should be in graveyard") {
                    game.isInGraveyard(1, "Rotlung Reanimator") shouldBe true
                }

                // Rotlung's self-death trigger + Husk's ability should both be on stack
                // Resolve everything
                game.resolveStack()

                withClue("Should create a Zombie token from Rotlung's self-death trigger") {
                    game.findAllPermanents("Zombie Token").size shouldBe 1
                }
            }
        }

        context("Rotlung Reanimator triggers when a Cleric is sacrificed as additional spell cost") {
            test("creates a Zombie token when Final Strike sacrifices a Cleric") {
                // Final Strike: {2}{B}{B}, Additional cost: Sacrifice a creature.
                // Deals damage equal to sacrificed creature's power to target opponent.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator")  // Zombie Cleric 2/2
                    .withCardOnBattlefield(1, "Battlefield Medic")   // Human Cleric 1/1
                    .withCardInHand(1, "Final Strike")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Final Strike, sacrificing Battlefield Medic (a Cleric)
                val castResult = game.castSpellWithSacrifice(
                    1, "Final Strike", "Battlefield Medic", 2
                )
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                withClue("Battlefield Medic should be in graveyard after sacrifice") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }

                // Resolve stack - Rotlung trigger + Final Strike
                game.resolveStack()

                withClue("Should create a Zombie token from sacrificed Cleric") {
                    game.findAllPermanents("Zombie Token").size shouldBe 1
                }

                withClue("Player 2 should take 1 damage from Final Strike (Medic had 1 power)") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }

        context("Multiple Cleric deaths trigger multiple tokens") {
            test("Infest kills multiple Clerics and creates tokens for each") {
                // Infest: All creatures get -2/-2 until end of turn
                // This kills both the 2/2 Rotlung and the 1/1 Medic
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rotlung Reanimator") // 2/2 Cleric
                    .withCardOnBattlefield(1, "Battlefield Medic")  // 1/1 Cleric
                    .withCardInHand(1, "Infest")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Infest")
                withClue("Infest should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve Infest -> both creatures die -> triggers fire
                game.resolveStack()

                withClue("Rotlung Reanimator should be in graveyard") {
                    game.isInGraveyard(1, "Rotlung Reanimator") shouldBe true
                }
                withClue("Battlefield Medic should be in graveyard") {
                    game.isInGraveyard(1, "Battlefield Medic") shouldBe true
                }

                // Rotlung dying = 1 token (self-death trigger)
                // Medic dying = 1 token (other Cleric trigger, but Rotlung is also dying so
                // whether it sees the Medic die depends on last-known-info)
                // This is a subtle rules interaction - both die simultaneously,
                // so Rotlung still sees the Medic die (Rule 603.10)
                val zombieTokens = game.findAllPermanents("Zombie Token")
                withClue("Should create 2 Zombie tokens (one for each Cleric death)") {
                    zombieTokens.size shouldBe 2
                }
            }
        }
    }
}
