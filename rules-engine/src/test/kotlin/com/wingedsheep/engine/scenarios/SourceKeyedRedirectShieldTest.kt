package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.combat.RedirectNextDamageExecutor
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `RedirectNextDamageEffect.fromSource` — a redirection shield keyed to one damage *object*
 * ("all damage that would be dealt to you this turn by target attacking creature", Shimian Night
 * Stalker), and with no protected targets one that covers every recipient ("all damage that would
 * be dealt this turn by target sorcery spell is dealt to that spell's controller", Reverberation).
 */
class SourceKeyedRedirectShieldTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Savannah Lions")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        /** Player 1 installs a shield keyed to [source] (ContextTarget 0). */
        fun TestGame.install(source: EntityId, protected: List<EffectTarget>, redirectTo: EffectTarget, scope: RedirectScope) {
            val effect = RedirectNextDamageEffect(
                protectedTargets = protected, redirectTo = redirectTo, scope = scope,
                fromSource = EffectTarget.ContextTarget(0)
            )
            val result = RedirectNextDamageExecutor().execute(
                state, effect,
                EffectContext(
                    sourceId = findPermanent("Grizzly Bears"), controllerId = player1Id,
                    targets = listOf(ChosenTarget.Permanent(source))
                )
            )
            result.error shouldBe null
            state = result.state
        }

        test("only damage from the keyed source is redirected, and a continuous shield keeps redirecting") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findPermanent("Savannah Lions")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.install(giant, listOf(EffectTarget.Controller), EffectTarget.Self, RedirectScope.CONTINUOUS)

            val (afterFirst, firstTarget, firstAmount) =
                DamageUtils.checkDamageRedirection(game.state, game.player1Id, 3, sourceId = giant)
            firstTarget shouldBe bears
            firstAmount shouldBe 3
            withClue("CONTINUOUS: the shield survives its first use") {
                DamageUtils.checkDamageRedirection(afterFirst, game.player1Id, 2, sourceId = giant).second shouldBe bears
            }
            withClue("another source's damage to the protected player is untouched") {
                DamageUtils.checkDamageRedirection(game.state, game.player1Id, 2, sourceId = lions).second shouldBe null
            }
            withClue("the keyed source's damage to an unprotected recipient is untouched") {
                DamageUtils.checkDamageRedirection(game.state, game.player2Id, 3, sourceId = giant).second shouldBe null
            }
        }

        test("with no protected targets the shield covers every recipient, except the one it redirects to") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findPermanent("Savannah Lions")!!
            game.install(giant, emptyList(), EffectTarget.TargetController, RedirectScope.CONTINUOUS)

            DamageUtils.checkDamageRedirection(game.state, game.player1Id, 3, sourceId = giant).second shouldBe game.player2Id
            DamageUtils.checkDamageRedirection(game.state, lions, 3, sourceId = giant).second shouldBe game.player2Id
            withClue("damage already aimed at the redirect target stays put — no redirect loop") {
                DamageUtils.checkDamageRedirection(game.state, game.player2Id, 3, sourceId = giant).second shouldBe null
            }
        }

        test("a source that left the battlefield and came back is a new object the shield doesn't know") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.install(giant, listOf(EffectTarget.Controller), EffectTarget.Self, RedirectScope.CONTINUOUS)
            val shield = game.state.floatingEffects.single {
                it.effect.modification is SerializableModification.RedirectNextDamage
            }.effect.modification as SerializableModification.RedirectNextDamage
            shield.chosenSource!!.reference shouldBe game.state.objectRef(giant)

            val staleRef = shield.chosenSource!!.reference.copy(generation = shield.chosenSource!!.reference.generation + 1)
            DamageUtils.checkDamageRedirection(
                game.state, game.player1Id, 3, sourceId = giant, damageSourceRef = staleRef
            ).second shouldBe null
        }
    }
}
