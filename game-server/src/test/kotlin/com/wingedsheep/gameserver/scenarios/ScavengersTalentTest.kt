package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityFiredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Scavenger's Talent.
 *
 * Scavenger's Talent {B}
 * Enchantment — Class
 *
 * Level 1: Whenever one or more creatures you control die, create a Food token.
 *          This ability triggers only once each turn.
 * Level 2 ({1}{B}): Whenever you sacrifice a permanent, target player mills two cards.
 * Level 3 ({2}{B}): At the beginning of your end step, you may sacrifice three other
 *          nonland permanents. If you do, return a creature card from your graveyard
 *          to the battlefield with a finality counter on it.
 */
class ScavengersTalentTest : ScenarioTestBase() {

    init {
        context("Scavenger's Talent Level 1 — creature dies creates Food") {
            test("creates a Food token when a creature you control dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scavenger's Talent")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature to die
                    .withCardInHand(1, "Volcanic Hammer") // 3 damage to kill own creature
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Volcanic Hammer targeting our own Glory Seeker
                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Volcanic Hammer", glorySeekerId)
                withClue("Should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve Volcanic Hammer — kills Glory Seeker, triggers Scavenger's Talent
                game.resolveStack()

                // Check that a Food token was created
                val food = game.findPermanent("Food")
                withClue("Should have created a Food token") {
                    food shouldNotBe null
                }
            }

            test("once-per-turn tracker is set after trigger fires") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scavenger's Talent")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                val castResult = game.castSpell(1, "Volcanic Hammer", glorySeekerId)
                withClue("Should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Check that the once-per-turn tracker was set
                val talentId = game.findPermanent("Scavenger's Talent")!!
                val tracker = game.state.getEntity(talentId)?.get<TriggeredAbilityFiredThisTurnComponent>()
                withClue("Once-per-turn tracker should be set") {
                    tracker shouldNotBe null
                    tracker!!.abilityIds.size shouldBe 1
                }
            }
        }

        context("Scavenger's Talent level-up") {
            test("can level up from 1 to 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scavenger's Talent")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Scavenger's Talent")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = talentId,
                        abilityId = AbilityId.classLevelUp(2)
                    )
                )
                withClue("Level-up should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val classComponent = game.state.getEntity(talentId)?.get<ClassLevelComponent>()
                withClue("Should be at level 2") {
                    classComponent?.currentLevel shouldBe 2
                }
            }

            test("can level up from 2 to 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scavenger's Talent", classLevel = 2)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Scavenger's Talent")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = talentId,
                        abilityId = AbilityId.classLevelUp(3)
                    )
                )
                withClue("Level-up should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val classComponent = game.state.getEntity(talentId)?.get<ClassLevelComponent>()
                withClue("Should be at level 3") {
                    classComponent?.currentLevel shouldBe 3
                }
            }
        }
    }
}
