package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Scenario tests for Ironfist Crusher (ONS #42).
 *
 * Ironfist Crusher {4}{W}
 * Creature — Human Soldier
 * 2/4
 * Ironfist Crusher can block any number of creatures.
 * Morph {3}{W}
 */
class IronfistCrusherScenarioTest : ScenarioTestBase() {

    init {
        context("Can block any number of creatures") {
            test("Ironfist Crusher can block two attackers at once") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")   // 2/2 attacker 1
                    .withCardOnBattlefield(1, "Devoted Hero")    // 1/1 attacker 2 (using a small creature, actually Devoted Hero is 1/2)
                    .withCardOnBattlefield(2, "Ironfist Crusher") // 2/4 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val heroId = game.findPermanent("Devoted Hero")!!
                val crusherId = game.findPermanent("Ironfist Crusher")!!

                // Declare both creatures as attackers
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        bearsId to game.player2Id,
                        heroId to game.player2Id
                    ))
                )
                withClue("Attackers should be declared successfully: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Ironfist Crusher blocks BOTH attackers
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(crusherId to listOf(bearsId, heroId))
                    )
                )

                withClue("Ironfist Crusher should be able to block both attackers: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Verify Ironfist Crusher has BlockingComponent with both attackers
                val blockerEntity = game.state.getEntity(crusherId)
                withClue("Ironfist Crusher should have BlockingComponent") {
                    blockerEntity shouldNotBe null
                    val blockingComponent = blockerEntity!!.get<BlockingComponent>()
                    blockingComponent shouldNotBe null
                    blockingComponent!!.blockedAttackerIds.size shouldBe 2
                }
            }

            test("Ironfist Crusher blocks three attackers and takes combined damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")   // 2/2
                    .withCardOnBattlefield(1, "Devoted Hero")    // 1/2
                    .withCardOnBattlefield(1, "Glory Seeker")    // 2/2
                    .withCardOnBattlefield(2, "Ironfist Crusher") // 2/4
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val heroId = game.findPermanent("Devoted Hero")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val crusherId = game.findPermanent("Ironfist Crusher")!!

                // Declare all three as attackers
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        bearsId to game.player2Id,
                        heroId to game.player2Id,
                        seekerId to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Ironfist Crusher blocks all three
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(crusherId to listOf(bearsId, heroId, seekerId))
                    )
                )

                withClue("Should block all three: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Advance through combat damage
                // The crusher takes 2+1+2 = 5 damage total, which exceeds its 4 toughness
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Ironfist Crusher should be dead (took 5 damage with only 4 toughness)
                withClue("Ironfist Crusher should be dead from 5 combined damage") {
                    game.findPermanent("Ironfist Crusher") shouldBe null
                }

                // No damage should have gone through to the defending player
                withClue("Defender should still be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("two Crushers each blocking both Silvos and Spurred Wolverine: both Crushers die, no trample through") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Silvos, Rogue Elemental")  // 8/5 trample
                    .withCardOnBattlefield(1, "Spurred Wolverine")        // 3/2
                    .withCardOnBattlefield(2, "Ironfist Crusher")         // 2/4 can-block-any-number
                    .withCardOnBattlefield(2, "Ironfist Crusher")         // 2/4 can-block-any-number
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val silvosId = game.findPermanent("Silvos, Rogue Elemental")!!
                val wolverineId = game.findPermanent("Spurred Wolverine")!!
                val crushers = game.findAllPermanents("Ironfist Crusher")
                crushers.size shouldBe 2

                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        silvosId to game.player2Id,
                        wolverineId to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Both Crushers each block BOTH attackers (8 blocker-attacker edges total).
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(
                            crushers[0] to listOf(silvosId, wolverineId),
                            crushers[1] to listOf(silvosId, wolverineId)
                        )
                    )
                )
                withClue("Both Crushers should be allowed to block both attackers: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Verify each Crusher records both attackers in its BlockingComponent.
                crushers.forEach { crusherId ->
                    val blocking = game.state.getEntity(crusherId)!!.get<BlockingComponent>()
                    withClue("Crusher should block both attackers") {
                        blocking shouldNotBe null
                        blocking!!.blockedAttackerIds.size shouldBe 2
                    }
                }

                // Resolve combat (default damage assignments).
                // Silvos (8 power, trample): each Crusher has 4 toughness, lethal to each = 4.
                //   Two blockers × 4 lethal = 8, consuming all of Silvos's power → 0 trample.
                // Wolverine (3 power, no trample): can only assign to first blocker in damage order
                //   (3 < 4 lethal), so all 3 land on a single Crusher.
                // → One Crusher takes 4+3=7, the other takes 4. Both die. Defender takes 0.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Both Ironfist Crushers should die (each took at least 4 damage)") {
                    game.findAllPermanents("Ironfist Crusher").shouldBe(emptyList())
                }
                withClue("Defender takes 0 damage: Silvos's 8 power was fully absorbed by the two 4-toughness blockers") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("blocking multiple attackers prevents all damage to defending player") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")    // 1/2
                    .withCardOnBattlefield(1, "Devoted Hero")    // 1/2
                    .withCardOnBattlefield(2, "Ironfist Crusher") // 2/4
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroes = game.findAllPermanents("Devoted Hero")
                val crusherId = game.findPermanent("Ironfist Crusher")!!

                // Both Devoted Heroes attack
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        heroes[0] to game.player2Id,
                        heroes[1] to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Ironfist Crusher blocks both
                game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(crusherId to listOf(heroes[0], heroes[1]))
                    )
                )

                // Advance through combat
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Ironfist Crusher survives (1+1 = 2 damage < 4 toughness)
                withClue("Ironfist Crusher should survive blocking two 1-power creatures") {
                    game.findPermanent("Ironfist Crusher") shouldNotBe null
                }

                // No damage to defending player
                withClue("Defender should still be at 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }

        context("Normal creatures cannot block multiple attackers") {
            test("Grizzly Bears cannot block two attackers") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")    // attacker 1
                    .withCardOnBattlefield(1, "Glory Seeker")    // attacker 2
                    .withCardOnBattlefield(2, "Grizzly Bears")   // blocker (no CanBlockAnyNumber)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare both creatures as attackers
                game.execute(
                    DeclareAttackers(game.player1Id, mapOf(
                        heroId to game.player2Id,
                        seekerId to game.player2Id
                    ))
                )

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Grizzly Bears tries to block both attackers - should fail
                val blockResult = game.execute(
                    DeclareBlockers(
                        game.player2Id,
                        mapOf(bearsId to listOf(heroId, seekerId))
                    )
                )

                withClue("Normal creature should not be able to block multiple attackers") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "can only block one creature"
                }
            }
        }

        context("Morph") {
            test("Ironfist Crusher can be cast face down as a 2/2 for {3}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ironfist Crusher")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Ironfist Crusher").first()

                // Cast face-down
                val castResult = game.execute(
                    CastSpell(game.player1Id, cardId, castFaceDown = true)
                )

                withClue("Should be able to cast face-down: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Should be on the battlefield as a face-down creature
                val faceDownCreatures = game.state.getBattlefield().filter { entityId ->
                    game.state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.FaceDownComponent>() != null
                }
                withClue("Should have a face-down creature on the battlefield") {
                    faceDownCreatures.size shouldBe 1
                }
            }
        }
    }
}
