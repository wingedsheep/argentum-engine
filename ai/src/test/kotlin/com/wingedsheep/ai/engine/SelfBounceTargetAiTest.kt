package com.wingedsheep.ai.engine

import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [AiProfile.recastBouncedPermanents]: a "return another target permanent you control to its
 * owner's hand" aims at the permanent worth recasting, and is taken for that recast.
 *
 * Reported from live games: Rescue Girl, First Responder kept returning the AI's own Island while
 * Mindseeker Oculus ("when this creature enters, empower Jace 4") sat beside it. Target refinement
 * scored each pick as "permanent gone, card in hand" and took the cheapest loss.
 */
class SelfBounceTargetAiTest : ScenarioTestBase() {

    private val withoutFlag = AiProfile.LIVE.copy(id = "live-without-recastbounced", recastBouncedPermanents = false)

    private fun choose(game: TestGame, profile: AiProfile): Pair<GameAction, List<AiActionOption>> {
        var options = emptyList<AiActionOption>()
        val sink = AiInsightSink { _, insight -> if (insight.options.size > 1) options = insight.options }
        val action = AIPlayer.create(cardRegistry, game.player1Id, profile, insightSink = sink)
            .chooseAction(game.state)
        return action to options
    }

    /** The permanent the Rescue Girl activation among [options] aims at, by name. */
    private fun TestGame.rescueTarget(options: List<AiActionOption>): String? =
        options.firstOrNull { it.cardName == RESCUE_GIRL }?.targets?.singleOrNull()

    private fun TestGame.returns(action: GameAction): EntityId? {
        if (action !is ActivateAbility || action.sourceId != findPermanent(RESCUE_GIRL)) return null
        return (action.targets.singleOrNull() as? ChosenTarget.Permanent)?.entityId
    }

    /** Five lands untapped: the mana to recast the Oculus, and more. */
    private fun board(jace: Boolean, bearsInHand: Boolean, step: Step = Step.POSTCOMBAT_MAIN): TestGame = scenario().withPlayers()
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(1, RESCUE_GIRL)
        .withCardOnBattlefield(1, "Mindseeker Oculus")
        .apply { if (jace) withCardOnBattlefield(1, "Jace", isToken = true) }
        .apply { if (bearsInHand) withCardInHand(1, "Grizzly Bears") }
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(2, "Forest", 4)
        .apply { repeat(6) { withCardInLibrary(1, "Island") } }
        .apply { repeat(6) { withCardInLibrary(2, "Forest") } }
        .withTurnNumber(7)
        .build().advanceToPriority(1, step)

    init {
        test("with a spell in hand, Rescue Girl was aimed at an Island; now it returns the Oculus") {
            val game = board(jace = false, bearsInHand = true)

            val (_, oldOptions) = choose(game, withoutFlag)
            withClue("reproduction: the activation was aimed at a land\n${oldOptions.joinToString("\n")}") {
                game.rescueTarget(oldOptions) shouldBe "Island"
            }

            val (action, options) = choose(game, AiProfile.LIVE)
            withClue("chose $action\n${options.joinToString("\n")}") {
                game.rescueTarget(options) shouldBe "Mindseeker Oculus"
                game.returns(action) shouldBe game.findPermanent("Mindseeker Oculus")
            }
        }

        test("a Jace token is never the pick — it ceases to exist in hand") {
            val game = board(jace = true, bearsInHand = false, step = Step.PRECOMBAT_MAIN)

            val (_, oldOptions) = choose(game, withoutFlag)
            withClue("reproduction: the activation was aimed at the token\n${oldOptions.joinToString("\n")}") {
                game.rescueTarget(oldOptions) shouldBe "Jace"
            }

            val (action, options) = choose(game, AiProfile.LIVE)
            withClue("chose $action\n${options.joinToString("\n")}") {
                game.rescueTarget(options) shouldBe "Mindseeker Oculus"
                game.returns(action) shouldNotBe game.findPermanent("Jace")
            }
        }
    }

    private companion object {
        const val RESCUE_GIRL = "Rescue Girl, First Responder"
    }
}
