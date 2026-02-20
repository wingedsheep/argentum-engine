package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Scenario tests for Fleet-Footed Monk's blocking restriction.
 *
 * Fleet-Footed Monk: {1}{W} 1/1 Creature - Human Monk
 * "Fleet-Footed Monk can't be blocked by creatures with power 2 or greater."
 *
 * These tests verify:
 * 1. Creatures with power >= 2 cannot block Fleet-Footed Monk
 * 2. Creatures with power < 2 CAN block Fleet-Footed Monk
 * 3. The check uses PROJECTED power (includes buffs from spells like Giant Growth)
 */
class FleetFootedMonkScenarioTest : ScenarioTestBase() {

    /**
     * Test instant: +2/+2 until end of turn.
     * Used to test that power-based blocking restrictions use projected power.
     */
    private val naturesBlessing = CardDefinition.instant(
        name = "Nature's Blessing",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Target creature gets +2/+2 until end of turn.",
        script = CardScript.spell(
            effect = ModifyStatsEffect(2, 2, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            TargetCreature()
        )
    )

    init {
        // Register test card
        cardRegistry.register(listOf(naturesBlessing))

        context("Fleet-Footed Monk blocking restriction") {

            test("cannot be blocked by creature with power 2") {
                // Setup:
                // - Player 1 has Fleet-Footed Monk (1/1) that will attack
                // - Player 2 has Grizzly Bears (2/2) as potential blocker
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Fleet-Footed Monk")  // 1/1 attacker
                    .withCardOnBattlefield(2, "Grizzly Bears")      // 2/2 blocker (power >= 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val monkId = game.findPermanent("Fleet-Footed Monk")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare Fleet-Footed Monk as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(monkId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block with Grizzly Bears - should FAIL (power 2 >= 2)
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(monkId)))
                )

                withClue("Block should fail due to power restriction") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "cannot block"
                    blockResult.error!! shouldContainIgnoringCase "power"
                }
            }

            test("CAN be blocked by creature with power 1") {
                // Setup:
                // - Player 1 has Fleet-Footed Monk (1/1) that will attack
                // - Player 2 has Devoted Hero (2/1) as potential blocker (power 1 < 2)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Fleet-Footed Monk")  // 1/1 attacker
                    .withCardOnBattlefield(2, "Devoted Hero")       // 2/1 blocker (power 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val monkId = game.findPermanent("Fleet-Footed Monk")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Declare Fleet-Footed Monk as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(monkId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Devoted Hero - should SUCCEED (power 1 < 2)
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(heroId to listOf(monkId)))
                )

                withClue("Block should succeed (power 1 < 2): ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("cannot be blocked by creature with power 3") {
                // Setup:
                // - Player 1 has Fleet-Footed Monk (1/1) that will attack
                // - Player 2 has Hill Giant (3/3) as potential blocker (power 3 >= 2)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Fleet-Footed Monk")  // 1/1 attacker
                    .withCardOnBattlefield(2, "Hill Giant")         // 3/3 blocker (power >= 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val monkId = game.findPermanent("Fleet-Footed Monk")!!
                val giantId = game.findPermanent("Hill Giant")!!

                // Declare Fleet-Footed Monk as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(monkId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Try to block with Hill Giant - should FAIL (power 3 >= 2)
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(giantId to listOf(monkId)))
                )

                withClue("Block should fail due to power restriction") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "cannot block"
                }
            }

            test("cannot be blocked by creature whose power was increased by a pump spell") {
                // Setup:
                // - Player 1 has Fleet-Footed Monk (1/1) that will attack
                // - Player 2 has Devoted Hero (1/1), and Nature's Blessing (+2/+2 instant) to buff it
                // - After Nature's Blessing, Devoted Hero becomes 3/3 (power 3 >= 2)
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Fleet-Footed Monk")  // 1/1 attacker
                    .withCardOnBattlefield(2, "Devoted Hero")       // 1/1 blocker initially
                    .withCardInHand(2, "Nature's Blessing")         // +2/+2 instant
                    .withLandsOnBattlefield(2, "Forest", 1)         // Mana to cast Nature's Blessing ({G})
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val monkId = game.findPermanent("Fleet-Footed Monk")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Declare Fleet-Footed Monk as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(monkId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Player 2 casts Nature's Blessing on Devoted Hero (+2/+2 makes it 3/3)
                val naturesBlessingId = game.findCardsInHand(2, "Nature's Blessing").first()
                val castResult = game.execute(
                    CastSpell(
                        playerId = game.player2Id,
                        cardId = naturesBlessingId,
                        targets = listOf(ChosenTarget.Permanent(heroId)),
                        paymentStrategy = PaymentStrategy.AutoPay
                    )
                )
                withClue("Cast Nature's Blessing should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Both players pass to resolve Nature's Blessing
                game.execute(PassPriority(game.player2Id))
                game.execute(PassPriority(game.player1Id))

                // Advance to declare blockers by passing priority (through normal game flow)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Should be in declare blockers step") {
                    game.state.step shouldBe Step.DECLARE_BLOCKERS
                }

                // Try to block with Devoted Hero (now 3/3) - should FAIL
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(heroId to listOf(monkId)))
                )

                withClue("Block should fail - Devoted Hero is now 3/3 (power >= 2)") {
                    blockResult.error shouldNotBe null
                    blockResult.error!! shouldContainIgnoringCase "cannot block"
                    blockResult.error!! shouldContainIgnoringCase "power"
                }
            }

            test("normal creature can be blocked by any power creature") {
                // Verify that a creature WITHOUT the restriction can be blocked normally
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Devoted Hero")   // 1/1 attacker (no restriction)
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 blocker
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Declare Devoted Hero as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(heroId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers by passing priority
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with Grizzly Bears - should SUCCEED (normal creature)
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(bearsId to listOf(heroId)))
                )

                withClue("Block should succeed for normal creature: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }
        }
    }
}
