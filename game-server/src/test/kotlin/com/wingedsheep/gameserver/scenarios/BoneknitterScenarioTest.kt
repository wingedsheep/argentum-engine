package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Boneknitter and the regeneration mechanic.
 *
 * Boneknitter: {1}{B} 1/1 Creature — Zombie Cleric
 * {1}{B}: Regenerate target Zombie.
 * Morph {2}{B}
 */
class BoneknitterScenarioTest : ScenarioTestBase() {

    init {
        context("Boneknitter regeneration ability") {

            test("regenerated zombie survives destruction") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boneknitter")
                    .withCardOnBattlefield(1, "Severed Legion") // 2/2 Zombie
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!
                val legionId = game.findPermanent("Severed Legion")!!

                val cardDef = cardRegistry.getCard("Boneknitter")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate Boneknitter's ability targeting Severed Legion
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(legionId))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the regeneration ability
                game.resolveStack()

                // Verify regeneration shield was applied
                withClue("Should have a floating effect (regen shield) after resolving") {
                    game.state.floatingEffects.isNotEmpty() shouldBe true
                }

                // Opponent casts Shock targeting the Severed Legion (instant, during P1's turn)
                game.execute(PassPriority(game.player1Id))
                val shockResult = game.castSpell(2, "Shock", legionId)
                withClue("Shock should succeed: ${shockResult.error}") {
                    shockResult.error shouldBe null
                }

                // Resolve Shock — lethal damage to 2/2, but regen shield saves it
                game.resolveStack()

                withClue("Severed Legion should survive via regeneration") {
                    game.isOnBattlefield("Severed Legion") shouldBe true
                }
            }

            test("regenerated creature is tapped and has no damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boneknitter")
                    .withCardOnBattlefield(1, "Severed Legion") // 2/2 Zombie
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!
                val legionId = game.findPermanent("Severed Legion")!!

                val cardDef = cardRegistry.getCard("Boneknitter")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(legionId))
                    )
                )
                game.resolveStack()

                // Shock the creature to trigger regeneration
                game.execute(PassPriority(game.player1Id))
                game.castSpell(2, "Shock", legionId)
                game.resolveStack()

                // Check creature state after regeneration
                val container = game.state.getEntity(legionId)!!
                withClue("Regenerated creature should be tapped") {
                    container.has<TappedComponent>() shouldBe true
                }
                withClue("Regenerated creature should have no damage") {
                    (container.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
            }

            test("non-zombie target is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boneknitter")
                    .withCardOnBattlefield(1, "Devoted Hero") // 1/2 Human, not a Zombie
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("Boneknitter")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activation should fail because Devoted Hero is not a Zombie") {
                    result.error shouldBe "Target does not match filter: creature Zombie"
                }
            }

            test("creature without regeneration shield dies normally") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Severed Legion") // 2/2 Zombie, no shield
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val legionId = game.findPermanent("Severed Legion")!!

                game.execute(PassPriority(game.player1Id))
                game.castSpell(2, "Shock", legionId)
                game.resolveStack()

                withClue("Severed Legion should die without regeneration shield") {
                    game.isOnBattlefield("Severed Legion") shouldBe false
                    game.isInGraveyard(1, "Severed Legion") shouldBe true
                }
            }

            test("cant be regenerated prevents regeneration (Smother)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boneknitter")
                    .withCardOnBattlefield(1, "Severed Legion") // 2/2 Zombie, MV 3
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Smother") // {1}{B}: Destroy target MV <= 3, can't be regenerated
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!
                val legionId = game.findPermanent("Severed Legion")!!

                val cardDef = cardRegistry.getCard("Boneknitter")!!
                val ability = cardDef.script.activatedAbilities.first()

                // P1 activates regen on Severed Legion
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(legionId))
                    )
                )
                game.resolveStack()

                // P2 casts Smother (instant: can't be regenerated + destroy)
                game.execute(PassPriority(game.player1Id))
                game.castSpell(2, "Smother", legionId)
                game.resolveStack()

                withClue("Severed Legion should be destroyed — Smother prevents regeneration") {
                    game.isOnBattlefield("Severed Legion") shouldBe false
                    game.isInGraveyard(1, "Severed Legion") shouldBe true
                }
            }

            test("noRegenerate on DestroyAll prevents regeneration (Wrath of God)") {
                // P2 is active so they can cast Wrath (sorcery)
                // P1 activates Boneknitter in response on the stack
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boneknitter")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Wrath of God")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!
                val legionId = game.findPermanent("Severed Legion")!!

                // P2 casts Wrath of God (sorcery, during their main phase)
                val wrathResult = game.castSpell(2, "Wrath of God")
                withClue("Wrath of God should succeed: ${wrathResult.error}") {
                    wrathResult.error shouldBe null
                }

                // P2 passes priority after casting
                game.passPriority()

                // P1 responds by activating Boneknitter's regen on Severed Legion
                val cardDef = cardRegistry.getCard("Boneknitter")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(legionId))
                    )
                )
                withClue("Regen activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Both players pass to resolve the stack (regen resolves first, then Wrath)
                game.resolveStack()

                // Both creatures should be destroyed despite regen shield (Wrath has noRegenerate)
                withClue("Severed Legion should be destroyed — Wrath prevents regeneration") {
                    game.isOnBattlefield("Severed Legion") shouldBe false
                }
                withClue("Boneknitter should also be destroyed") {
                    game.isOnBattlefield("Boneknitter") shouldBe false
                }
            }

            test("regeneration shield is consumed (only protects once)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boneknitter")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Shock")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boneknitterId = game.findPermanent("Boneknitter")!!
                val legionId = game.findPermanent("Severed Legion")!!

                val cardDef = cardRegistry.getCard("Boneknitter")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Put ONE regeneration shield on Severed Legion
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boneknitterId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(legionId))
                    )
                )
                game.resolveStack()

                // First Shock — regeneration saves it
                game.execute(PassPriority(game.player1Id))
                game.castSpell(2, "Shock", legionId)
                game.resolveStack()

                withClue("Severed Legion should survive the first Shock") {
                    game.isOnBattlefield("Severed Legion") shouldBe true
                }

                // Pass priority to P2 for second Shock
                game.execute(PassPriority(game.player1Id))

                // Second Shock — no more shields, creature dies
                val shock2 = game.castSpell(2, "Shock", legionId)
                withClue("Second Shock should succeed: ${shock2.error}") {
                    shock2.error shouldBe null
                }
                game.resolveStack()

                withClue("Severed Legion should die from the second Shock") {
                    game.isOnBattlefield("Severed Legion") shouldBe false
                    game.isInGraveyard(1, "Severed Legion") shouldBe true
                }
            }
        }
    }
}
