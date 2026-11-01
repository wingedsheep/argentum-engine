package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.combat.RedirectNextDamageExecutor
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Rules every damage-redirection shield ([RedirectNextDamageEffect]) obeys, whatever card made it:
 *
 * - CR 614.9 — damage isn't redirected to a permanent that is no longer a battle, creature or
 *   planeswalker (or has left the battlefield).
 * - CR 614.5 — a shield gets one opportunity at a damage event, so redirected damage is checked only
 *   against the shields that haven't applied to it yet; two "dealt to that permanent instead"
 *   shields pointing at each other hand the damage on once instead of looping.
 * - A filter shield also covers the targets it lists ("you and permanents you control").
 */
class RedirectionShieldRulesTest : ScenarioTestBase() {
    init {
        val evaluator = PredicateEvaluator(cardRegistry = null)

        fun board() = scenario().withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withLandsOnBattlefield(1, "Forest", 1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        /** Player 1 installs an all-turn shield sending damage to [recipient]. */
        fun TestGame.install(recipient: EntityId, protected: List<EffectTarget>, filter: GameObjectFilter?) {
            val effect = RedirectNextDamageEffect(
                protectedTargets = protected, redirectTo = EffectTarget.ContextTarget(0),
                scope = RedirectScope.CONTINUOUS, protectedFilter = filter
            )
            val result = RedirectNextDamageExecutor().execute(
                state, effect,
                EffectContext(
                    sourceId = findPermanent("Grizzly Bears"), controllerId = player1Id,
                    targets = listOf(ChosenTarget.Permanent(recipient))
                )
            )
            result.error shouldBe null
            state = result.state
        }

        test("CR 614.9: damage is not redirected to a permanent that is not a creature, planeswalker or battle") {
            val game = board()
            val forest = game.findPermanent("Forest")!!
            game.install(forest, listOf(EffectTarget.Controller), null)

            DamageUtils.checkDamageRedirection(game.state, game.player1Id, 3, predicateEvaluator = evaluator)
                .redirectTo shouldBe null
        }

        test("a filter shield covers the players it lists as well as the permanents it filters") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.install(giant, listOf(EffectTarget.Controller), GameObjectFilter.Permanent.youControl())

            DamageUtils.checkDamageRedirection(game.state, game.player1Id, 3, predicateEvaluator = evaluator)
                .redirectTo shouldBe giant
            DamageUtils.checkDamageRedirection(game.state, bears, 3, predicateEvaluator = evaluator)
                .redirectTo shouldBe giant
            DamageUtils.checkDamageRedirection(game.state, game.player2Id, 3, predicateEvaluator = evaluator)
                .redirectTo shouldBe null
        }

        test("CR 614.5: a shield that already redirected this damage doesn't apply to it again") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.install(bears, emptyList(), GameObjectFilter.Creature.youControl())
            game.install(giant, emptyList(), GameObjectFilter.Creature.youControl())

            // Damage headed for the Bears: their own shield leaves it, the Giant's shield takes it.
            val first = DamageUtils.checkDamageRedirection(game.state, bears, 2, predicateEvaluator = evaluator)
            first.redirectTo shouldBe giant
            val giantShield = first.shieldId.shouldNotBeNull()

            // Now headed for the Giant: the Bears' shield sends it back once…
            val second = DamageUtils.checkDamageRedirection(
                first.state, giant, 2, appliedShields = setOf(giantShield), predicateEvaluator = evaluator
            )
            second.redirectTo shouldBe bears
            // …and with both spent, the damage stays where it is.
            DamageUtils.checkDamageRedirection(
                second.state, bears, 2, appliedShields = setOf(giantShield, second.shieldId!!), predicateEvaluator = evaluator
            ).redirectTo shouldBe null
        }
    }
}
