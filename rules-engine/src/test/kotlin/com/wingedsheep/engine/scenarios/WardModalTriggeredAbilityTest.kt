package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ward (CR 702.21) must fire when a **modal triggered ability** targets the warded permanent.
 *
 * CR 603.3c: the controller of a triggered ability chooses its modes *and* its targets as the
 * ability is put onto the stack — not while it resolves. Choosing them at resolution time means
 * the target never "becomes the target" of anything on the stack, so ward never triggers and the
 * controller gets the mode for free.
 *
 * Regression scenario: Downwind Ambusher's ETB ("choose one — target creature an opponent
 * controls gets -1/-1; or destroy target creature an opponent controls that was dealt damage
 * this turn") pointed at Long River Lurker (ward {1}).
 */
class WardModalTriggeredAbilityTest : ScenarioTestBase() {

    init {
        context("ward vs. a modal triggered ability") {

            test("ward triggers when the -1/-1 mode targets the warded creature") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Downwind Ambusher")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Long River Lurker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurker = game.findPermanent("Long River Lurker")!!

                val cast = game.castSpell(1, "Downwind Ambusher")
                withClue("Downwind Ambusher should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // Mode + targets are chosen as the trigger goes on the stack (CR 603.3c).
                val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the mode; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(lurker))))

                // Ward {1} triggered — resolving it asks the ability's controller to pay.
                game.resolveStack()
                val wardDecision = game.state.pendingDecision as? SelectManaSourcesDecision
                    ?: error("expected ward's SelectManaSourcesDecision; got ${game.state.pendingDecision}")
                withClue("ward's cost is asked of the ability's controller") {
                    wardDecision.playerId shouldBe game.player1Id
                    wardDecision.requiredCost shouldBe "{1}"
                    wardDecision.canDecline shouldBe true
                }
            }

            test("declining the ward cost counters the modal ability, so -1/-1 never applies") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Downwind Ambusher")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Long River Lurker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurker = game.findPermanent("Long River Lurker")!!

                game.castSpell(1, "Downwind Ambusher")
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))
                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(lurker))))

                game.resolveStack()
                game.state.pendingDecision.shouldBeSelectManaSources()
                // Decline the ward payment — the modal ability is countered.
                game.submitManaSourcesDecision()
                game.resolveStack()

                withClue("countered ability never applied -1/-1; the 2/3 Lurker is untouched") {
                    game.state.projectedState.getPower(lurker) shouldBe 2
                    game.state.projectedState.getToughness(lurker) shouldBe 3
                }
            }

            test("paying the ward cost lets the destroy mode resolve") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Downwind Ambusher")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardOnBattlefield(2, "Long River Lurker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurker = game.findPermanent("Long River Lurker")!!

                // Damage the 2/3 Lurker so the destroy mode has a legal target (it survives Shock).
                game.castSpell(1, "Shock", lurker)
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                // Shock also targets, so its own ward trigger has to be paid first.
                if (game.state.pendingDecision is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }
                withClue("Shock's 2 damage doesn't kill the 2/3") {
                    game.findPermanent("Long River Lurker") shouldNotBe null
                }

                game.castSpell(1, "Downwind Ambusher")
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
                game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 1))
                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(lurker))))

                game.resolveStack()
                game.state.pendingDecision.shouldBeSelectManaSources()
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("ward paid, so the destroy mode resolves") {
                    game.findPermanent("Long River Lurker") shouldBe null
                }
            }

            test("declining ward counters a plain targeted trigger too, not just spells") {
                // Ward's "counter it unless…" has always been able to point at an ability, but the
                // decline path only knew how to counter *spells*: countering a triggered ability
                // errored out and left it on the stack to resolve for free.
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Flametongue Kavu")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Long River Lurker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurker = game.findPermanent("Long River Lurker")!!

                game.castSpell(1, "Flametongue Kavu")
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the ETB; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(lurker))))

                game.resolveStack()
                game.state.pendingDecision.shouldBeSelectManaSources()
                game.submitManaSourcesDecision()
                game.resolveStack()

                withClue("countered trigger deals no damage; the 2/3 survives") {
                    game.findPermanent("Long River Lurker") shouldNotBe null
                }
            }
        }
    }
}

private fun Any?.shouldBeSelectManaSources() {
    if (this !is SelectManaSourcesDecision) error("expected ward's SelectManaSourcesDecision; got $this")
}
