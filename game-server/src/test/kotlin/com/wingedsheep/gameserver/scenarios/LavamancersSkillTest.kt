package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Scenario tests for Lavamancer's Skill.
 *
 * Lavamancer's Skill: {1}{R} Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "{T}: This creature deals 1 damage to target creature."
 * As long as enchanted creature is a Wizard, it has
 * "{T}: This creature deals 2 damage to target creature." instead.
 */
class LavamancersSkillTest : ScenarioTestBase() {

    init {
        context("Lavamancer's Skill on a non-Wizard creature") {

            test("deals 1 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lavamancer's Skill")
                    .withCardOnBattlefield(1, "Grizzly Bears")       // 2/2 non-Wizard creature
                    .withCardOnBattlefield(2, "Devoted Hero")        // 1/2 target
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Cast Lavamancer's Skill targeting Grizzly Bears
                val castResult = game.castSpell(1, "Lavamancer's Skill", bearsId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Verify aura is attached
                val skillId = game.findPermanent("Lavamancer's Skill")!!
                val attachedTo = game.state.getEntity(skillId)!!.get<AttachedToComponent>()
                withClue("Lavamancer's Skill should be attached to Grizzly Bears") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe bearsId
                }

                // Activate the ability targeting Devoted Hero
                val cardDef = cardRegistry.getCard("Lavamancer's Skill")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = skillId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Grizzly Bears should now be tapped
                withClue("Grizzly Bears should be tapped after activation") {
                    game.state.getEntity(bearsId)!!.has<TappedComponent>() shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Devoted Hero should have taken 1 damage (non-Wizard: 1 damage)
                // Devoted Hero is 1/2, so with 1 damage it should still be alive
                withClue("Devoted Hero should still be on the battlefield (1/2 with 1 damage)") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }
            }
        }

        context("Lavamancer's Skill on a Wizard creature") {

            test("deals 2 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lavamancer's Skill")
                    .withCardOnBattlefield(1, "Information Dealer")  // 1/1 Wizard
                    .withCardOnBattlefield(2, "Grizzly Bears")       // 2/2 target
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wizardId = game.findPermanent("Information Dealer")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Cast Lavamancer's Skill targeting Information Dealer (Wizard)
                val castResult = game.castSpell(1, "Lavamancer's Skill", wizardId)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Activate the ability targeting Grizzly Bears
                val skillId = game.findPermanent("Lavamancer's Skill")!!
                val cardDef = cardRegistry.getCard("Lavamancer's Skill")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = skillId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Information Dealer should now be tapped
                withClue("Information Dealer should be tapped after activation") {
                    game.state.getEntity(wizardId)!!.has<TappedComponent>() shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Grizzly Bears should have taken 2 damage (Wizard: 2 damage)
                // Grizzly Bears is 2/2, so 2 damage = lethal
                withClue("Grizzly Bears should be dead (2/2 with 2 damage from Wizard)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }

        context("Lavamancer's Skill activation restrictions") {

            test("cannot activate if enchanted creature is tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lavamancer's Skill")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Cast and resolve Lavamancer's Skill
                game.castSpell(1, "Lavamancer's Skill", bearsId)
                game.resolveStack()

                val skillId = game.findPermanent("Lavamancer's Skill")!!
                val cardDef = cardRegistry.getCard("Lavamancer's Skill")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Manually tap Grizzly Bears
                game.state = game.state.updateEntity(bearsId) { it.with(TappedComponent) }

                // Try to activate - should fail because bears is tapped
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = skillId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )
                withClue("Activation should fail when enchanted creature is tapped") {
                    activateResult.error shouldNotBe null
                }
            }

            test("cannot activate if enchanted creature has summoning sickness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lavamancer's Skill")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(2, "Devoted Hero")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Cast Lavamancer's Skill on the Bears (which has summoning sickness)
                game.castSpell(1, "Lavamancer's Skill", bearsId)
                game.resolveStack()

                val skillId = game.findPermanent("Lavamancer's Skill")!!
                val cardDef = cardRegistry.getCard("Lavamancer's Skill")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to activate - should fail because Bears has summoning sickness
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = skillId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )
                withClue("Activation should fail when enchanted creature has summoning sickness") {
                    activateResult.error shouldNotBe null
                    activateResult.error!! shouldContainIgnoringCase "summoning sickness"
                }
            }
        }
    }
}
