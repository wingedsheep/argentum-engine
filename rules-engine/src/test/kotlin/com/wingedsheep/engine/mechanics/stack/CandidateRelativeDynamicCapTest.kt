package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.matchers.shouldBe

/**
 * A dynamic filter cap phrased relative to the object under test — "target creature with mana value
 * less than or equal to the number of cards in **its controller's** graveyard" (Drown in the Loch).
 * The predicate evaluator binds each candidate's controller as [Player.Candidate], so the count
 * reads that candidate's controller's graveyard, not the ability controller's.
 * Both candidates are judged in one state, and the bound is re-read on resolution (CR 608.2b).
 */
class CandidateRelativeDynamicCapTest : ScenarioTestBase() {
    init {
        val requirement = TargetObject(
            filter = TargetFilter.Creature.manaValueAtMostDynamic(
                DynamicAmounts.cardsInYourGraveyard(Player.Candidate)
            )
        )

        fun validate(game: TestGame, target: ChosenTarget) =
            ResolutionTargetValidator(PredicateEvaluator(cardRegistry)).validateTargets(
                state = game.state,
                targets = listOf(target),
                controllerId = game.player1Id,
                targetRequirements = listOf(requirement),
                targetingSourceType = TargetingSourceType.SPELL,
            )

        // Player 1 (the ability's controller) has an empty graveyard; player 2 has two cards.
        fun board() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInGraveyard(2, "Island")
            .withCardInGraveyard(2, "Island")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        test("an opponent's creature is measured against the opponent's graveyard") {
            val game = board()
            val target = ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)
            validate(game, target) shouldBe listOf(target)
        }

        test("the controller's own creature is measured against the controller's graveyard") {
            val game = board()
            val target = ChosenTarget.Permanent(game.findPermanent("Savannah Lions")!!)
            validate(game, target) shouldBe emptyList()
        }

        test("the candidate's bound is re-read on resolution") {
            val game = board()
            val target = ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)
            val island = game.findCardsInGraveyard(2, "Island").first()
            game.state = game.zones.moveToZone(game.state, island, Zone.EXILE).state
            validate(game, target) shouldBe emptyList()
        }
    }
}
