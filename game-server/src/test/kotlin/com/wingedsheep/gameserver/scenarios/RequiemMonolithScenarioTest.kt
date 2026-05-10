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
 * Scenario tests for Requiem Monolith.
 *
 * Card reference:
 * - Requiem Monolith ({2}{B}): Artifact
 *   "{T}: Until end of turn, target creature gains 'Whenever this creature is dealt damage,
 *    you draw that many cards and lose that much life.' That creature's controller may have
 *    this artifact deal 1 damage to it. Activate only as a sorcery."
 *
 * Exercises the new `MayEffect.decisionMaker` field — the may decision is delegated to the
 * targeted creature's controller rather than the activator.
 */
class RequiemMonolithScenarioTest : ScenarioTestBase() {

    init {
        context("Requiem Monolith granted trigger + delegated may") {

            test("targeting own creature, accept may: draw 1, lose 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Requiem Monolith")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHand = game.handSize(1)
                val initialLife = game.getLifeTotal(1)

                val monolithId = game.findPermanent("Requiem Monolith")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val ability = cardRegistry.getCard("Requiem Monolith")!!.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monolithId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(seekerId))
                    )
                )
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }

                game.resolveStack()

                withClue("May decision should be pending") { game.hasPendingDecision() shouldBe true }
                withClue("Player1 controls Glory Seeker, so player1 decides") {
                    game.state.pendingDecision?.playerId shouldBe game.player1Id
                }

                game.answerYesNo(true)
                game.resolveStack()

                withClue("Player1 draws 1 from granted trigger") { game.handSize(1) shouldBe initialHand + 1 }
                withClue("Player1 loses 1 life from granted trigger") { game.getLifeTotal(1) shouldBe initialLife - 1 }
                withClue("Glory Seeker survives 1 damage") { game.isOnBattlefield("Glory Seeker") shouldBe true }
            }

            test("targeting own creature, decline may: no damage, no draw, no life loss") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Requiem Monolith")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHand = game.handSize(1)
                val initialLife = game.getLifeTotal(1)

                val monolithId = game.findPermanent("Requiem Monolith")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val ability = cardRegistry.getCard("Requiem Monolith")!!.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monolithId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(seekerId))
                    )
                )
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                game.handSize(1) shouldBe initialHand
                game.getLifeTotal(1) shouldBe initialLife
                game.isOnBattlefield("Glory Seeker") shouldBe true
            }

            test("targeting opponent's creature: opponent makes the may decision") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Requiem Monolith")
                    .withCardOnBattlefield(2, "Glory Seeker") // opponent controls
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p2InitialHand = game.handSize(2)
                val p2InitialLife = game.getLifeTotal(2)

                val monolithId = game.findPermanent("Requiem Monolith")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val ability = cardRegistry.getCard("Requiem Monolith")!!.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monolithId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(seekerId))
                    )
                )
                game.resolveStack()

                withClue("Opponent (target's controller) is asked the may, not the activator") {
                    game.state.pendingDecision?.playerId shouldBe game.player2Id
                }

                // Opponent accepts to demonstrate the granted trigger reaches them.
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Opponent draws 1 from the granted trigger they now control") {
                    game.handSize(2) shouldBe p2InitialHand + 1
                }
                withClue("Opponent loses 1 life") { game.getLifeTotal(2) shouldBe p2InitialLife - 1 }
            }

            test("cannot activate at instant speed (during opponent's turn)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Requiem Monolith")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2) // Player 2's turn — Player 1 cannot activate at sorcery speed
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monolithId = game.findPermanent("Requiem Monolith")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val ability = cardRegistry.getCard("Requiem Monolith")!!.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monolithId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(seekerId))
                    )
                )

                withClue("Activation should be rejected at instant speed") {
                    result.error shouldNotBe null
                }
            }

            test("granted trigger fires when an opposing spell deals damage to the granted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Requiem Monolith")
                    .withCardOnBattlefield(1, "Towering Baloth") // 7/6 — easily survives 2 damage
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monolithId = game.findPermanent("Requiem Monolith")!!
                val balothId = game.findPermanent("Towering Baloth")!!
                val ability = cardRegistry.getCard("Requiem Monolith")!!.script.activatedAbilities.first()

                // Player 1 grants the trigger to their Baloth, declines the self-ping.
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monolithId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(balothId))
                    )
                )
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                val handAfterGrant = game.handSize(1)
                val lifeAfterGrant = game.getLifeTotal(1)

                // Pass priority to P2 so they can cast Shock at instant speed.
                game.passPriority()

                // Player 2 casts Shock targeting the Baloth while it still carries the granted trigger.
                val shockResult = game.castSpell(2, "Shock", balothId)
                withClue("Player 2 should be able to cast Shock: ${shockResult.error}") {
                    shockResult.error shouldBe null
                }
                game.resolveStack() // Shock resolves → 2 damage → granted trigger fires → resolve

                withClue("Granted trigger fires from a different damage source (Shock, not the artifact)") {
                    game.handSize(1) shouldBe handAfterGrant + 2
                }
                withClue("Player1 loses 2 life — equal to damage taken") {
                    game.getLifeTotal(1) shouldBe lifeAfterGrant - 2
                }
                withClue("Towering Baloth (7/6) survives 2 damage from Shock") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }
            }

            // Multi-blocker combat: cumulative observable totals match the Scryfall ruling
            // (2025-07-25, "the creature's controller draws cards and loses life equal to the
            // total amount of damage dealt"). The mechanism differs from the ruling though:
            // the engine emits a separate DamageReceivedEvent per source, so the trigger fires
            // N times with per-source amounts instead of once with the aggregated total. The
            // sum is the same for Requiem Monolith, but the trigger *count* differs — which
            // matters for counter-the-trigger interactions and replacement effects on draws.
            // This test pins the totals; the trigger-count semantics are an engine-wide gap
            // (also affects Thrashing Mudspawn et al.) that needs DamageReceivedEvent
            // aggregation per target before the ruling is faithfully implemented.
            test("multi-blocker combat: cumulative draws + life loss match total damage taken") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Requiem Monolith")
                    .withCardOnBattlefield(1, "Towering Baloth") // 7/6 attacker
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 blocker A
                    .withCardOnBattlefield(2, "Devoted Hero") // 1/2 blocker B
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val monolithId = game.findPermanent("Requiem Monolith")!!
                val balothId = game.findPermanent("Towering Baloth")!!
                val ability = cardRegistry.getCard("Requiem Monolith")!!.script.activatedAbilities.first()

                // Grant the trigger to our attacker; decline the self-ping.
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = monolithId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(balothId))
                    )
                )
                game.resolveStack()
                game.answerYesNo(false)
                game.resolveStack()

                val handAfterGrant = game.handSize(1)
                val lifeAfterGrant = game.getLifeTotal(1)

                // Combat: Baloth attacks, both opposing creatures block — total 2 + 1 = 3 damage to Baloth.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Towering Baloth" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Towering Baloth"), "Devoted Hero" to listOf("Towering Baloth")))

                // Drive priority through combat damage and let the granted trigger(s) resolve.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Net cards drawn equals total combat damage to Baloth (2 + 1 = 3)") {
                    game.handSize(1) shouldBe handAfterGrant + 3
                }
                withClue("Net life lost equals total combat damage to Baloth (2 + 1 = 3)") {
                    game.getLifeTotal(1) shouldBe lifeAfterGrant - 3
                }
            }
        }
    }
}
