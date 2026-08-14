package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Alacrian Armory (DFT #2) — {3}{W} Artifact.
 *
 * "Creatures you control get +0/+1 and have vigilance.
 *  At the beginning of combat on your turn, choose up to one target Mount or Vehicle you control.
 *  Until end of turn, that permanent becomes saddled if it's a Mount and becomes an artifact
 *  creature if it's a Vehicle."
 *
 * The trigger's two halves are separately gated on the chosen permanent's type, so the tests care
 * about which half fires for which target: a Vehicle must be animated and *not* stamped saddled
 * (the `BecomeSaddled` executor has no Mount check of its own), and a Mount must be saddled and not
 * spuriously re-typed. The lord half is checked on the same board so the +0/+1 doesn't get lost
 * behind the animate.
 */
class AlacrianArmoryScenarioTest : ScenarioTestBase() {

    init {
        context("Alacrian Armory") {

            test("a targeted Vehicle becomes an artifact creature and is not marked saddled") {
                val game = armoryGame("Skybox Ferry")
                val ferry = game.findPermanent("Skybox Ferry")!!

                withClue("a Vehicle is not a creature before the trigger resolves") {
                    game.state.projectedState.isCreature(ferry) shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveTriggerTargeting(ferry)

                val projected = game.state.projectedState
                withClue("the Vehicle half fired") {
                    projected.isCreature(ferry) shouldBe true
                    projected.hasType(ferry, "ARTIFACT") shouldBe true
                }
                withClue("the Mount half must not fire on a Vehicle") {
                    game.state.getEntity(ferry)!!.has<SaddledComponent>() shouldBe false
                }
                withClue("the printed 4/4 is the base P/T, plus the Armory's own +0/+1 -> 4/5") {
                    projected.getPower(ferry) shouldBe 4
                    projected.getToughness(ferry) shouldBe 5
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("\"until end of turn\" — the Vehicle stops being a creature") {
                    game.state.projectedState.isCreature(ferry) shouldBe false
                }
            }

            test("a targeted Mount becomes saddled") {
                val game = armoryGame("Gilded Ghoda")
                val mount = game.findPermanent("Gilded Ghoda")!!

                withClue("nothing has saddled it yet") {
                    game.state.getEntity(mount)!!.has<SaddledComponent>() shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveTriggerTargeting(mount)

                withClue("the Mount half fired") {
                    game.state.getEntity(mount)!!.has<SaddledComponent>() shouldBe true
                }
            }

            test("creatures you control get +0/+1 and vigilance") {
                val game = armoryGame("Hill Giant")
                val giant = game.findPermanent("Hill Giant")!!

                val projected = game.state.projectedState
                withClue("printed 3/3 plus +0/+1") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 4
                }
                withClue("and vigilance") {
                    projected.hasKeyword(giant, com.wingedsheep.sdk.core.Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }

    /**
     * Resolve the begin-combat trigger, choosing [choice] for its "up to one target". The trigger
     * pauses for target selection before it goes on the stack.
     */
    private fun TestGame.resolveTriggerTargeting(choice: com.wingedsheep.sdk.model.EntityId) {
        if (hasPendingDecision()) selectTargets(listOf(choice))
        resolveStack()
    }

    /**
     * The Armory plus one companion permanent, starting in the upkeep so passing forward reaches the
     * begin-combat trigger. The companion is named by the test: an ability-free Vehicle, a Mount, or
     * a vanilla creature for the lord check.
     */
    private fun armoryGame(companion: String): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Alacrian Armory")
        .withCardOnBattlefield(1, companion, summoningSickness = false)
        .withActivePlayer(1)
        .withTurnNumber(3)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
}
