package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Protection from creature subtype (Rule 702.16).
 *
 * Protection prevents DEBT:
 * - **D**amage: Damage from sources of the stated quality is prevented
 * - **B**locking: Can't be blocked by creatures of the stated quality
 * - **T**argeting: Can't be targeted by sources of the stated quality
 *
 * Cards used:
 * - Foothill Guide (1/1, {W}, protection from Goblins, morph {W})
 * - Goblin Sledder (1/1, {R}, Goblin — "Sacrifice a Goblin: Target creature gets +1/+1 until end of turn.")
 * - Goblin Sky Raider (1/2, {2}{R}, Goblin Warrior with flying — sacrificed for Sledder's ability)
 * - Grizzly Bears (2/2, green creature — non-Goblin)
 */
class ProtectionFromSubtypeTest : ScenarioTestBase() {

    init {
        context("Protection from Goblins - Blocking (DEBT: B)") {

            test("Goblin creature cannot block creature with protection from Goblins") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Foothill Guide")   // 1/1, pro Goblins
                    .withCardOnBattlefield(2, "Goblin Sledder")   // 1/1 Goblin
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Foothill Guide" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Goblin Sledder" to listOf("Foothill Guide")
                ))
                withClue("Goblin creature should not be able to block creature with protection from Goblins") {
                    blockResult.error shouldNotBe null
                }
            }

            test("non-Goblin creature CAN block creature with protection from Goblins") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Foothill Guide")   // 1/1, pro Goblins
                    .withCardOnBattlefield(2, "Grizzly Bears")    // 2/2 green, non-Goblin
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Foothill Guide" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Foothill Guide")
                ))
                withClue("Non-Goblin creature should be able to block creature with protection from Goblins: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }

        context("Protection from Goblins - Damage (DEBT: D)") {

            test("combat damage from Goblin creature is prevented by protection from Goblins") {
                // Goblin Sledder (1/1 Goblin) attacks, Foothill Guide (1/1, pro Goblins) blocks.
                // Goblin's 1 damage is prevented, Foothill Guide survives.
                // Foothill Guide's 1 damage kills Goblin Sledder.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Sledder")   // 1/1 Goblin
                    .withCardOnBattlefield(2, "Foothill Guide")   // 1/1, pro Goblins
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Sledder" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf(
                    "Foothill Guide" to listOf("Goblin Sledder")
                ))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Foothill Guide should survive — Goblin damage is prevented by protection") {
                    game.isOnBattlefield("Foothill Guide") shouldBe true
                }

                withClue("Goblin Sledder should die — Foothill Guide deals 1 damage, Goblin has 1 toughness") {
                    game.isOnBattlefield("Goblin Sledder") shouldBe false
                }
            }

            test("combat damage from non-Goblin creature is NOT prevented") {
                // Grizzly Bears (2/2 non-Goblin) attacks, Foothill Guide (1/1 pro Goblins) blocks.
                // Green damage is not prevented — Foothill Guide takes 2 damage and dies.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")    // 2/2 green
                    .withCardOnBattlefield(2, "Foothill Guide")   // 1/1, pro Goblins
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf(
                    "Foothill Guide" to listOf("Grizzly Bears")
                ))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Foothill Guide should die from 2 non-Goblin damage (not protected from green)") {
                    game.isOnBattlefield("Foothill Guide") shouldBe false
                }
            }
        }

        context("Protection from Goblins - Targeting (DEBT: T)") {

            test("Goblin source's activated ability cannot target creature with protection from Goblins") {
                // Goblin Sledder has "Sacrifice a Goblin: Target creature gets +1/+1 until end of turn."
                // It should not be able to target Foothill Guide since its source is a Goblin.
                val game = scenario()
                    .withPlayers("GoblinPlayer", "GuidePlayer")
                    .withCardOnBattlefield(1, "Goblin Sledder")     // 1/1 Goblin with activated ability
                    .withCardOnBattlefield(1, "Goblin Sky Raider")  // Goblin to sacrifice
                    .withCardOnBattlefield(2, "Foothill Guide")     // 1/1, pro Goblins
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sledderId = game.findPermanent("Goblin Sledder")!!
                val guideId = game.findPermanent("Foothill Guide")!!

                val ability = cardRegistry.getCard("Goblin Sledder")!!.script.activatedAbilities.first()

                // Attempt to activate Goblin Sledder's ability targeting Foothill Guide
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sledderId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(guideId))
                    )
                )

                withClue("Goblin source should not be able to target creature with protection from Goblins") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
