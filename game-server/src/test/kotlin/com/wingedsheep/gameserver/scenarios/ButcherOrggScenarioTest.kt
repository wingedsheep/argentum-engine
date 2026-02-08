package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Butcher Orgg's "divide combat damage freely" ability.
 *
 * Butcher Orgg: {4}{R}{R}{R} 6/6 Creature — Orgg
 * "You may assign Butcher Orgg's combat damage divided as you choose among
 * defending player and/or any number of creatures they control."
 *
 * Key rulings (2004-10-04):
 * - If blocked but all blockers removed before combat damage, it can't deal combat damage.
 * - You can use the ability to divide damage even if it is not blocked.
 * - Minimum 1 point per creature or player chosen as a recipient.
 * - The ability does not target, so shroud/hexproof do not stop it.
 */
class ButcherOrggScenarioTest : ScenarioTestBase() {

    init {
        context("Butcher Orgg combat damage division") {

            test("unblocked Butcher Orgg deals full damage to defending player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Butcher Orgg") // 6/6
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val orggId = game.findPermanent("Butcher Orgg")!!
                val startingLife = game.getLifeTotal(2)

                // Declare Butcher Orgg as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(orggId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance through combat (no blockers)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Unblocked 6/6 deals 6 damage to opponent
                withClue("Defending player should take 6 damage from unblocked Butcher Orgg") {
                    game.getLifeTotal(2) shouldBe startingLife - 6
                }
            }

            test("blocked by single small creature - excess damage goes to defending player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Butcher Orgg") // 6/6
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val orggId = game.findPermanent("Butcher Orgg")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val startingLife = game.getLifeTotal(2)

                // Declare attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(orggId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Grizzly Bears
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(orggId)))
                )
                withClue("Block should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Advance to combat damage step (where the distribution decision is created)
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Submit damage distribution: 2 to blocker (lethal), 4 to player
                withClue("Should have distribute decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.submitDistribution(mapOf(bearsId to 2, game.player2Id to 4))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Bears should be dead (received 2 lethal)
                withClue("Grizzly Bears should be dead") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }

                // Remaining 4 damage to defending player (6 - 2 lethal = 4)
                withClue("Defending player should take 4 excess damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 4
                }

                // Butcher Orgg survives with 2 damage from Bears
                withClue("Butcher Orgg should survive") {
                    game.findPermanent("Butcher Orgg") shouldNotBe null
                }
            }

            test("blocked by multiple creatures - excess goes to defending player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Butcher Orgg")  // 6/6
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withCardOnBattlefield(2, "Goblin Bully")  // 2/1
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val orggId = game.findPermanent("Butcher Orgg")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val bullyId = game.findPermanent("Goblin Bully")!!
                val startingLife = game.getLifeTotal(2)

                // Declare attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(orggId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with both creatures
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            bearsId to listOf(orggId),
                            bullyId to listOf(orggId)
                        )
                    )
                )
                withClue("Block should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Multiple blockers require damage assignment order decision
                val decision = game.getPendingDecision()
                withClue("Should have damage assignment order decision") {
                    decision shouldNotBe null
                    decision shouldBe io.kotest.matchers.types.beInstanceOf<OrderObjectsDecision>()
                }

                // Order: Bears first, then Goblin Bully
                game.submitDecision(
                    OrderedResponse((decision as OrderObjectsDecision).id, listOf(bearsId, bullyId))
                )

                // Advance to combat damage step (where the distribution decision is created)
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Submit damage distribution: 2 to bears (lethal), 1 to bully (lethal), 3 to player
                withClue("Should have distribute decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.submitDistribution(mapOf(bearsId to 2, bullyId to 1, game.player2Id to 3))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Both blockers should be dead
                withClue("Grizzly Bears should be dead") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("Goblin Bully should be dead") {
                    game.findPermanent("Goblin Bully") shouldBe null
                }

                // Remaining damage to player: 6 - 2 (bears lethal) - 1 (bully lethal) = 3
                withClue("Defending player should take 3 excess damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 3
                }
            }

            test("blocked by large creature - little excess to defending player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Butcher Orgg")    // 6/6
                    .withCardOnBattlefield(2, "Barkhide Mauler") // 4/4
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val orggId = game.findPermanent("Butcher Orgg")!!
                val maulerId = game.findPermanent("Barkhide Mauler")!!
                val startingLife = game.getLifeTotal(2)

                // Declare attacker
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(orggId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with 4/4
                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(maulerId to listOf(orggId)))
                )

                // Advance to combat damage step (where the distribution decision is created)
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Submit damage distribution: 4 to blocker (lethal), 2 to player
                withClue("Should have distribute decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.submitDistribution(mapOf(maulerId to 4, game.player2Id to 2))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // 4/4 dies (received 4 lethal), remaining 2 to player
                withClue("Barkhide Mauler should be dead") {
                    game.findPermanent("Barkhide Mauler") shouldBe null
                }
                withClue("Defending player should take 2 excess damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 2
                }

                // Butcher Orgg takes 4 damage from blocker but survives (6 toughness)
                withClue("Butcher Orgg should survive with 4 damage") {
                    game.findPermanent("Butcher Orgg") shouldNotBe null
                }
            }

            test("blocked by creature with toughness greater than power - all damage to blocker, 0 to player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Butcher Orgg")     // 6/6
                    .withCardOnBattlefield(2, "Towering Baloth")  // 7/6
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val orggId = game.findPermanent("Butcher Orgg")!!
                val balothId = game.findPermanent("Towering Baloth")!!
                val startingLife = game.getLifeTotal(2)

                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(orggId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(balothId to listOf(orggId)))
                )

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // 7/6 Baloth has 6 toughness, gets 6 damage from Orgg = lethal
                // No excess damage to player (6 power - 6 lethal = 0)
                withClue("Towering Baloth should be dead") {
                    game.findPermanent("Towering Baloth") shouldBe null
                }
                withClue("Defending player should take no damage") {
                    game.getLifeTotal(2) shouldBe startingLife
                }

                // Butcher Orgg takes 7 damage from Baloth and dies (6 toughness < 7)
                withClue("Butcher Orgg should be dead from Baloth's counterattack") {
                    game.findPermanent("Butcher Orgg") shouldBe null
                }
            }

            test("blockers deal damage back to Butcher Orgg normally") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Butcher Orgg")  // 6/6
                    .withCardOnBattlefield(2, "Hill Giant")    // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val orggId = game.findPermanent("Butcher Orgg")!!
                val giantId = game.findPermanent("Hill Giant")!!
                val startingLife = game.getLifeTotal(2)

                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(orggId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(giantId to listOf(orggId)))
                )

                // Advance to combat damage step (where the distribution decision is created)
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Submit damage distribution: 3 to blocker (lethal), 3 to player
                withClue("Should have distribute decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.submitDistribution(mapOf(giantId to 3, game.player2Id to 3))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Hill Giant dies (3 lethal from Orgg)
                withClue("Hill Giant should be dead") {
                    game.findPermanent("Hill Giant") shouldBe null
                }

                // Remaining 3 damage to player (6 - 3 lethal = 3)
                withClue("Defending player should take 3 excess damage") {
                    game.getLifeTotal(2) shouldBe startingLife - 3
                }

                // Butcher Orgg survives with 3 damage from Hill Giant (6 toughness)
                withClue("Butcher Orgg should survive") {
                    game.findPermanent("Butcher Orgg") shouldNotBe null
                }
            }

            test("normal creature blocked by same creature does NOT deal excess to player") {
                // Control test: A normal 6/6 creature (without DivideCombatDamageFreely)
                // blocked by a 2/2 should NOT deal excess damage to the player.
                // This verifies that DivideCombatDamageFreely is the differentiator.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Towering Baloth") // 7/6 (normal creature)
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val balothId = game.findPermanent("Towering Baloth")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val startingLife = game.getLifeTotal(2)

                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(balothId to game.player2Id))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(balothId)))
                )

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Bears should be dead
                withClue("Grizzly Bears should be dead") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }

                // Normal creature without trample: NO excess damage to player
                withClue("Defending player should take NO damage (normal creature, no trample)") {
                    game.getLifeTotal(2) shouldBe startingLife
                }
            }
        }
    }
}
