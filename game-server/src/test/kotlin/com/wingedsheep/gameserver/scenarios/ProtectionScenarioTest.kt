package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Protection from color (Rule 702.16).
 *
 * Protection prevents four interactions, remembered by the mnemonic DEBT:
 * - **D**amage: Damage from sources of the stated quality is prevented
 * - **E**nchant/Equip: Can't be enchanted/equipped by permanents of the stated quality
 * - **B**locking: Can't be blocked by creatures of the stated quality
 * - **T**argeting: Can't be targeted by spells/abilities of the stated quality
 *
 * Cards used:
 * - Disciple of Grace (1/2, {1}{W}, protection from black)
 * - Disciple of Malice (1/2, {1}{B}, protection from white)
 * - Smother ({1}{B}, destroy target creature with CMC ≤ 3) — black removal spell
 * - Angelic Blessing ({2}{W}, target creature gets +3/+3 and flying) — white spell
 * - Muck Rats (1/1, black creature)
 * - Grizzly Bears (2/2, green creature)
 * - Akroma's Blessing ({2}{W}, choose a color, creatures you control gain protection)
 */
class ProtectionScenarioTest : ScenarioTestBase() {

    init {
        context("Protection from color - Targeting (DEBT: T)") {

            test("black spell cannot target creature with protection from black") {
                // Disciple of Grace has protection from black.
                // Smother is a black spell — it should NOT be able to target Disciple of Grace.
                val game = scenario()
                    .withPlayers("Caster", "Protector")
                    .withCardOnBattlefield(2, "Disciple of Grace")  // 1/2, pro black
                    .withCardInHand(1, "Smother")                   // {1}{B} destroy target creature CMC ≤ 3
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val discipleId = game.findPermanent("Disciple of Grace")!!

                // Attempt to cast Smother targeting Disciple of Grace
                val castResult = game.castSpell(1, "Smother", discipleId)

                withClue("Black spell should not be able to target creature with protection from black") {
                    castResult.error shouldNotBe null
                }
            }

            test("non-black spell CAN target creature with protection from black") {
                // Angelic Blessing is a white spell — it should be able to target Disciple of Grace.
                val game = scenario()
                    .withPlayers("Caster", "Protector")
                    .withCardOnBattlefield(2, "Disciple of Grace")
                    .withCardInHand(1, "Angelic Blessing")  // {2}{W} target creature gets +3/+3 and flying
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val discipleId = game.findPermanent("Disciple of Grace")!!

                val castResult = game.castSpell(1, "Angelic Blessing", discipleId)
                withClue("White spell should be able to target creature with protection from black: ${castResult.error}") {
                    castResult.error shouldBe null
                }
            }
        }

        context("Protection from color - Blocking (DEBT: B)") {

            test("creature with protection from black cannot be blocked by black creature") {
                // Disciple of Grace (protection from black) attacks.
                // Muck Rats (black creature) should NOT be able to block it.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Disciple of Grace")  // 1/2, pro black
                    .withCardOnBattlefield(2, "Muck Rats")          // 1/1 black creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack with Disciple of Grace
                game.declareAttackers(mapOf("Disciple of Grace" to 2))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block with Muck Rats (black) — should FAIL
                val blockResult = game.declareBlockers(mapOf(
                    "Muck Rats" to listOf("Disciple of Grace")
                ))
                withClue("Black creature should not be able to block creature with protection from black") {
                    blockResult.error shouldNotBe null
                }
            }

            test("creature with protection from black CAN be blocked by non-black creature") {
                // Disciple of Grace attacks. Grizzly Bears (green) should be able to block.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Disciple of Grace")  // 1/2, pro black
                    .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2 green creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Disciple of Grace" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Grizzly Bears (green) — should succeed
                val blockResult = game.declareBlockers(mapOf(
                    "Grizzly Bears" to listOf("Disciple of Grace")
                ))
                withClue("Non-black creature should be able to block creature with protection from black: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }

        context("Protection from color - Damage (DEBT: D)") {

            test("combat damage from black creature is prevented by protection from black") {
                // Disciple of Grace (1/2, pro black) blocks Python (3/2 black).
                // Without protection, 3 damage would kill Disciple of Grace.
                // With protection, black combat damage is prevented and Disciple survives.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Python")             // 3/2 black
                    .withCardOnBattlefield(2, "Disciple of Grace")  // 1/2, pro black
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Python" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Disciple of Grace blocks Python
                game.declareBlockers(mapOf(
                    "Disciple of Grace" to listOf("Python")
                ))

                // Resolve combat damage
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Disciple of Grace should survive — 3 black damage is prevented by protection") {
                    game.isOnBattlefield("Disciple of Grace") shouldBe true
                }

                withClue("Python should survive — Disciple of Grace deals 1 damage, Python has 2 toughness") {
                    game.isOnBattlefield("Python") shouldBe true
                }
            }

            test("combat damage from non-black creature is NOT prevented") {
                // Disciple of Grace (1/2) blocks Grizzly Bears (2/2 green).
                // Green damage is not prevented by protection from black.
                // Both should deal damage normally.
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Grizzly Bears")      // 2/2 green
                    .withCardOnBattlefield(2, "Disciple of Grace")  // 1/2 pro black
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf(
                    "Disciple of Grace" to listOf("Grizzly Bears")
                ))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Disciple of Grace should die from 2 green damage (not protected from green)") {
                    game.isOnBattlefield("Disciple of Grace") shouldBe false
                }
            }
        }

        context("Akroma's Blessing - dynamic protection") {

            test("grants protection from chosen color to creatures you control") {
                // Cast Akroma's Blessing, choose black.
                // Then a black spell should not be able to target the protected creature.
                val game = scenario()
                    .withPlayers("Protector", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")      // Will gain protection
                    .withCardInHand(1, "Akroma's Blessing")         // {2}{W} choose color
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInHand(2, "Smother")                   // {1}{B} black removal
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Akroma's Blessing
                val castResult = game.castSpell(1, "Akroma's Blessing")
                withClue("Akroma's Blessing should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve — should prompt for color choice
                game.resolveStack()

                withClue("Should have pending color choice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose black
                val decisionId = game.getPendingDecision()!!.id
                game.submitDecision(ColorChosenResponse(decisionId, Color.BLACK))

                // Grizzly Bears now has protection from black until end of turn.
                // Pass priority to opponent.
                game.passPriority()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Opponent tries to Smother the Grizzly Bears — should fail
                val smotherResult = game.castSpell(2, "Smother", bearsId)
                withClue("Black spell should not target creature with dynamic protection from black") {
                    smotherResult.error shouldNotBe null
                }
            }
        }

        context("Protection from white - symmetric test") {

            test("white spell cannot target creature with protection from white") {
                // Disciple of Malice has protection from white.
                // Angelic Blessing (white) should NOT be able to target it.
                val game = scenario()
                    .withPlayers("Caster", "Protector")
                    .withCardOnBattlefield(2, "Disciple of Malice") // 1/2, pro white
                    .withCardInHand(1, "Angelic Blessing")          // {2}{W} white spell
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val discipleId = game.findPermanent("Disciple of Malice")!!

                val castResult = game.castSpell(1, "Angelic Blessing", discipleId)
                withClue("White spell should not be able to target creature with protection from white") {
                    castResult.error shouldNotBe null
                }
            }
        }
    }
}
