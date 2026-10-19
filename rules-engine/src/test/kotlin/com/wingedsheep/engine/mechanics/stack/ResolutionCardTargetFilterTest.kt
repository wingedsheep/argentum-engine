package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.matchers.shouldBe

/**
 * CR 608.2b for card targets outside the battlefield: a graveyard card target is re-checked against
 * its requirement's filter at resolution, not only for still being in the graveyard — so a dynamic
 * cap ("mana value X or less, where X is …") that no longer holds makes the target illegal.
 */
class ResolutionCardTargetFilterTest : ScenarioTestBase() {
    init {
        // "Creature card with mana value X or less from your graveyard, X = creatures you control."
        val requirement = TargetObject(
            filter = TargetFilter.CreatureInYourGraveyard.manaValueAtMostDynamic(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()
            )
        )

        fun validate(game: TestGame, target: ChosenTarget) =
            ResolutionTargetValidator(PredicateEvaluator(cardRegistry)).validateTargets(
                state = game.state,
                targets = listOf(target),
                controllerId = game.player1Id,
                targetRequirements = listOf(requirement),
                targetingSourceType = TargetingSourceType.TRIGGERED_ABILITY,
            )

        fun board() = scenario().withPlayers()
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Grizzly Bears")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        test("a graveyard card target that still fits its dynamic cap stays legal") {
            val game = board()
            val target = ChosenTarget.Card(game.findCardsInGraveyard(1, "Grizzly Bears").single(), game.player1Id, Zone.GRAVEYARD)
            validate(game, target) shouldBe listOf(target)
        }

        test("a graveyard card target whose dynamic cap dropped below its mana value is dropped") {
            val game = board()
            val target = ChosenTarget.Card(game.findCardsInGraveyard(1, "Grizzly Bears").single(), game.player1Id, Zone.GRAVEYARD)
            game.state = game.zones.moveToZone(game.state, game.findPermanent("Hill Giant")!!, Zone.GRAVEYARD).state
            validate(game, target) shouldBe emptyList()
        }

        test("the owner relation is re-read against the card's owner") {
            val game = board()
            val target = ChosenTarget.Card(game.findCardsInGraveyard(2, "Grizzly Bears").single(), game.player2Id, Zone.GRAVEYARD)
            validate(game, target) shouldBe emptyList()
        }
    }
}
