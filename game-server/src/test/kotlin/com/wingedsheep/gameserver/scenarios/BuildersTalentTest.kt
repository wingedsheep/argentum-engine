package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Builder's Talent.
 *
 * Builder's Talent {1}{W}
 * Enchantment — Class
 *
 * Level 1: When this Class enters, create a 0/4 white Wall creature token with defender.
 * Level 2 ({W}): Whenever one or more noncreature, nonland permanents you control enter,
 *          put a +1/+1 counter on target creature you control.
 * Level 3 ({4}{W}): When this Class becomes level 3, return target noncreature, nonland
 *          permanent card from your graveyard to the battlefield.
 */
class BuildersTalentTest : ScenarioTestBase() {

    init {
        context("Builder's Talent Level 1 — ETB creates Wall token") {
            test("creates a 0/4 white Wall token with defender when entering") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Builder's Talent")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Builder's Talent")
                withClue("Should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                // Resolve spell → goes to battlefield → ETB trigger goes on stack
                game.resolveStack()
                // Resolve ETB trigger → creates Wall token
                game.resolveStack()

                // Check that a Wall token was created
                val wallId = game.findPermanent("Wall Token")
                withClue("Should have created a Wall token") {
                    wallId shouldNotBe null
                }

                // Verify it's a 0/4 using projected state
                val projected = game.state.projectedState
                projected.getPower(wallId!!) shouldBe 0
                projected.getToughness(wallId) shouldBe 4
            }
        }

        context("Builder's Talent Level 2 — noncreature nonland ETB triggers counter") {
            test("puts +1/+1 counter on target creature when noncreature nonland permanent enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Builder's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Glory Seeker") // creature to get the counter
                    .withCardInHand(1, "Short Bow") // noncreature, nonland artifact (no ETB)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Short Bow (artifact, no ETB)
                val castResult = game.castSpell(1, "Short Bow")
                withClue("Should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                // Resolve Short Bow → enters battlefield → triggers Builder's Talent level 2
                game.resolveStack()

                // Builder's Talent level 2 trigger should fire — select target creature
                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(glorySeekerId))
                game.resolveStack()

                // Verify the creature got a +1/+1 counter
                val counters = game.state.getEntity(glorySeekerId)?.get<CountersComponent>()
                withClue("Glory Seeker should have a +1/+1 counter") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("does NOT trigger when a creature enters the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Builder's Talent", classLevel = 2)
                    .withCardOnBattlefield(1, "Glory Seeker") // existing creature
                    .withCardInHand(1, "Grizzly Bears") // creature spell
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // No trigger should fire — verify Glory Seeker has no counters
                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                val counters = game.state.getEntity(glorySeekerId)?.get<CountersComponent>()
                withClue("Glory Seeker should have no counters (creature entering doesn't trigger)") {
                    (counters == null || counters.getCount(CounterType.PLUS_ONE_PLUS_ONE) == 0) shouldBe true
                }
            }
        }

        context("Builder's Talent Level 3 — return permanent from graveyard") {
            test("returns noncreature nonland permanent card from graveyard to battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Builder's Talent", classLevel = 2)
                    .withCardInGraveyard(1, "Short Bow") // noncreature, nonland permanent in graveyard
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Builder's Talent")!!

                // Find Short Bow in graveyard
                val shortBowId = game.findCardsInGraveyard(1, "Short Bow").first()

                // Level up to 3
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
                // Resolve level-up ability → triggers "When this Class becomes level 3"
                game.resolveStack()

                // Level 3 ETB trigger fires — select Short Bow from graveyard
                game.selectTargets(listOf(shortBowId))
                game.resolveStack()

                // Short Bow should now be on the battlefield
                val onBattlefield = game.findPermanent("Short Bow")
                withClue("Short Bow should be returned to battlefield") {
                    onBattlefield shouldNotBe null
                }

                // Verify level is 3
                val classComponent = game.state.getEntity(talentId)?.get<ClassLevelComponent>()
                withClue("Should be at level 3") {
                    classComponent?.currentLevel shouldBe 3
                }
            }
        }
    }
}
