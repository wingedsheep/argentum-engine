package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Tephraderm.
 *
 * Card reference:
 * - Tephraderm ({4}{R}): Creature — Beast 4/5
 *   Whenever a creature deals damage to Tephraderm, Tephraderm deals that much damage to that creature.
 *   Whenever a spell deals damage to Tephraderm, Tephraderm deals that much damage to that spell's controller.
 */
class TephradermScenarioTest : ScenarioTestBase() {

    init {
        context("Tephraderm - creature damage retaliation") {

            test("Tephraderm deals damage back to a creature that dealt combat damage to it") {
                // Crude Rampart is a 4/5 Wall with Defender - can block but not attack.
                // Combat damage alone (4 from Tephraderm) doesn't kill it (4 < 5).
                // But the trigger adds 4 more damage (8 total > 5 toughness) → it dies.
                val game = scenario()
                    .withPlayers("Tephraderm Player", "Blocker")
                    .withCardOnBattlefield(1, "Tephraderm")
                    .withCardOnBattlefield(2, "Crude Rampart") // 4/5 Wall Defender
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tephraderm = game.findPermanent("Tephraderm")!!
                val crudeRampart = game.findPermanent("Crude Rampart")!!

                // Attack with Tephraderm
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Tephraderm" to 2))

                // Block with Crude Rampart
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Crude Rampart" to listOf("Tephraderm")))

                // Advance through combat damage and trigger resolution
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Tephraderm took 4 damage but survived (4/5)
                withClue("Tephraderm should still be on the battlefield") {
                    game.findPermanent("Tephraderm") shouldNotBe null
                }
                withClue("Tephraderm should have 4 damage") {
                    game.state.getEntity(tephraderm)?.get<DamageComponent>()?.amount shouldBe 4
                }

                // Crude Rampart should be dead: 4 combat + 4 trigger = 8 damage > 5 toughness
                withClue("Crude Rampart should be dead (in graveyard)") {
                    game.findPermanent("Crude Rampart") shouldBe null
                    game.findCardsInGraveyard(2, "Crude Rampart").size shouldBe 1
                }

                // No player damage (all creature-to-creature)
                withClue("Life totals should be unchanged") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("Tephraderm trigger fires and deals damage back even when attacker dies from combat") {
                // Glory Seeker (2/2) attacks, Tephraderm blocks.
                // Tephraderm's 4 power kills the 2/2 in combat (2 toughness).
                // The DamagedByCreature trigger should still fire (Rule 603.10 look-back)
                // and deal 2 damage back to the now-dead Glory Seeker (harmless no-op).
                // No infinite loop and no damage to Tephraderm's controller.
                val game = scenario()
                    .withPlayers("Blocker", "Attacker")
                    .withCardOnBattlefield(1, "Tephraderm")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tephraderm = game.findPermanent("Tephraderm")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Tephraderm" to listOf("Glory Seeker")))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Tephraderm took 2 damage (survives, 4/5)
                withClue("Tephraderm should have 2 damage") {
                    game.state.getEntity(tephraderm)?.get<DamageComponent>()?.amount shouldBe 2
                }

                // Glory Seeker should be dead (4 combat damage > 2 toughness)
                withClue("Glory Seeker should be in graveyard") {
                    game.findCardsInGraveyard(2, "Glory Seeker").size shouldBe 1
                }

                // No player damage — no infinite loop
                withClue("Life totals should be unchanged") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("no infinite loop when both Tephraderm and attacker die from mutual combat damage") {
                // Hulking Cyclops (5/4) attacks, Tephraderm (4/5) blocks.
                // Cyclops deals 5 to Tephraderm (lethal: 5 == toughness 5).
                // Tephraderm deals 4 to Cyclops (lethal: 4 == toughness 4).
                // Both die from SBAs. The trigger fires but targets the dead Cyclops (harmless).
                // Previously caused an infinite loop: trigger used wrong triggeringEntityId
                // (event.targetId = Tephraderm itself), making Tephraderm re-trigger forever.
                val game = scenario()
                    .withPlayers("Blocker", "Attacker")
                    .withCardOnBattlefield(1, "Tephraderm")
                    .withCardOnBattlefield(2, "Hulking Cyclops") // 5/4
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hulking Cyclops" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Tephraderm" to listOf("Hulking Cyclops")))
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Both creatures should be dead
                withClue("Tephraderm should be in graveyard") {
                    game.findPermanent("Tephraderm") shouldBe null
                    game.findCardsInGraveyard(1, "Tephraderm").size shouldBe 1
                }
                withClue("Hulking Cyclops should be in graveyard") {
                    game.findPermanent("Hulking Cyclops") shouldBe null
                    game.findCardsInGraveyard(2, "Hulking Cyclops").size shouldBe 1
                }

                // No player should have taken damage — no infinite loop
                withClue("Life totals should be unchanged (no self-damage loop)") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }

        context("Tephraderm - spell damage retaliation") {

            test("Tephraderm deals damage to spell controller when a spell deals damage to it") {
                val game = scenario()
                    .withPlayers("Tephraderm Player", "Spell Caster")
                    .withCardOnBattlefield(1, "Tephraderm")
                    .withCardInHand(2, "Shock") // Instant: deals 2 damage to any target
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tephraderm = game.findPermanent("Tephraderm")!!

                // Verify starting life totals
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20

                // Player 2 casts Shock targeting Tephraderm
                game.castSpell(2, "Shock", tephraderm)

                // Shock resolves, trigger fires and resolves
                game.resolveStack()

                // Tephraderm took 2 damage from Shock
                withClue("Tephraderm should have 2 damage from Shock") {
                    game.state.getEntity(tephraderm)?.get<DamageComponent>()?.amount shouldBe 2
                }

                // Player 2 (spell caster) should have lost 2 life from Tephraderm's trigger
                withClue("Spell caster should have lost 2 life from Tephraderm's retaliation") {
                    game.getLifeTotal(2) shouldBe 18
                }

                // Player 1 (Tephraderm controller) should be unaffected
                withClue("Tephraderm player's life should be unchanged") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
