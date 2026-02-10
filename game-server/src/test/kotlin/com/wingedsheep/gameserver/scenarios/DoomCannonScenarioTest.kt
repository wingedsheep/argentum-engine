package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Doom Cannon.
 *
 * Card reference:
 * - Doom Cannon ({6}): Artifact
 *   "As Doom Cannon enters the battlefield, choose a creature type.
 *    {3}, {T}, Sacrifice a creature of the chosen type: Doom Cannon deals 3 damage to any target."
 */
class DoomCannonScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Doom Cannon - as enters, choose creature type") {

            test("choosing a creature type on entry and dealing 3 damage to opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Doom Cannon")
                    .withCardOnBattlefield(1, "Goblin Sky Raider")  // 1/2 Goblin
                    .withLandsOnBattlefield(1, "Mountain", 9)  // 6 for cannon + 3 for activation
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.getLifeTotal(2) shouldBe 20

                // Cast Doom Cannon
                val castResult = game.castSpell(1, "Doom Cannon")
                withClue("Doom Cannon should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell - should pause for creature type choice
                game.resolveStack()

                withClue("Should have pending creature type decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose "Goblin"
                game.chooseCreatureType("Goblin")

                // Doom Cannon should now be on the battlefield
                withClue("Doom Cannon should be on battlefield") {
                    game.isOnBattlefield("Doom Cannon") shouldBe true
                }

                // Now activate: {3}, {T}, Sacrifice Goblin Sky Raider -> deal 3 to opponent
                val cannonId = game.findPermanent("Doom Cannon")!!
                val goblId = game.findPermanent("Goblin Sky Raider")!!
                val cardDef = cardRegistry.getCard("Doom Cannon")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cannonId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(goblId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Goblin should be sacrificed
                withClue("Goblin Sky Raider should be in graveyard") {
                    game.isInGraveyard(1, "Goblin Sky Raider") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Opponent should take 3 damage
                withClue("Opponent should take 3 damage") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("cannot sacrifice a creature of the wrong type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doom Cannon")
                    .withCardOnBattlefield(1, "Elvish Warrior")  // 2/3 Elf Warrior
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // The cannon is on the battlefield but needs a creature type choice.
                // Since we built it directly, we need to set it up through casting.
                // Let's use a different approach: cast it from hand.
                val game2 = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Doom Cannon")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and choose Goblin as the type
                game2.castSpell(1, "Doom Cannon")
                game2.resolveStack()
                game2.chooseCreatureType("Goblin")

                val cannonId = game2.findPermanent("Doom Cannon")!!
                val elfId = game2.findPermanent("Elvish Warrior")!!
                val cardDef = cardRegistry.getCard("Doom Cannon")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to sacrifice Elvish Warrior (Elf, not Goblin) - should fail
                val activateResult = game2.execute(
                    ActivateAbility(
                        playerId = game2.player1Id,
                        sourceId = cannonId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game2.player2Id)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(elfId)
                        )
                    )
                )

                withClue("Sacrificing wrong creature type should fail") {
                    activateResult.error shouldNotBe null
                }
            }

            test("deals 3 damage to a creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Doom Cannon")
                    .withCardOnBattlefield(1, "Goblin Sky Raider")
                    .withCardOnBattlefield(2, "Snapping Thragg")  // 3/3 Beast
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Doom Cannon and choose Goblin
                game.castSpell(1, "Doom Cannon")
                game.resolveStack()
                game.chooseCreatureType("Goblin")

                val cannonId = game.findPermanent("Doom Cannon")!!
                val goblId = game.findPermanent("Goblin Sky Raider")!!
                val thraggId = game.findPermanent("Snapping Thragg")!!
                val cardDef = cardRegistry.getCard("Doom Cannon")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate: deal 3 damage to Snapping Thragg (3/3) - should kill it
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cannonId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(thraggId)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(goblId)
                        )
                    )
                )

                withClue("Ability should activate: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Snapping Thragg should be dead (3 damage to 3 toughness)") {
                    game.isOnBattlefield("Snapping Thragg") shouldBe false
                }
            }
        }
    }
}
