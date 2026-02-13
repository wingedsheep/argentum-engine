package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test: Imagecrafter denies Boneknitter's regeneration by changing
 * the target's creature type in response.
 *
 * Tests Rule 608.2b — targets must still be legal when a spell or ability resolves.
 * Boneknitter's "Regenerate target Zombie" should fizzle when the target is no longer
 * a Zombie (changed by Imagecrafter).
 */
class ImagecrafterBoneknitterScenarioTest : ScenarioTestBase() {

    /**
     * Choose a creature type from a ChooseOptionDecision.
     */
    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = state.pendingDecision
            ?: error("Expected a pending decision for creature type selection")
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Imagecrafter + Boneknitter type-change interaction") {

            test("changing creature type causes regenerate ability to fizzle") {
                // P1 has Imagecrafter, P2 has Boneknitter + mana for regen
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Imagecrafter")
                    .withCardOnBattlefield(2, "Boneknitter")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val imagecrafterId = game.findPermanent("Imagecrafter")!!
                val boneknitterId = game.findPermanent("Boneknitter")!!

                val boneknitterDef = cardRegistry.getCard("Boneknitter")!!
                val regenAbility = boneknitterDef.script.activatedAbilities.first()

                val imagecrafterDef = cardRegistry.getCard("Imagecrafter")!!
                val typeChangeAbility = imagecrafterDef.script.activatedAbilities.first()

                // P1 passes priority to P2
                game.passPriority()

                // P2 activates Boneknitter's regen targeting itself
                val regenResult = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = boneknitterId,
                        abilityId = regenAbility.id,
                        targets = listOf(ChosenTarget.Permanent(boneknitterId))
                    )
                )
                withClue("Regen activation should succeed: ${regenResult.error}") {
                    regenResult.error shouldBe null
                }

                // P2 passes priority after activating → P1 gets priority to respond
                game.passPriority()

                // P1 responds with Imagecrafter targeting Boneknitter
                val typeChangeResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = imagecrafterId,
                        abilityId = typeChangeAbility.id,
                        targets = listOf(ChosenTarget.Permanent(boneknitterId))
                    )
                )
                withClue("Type change activation should succeed: ${typeChangeResult.error}") {
                    typeChangeResult.error shouldBe null
                }

                // Both pass to resolve top of stack (Imagecrafter's ability, LIFO)
                game.passPriority() // P1 passes
                game.passPriority() // P2 passes → stack resolves

                // Imagecrafter's ability pauses for creature type choice
                withClue("Should have a pending decision for creature type selection") {
                    (game.state.pendingDecision is ChooseOptionDecision) shouldBe true
                }

                // P1 chooses "Goblin" — Boneknitter is now a Goblin, not a Zombie
                game.chooseCreatureType("Goblin")

                // Stack still has Boneknitter's regen. Both pass to resolve it.
                game.resolveStack()

                // Boneknitter's regen should have fizzled — target is no longer a Zombie
                // Verify: no regeneration shield was created (type-change floating effect still exists)
                val hasRegenShield = game.state.floatingEffects.any {
                    it.effect.modification is SerializableModification.RegenerationShield
                }
                withClue("Should have no regeneration shield (regen fizzled)") {
                    hasRegenShield shouldBe false
                }

                // Boneknitter should still be alive (nothing destroyed it yet)
                withClue("Boneknitter should still be on the battlefield") {
                    game.isOnBattlefield("Boneknitter") shouldBe true
                }
            }

            test("regenerate resolves normally when creature type is not changed") {
                // Control test: without Imagecrafter interference, regen works
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boneknitter")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!

                val cardDef = cardRegistry.getCard("Boneknitter")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Boneknitter's regen targeting itself
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(boneknitterId))
                    )
                )
                game.resolveStack()

                // Verify regeneration shield was applied
                withClue("Should have a floating effect (regen shield)") {
                    game.state.floatingEffects.isNotEmpty() shouldBe true
                }

                // Shock the Boneknitter — lethal damage, but regen saves it
                // P1 is active, pass to P2 so P2 can cast Shock
                game.passPriority()
                game.castSpell(2, "Shock", boneknitterId)
                game.resolveStack()

                withClue("Boneknitter should survive via regeneration") {
                    game.isOnBattlefield("Boneknitter") shouldBe true
                }
            }
        }
    }
}
