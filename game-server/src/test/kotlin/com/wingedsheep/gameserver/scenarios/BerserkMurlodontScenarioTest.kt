package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Berserk Murlodont.
 *
 * Berserk Murlodont: {4}{G}
 * Creature — Beast
 * 3/3
 * Whenever a Beast becomes blocked, it gets +1/+1 until end of turn for each creature blocking it.
 */
class BerserkMurlodontScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    // Inline test creatures — avoids depending on specific set cards being registered
    private val vanillaBear = CardDefinition.creature(
        name = "Vanilla Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2
    )
    private val vanillaWarrior = CardDefinition.creature(
        name = "Vanilla Warrior",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Warrior")),
        power = 3, toughness = 3
    )

    init {
        cardRegistry.register(vanillaBear)
        cardRegistry.register(vanillaWarrior)
        context("Berserk Murlodont triggered ability") {

            test("gives itself +1/+1 when blocked by one creature") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Berserk Murlodont")  // 3/3 Beast
                    .withCardOnBattlefield(2, "Vanilla Warrior")          // 3/3 Warrior (non-Beast)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val murlodontId = game.findPermanent("Berserk Murlodont")!!
                val blockerId = game.findPermanent("Vanilla Warrior")!!

                // Declare Murlodont as attacker
                val attackResult = game.execute(
                    DeclareAttackers(game.player1Id, mapOf(murlodontId to game.player2Id))
                )
                withClue("Attack should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with one creature
                val blockResult = game.execute(
                    DeclareBlockers(game.player2Id, mapOf(blockerId to listOf(murlodontId)))
                )
                withClue("Block should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }

                // Trigger fires — pass to resolve it
                game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)

                // After trigger resolves, Murlodont should be 4/4 (3/3 + 1/1 for 1 blocker)
                val projected = projector.project(game.state)
                withClue("Murlodont should be 4/4 after getting +1/+1 for 1 blocker") {
                    projected.getPower(murlodontId) shouldBe 4
                    projected.getToughness(murlodontId) shouldBe 4
                }
            }

            test("gives itself +2/+2 when blocked by two creatures") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Berserk Murlodont")  // 3/3 Beast
                    .withCardOnBattlefield(2, "Vanilla Bear")       // 2/2
                    .withCardOnBattlefield(2, "Vanilla Warrior")          // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val murlodontId = game.findPermanent("Berserk Murlodont")!!
                val bearId = game.findPermanent("Vanilla Bear")!!
                val warriorId = game.findPermanent("Vanilla Warrior")!!

                // Declare Murlodont as attacker
                game.execute(DeclareAttackers(game.player1Id, mapOf(murlodontId to game.player2Id)))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block with two creatures
                game.execute(DeclareBlockers(game.player2Id, mapOf(
                    bearId to listOf(murlodontId),
                    warriorId to listOf(murlodontId)
                )))

                // Multiple blockers require damage assignment order
                val orderDecision = game.state.pendingDecision as OrderObjectsDecision
                game.submitDecision(OrderedResponse(orderDecision.id, listOf(bearId, warriorId)))

                // Trigger fires — pass to resolve
                game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)

                // After trigger resolves, Murlodont should be 5/5 (3/3 + 2/2 for 2 blockers)
                val projected = projector.project(game.state)
                withClue("Murlodont should be 5/5 after getting +2/+2 for 2 blockers") {
                    projected.getPower(murlodontId) shouldBe 5
                    projected.getToughness(murlodontId) shouldBe 5
                }
            }

            test("triggers for other Beasts becoming blocked") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Berserk Murlodont")  // 3/3 Beast (not attacking)
                    .withCardOnBattlefield(1, "Enormous Baloth")    // 7/7 Beast (will attack)
                    .withCardOnBattlefield(2, "Vanilla Warrior")          // 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val balothId = game.findPermanent("Enormous Baloth")!!
                val blockerId = game.findPermanent("Vanilla Warrior")!!

                // Only Baloth attacks
                game.execute(DeclareAttackers(game.player1Id, mapOf(balothId to game.player2Id)))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block the Baloth
                game.execute(DeclareBlockers(game.player2Id, mapOf(blockerId to listOf(balothId))))

                // Trigger fires — pass to resolve
                game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)

                // Baloth should get +1/+1 = 8/8
                val projected = projector.project(game.state)
                withClue("Enormous Baloth should be 8/8 after getting +1/+1 from Murlodont trigger") {
                    projected.getPower(balothId) shouldBe 8
                    projected.getToughness(balothId) shouldBe 8
                }
            }

            test("does not trigger for non-Beast creatures") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Berserk Murlodont")  // 3/3 Beast
                    .withCardOnBattlefield(1, "Vanilla Warrior")        // 3/3 non-Beast
                    .withCardOnBattlefield(2, "Vanilla Bear")       // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val warriorId = game.findPermanent("Vanilla Warrior")!!
                val bearId = game.findPermanent("Vanilla Bear")!!

                // Only Vanilla Warrior attacks (non-Beast)
                game.execute(DeclareAttackers(game.player1Id, mapOf(warriorId to game.player2Id)))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block the non-Beast
                game.execute(DeclareBlockers(game.player2Id, mapOf(bearId to listOf(warriorId))))

                // No trigger should fire — advance past combat damage
                game.passUntilPhase(Phase.COMBAT, Step.FIRST_STRIKE_COMBAT_DAMAGE)

                // Vanilla Warrior should still be 3/3 (no buff)
                val projected = projector.project(game.state)
                withClue("Vanilla Warrior should stay 3/3 (no trigger for non-Beast)") {
                    projected.getPower(warriorId) shouldBe 3
                    projected.getToughness(warriorId) shouldBe 3
                }
            }
        }
    }
}
