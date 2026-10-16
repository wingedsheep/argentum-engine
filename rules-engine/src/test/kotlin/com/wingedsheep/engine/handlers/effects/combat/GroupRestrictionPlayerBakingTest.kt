package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ResolutionPlayerBaker
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Group restrictions ("creatures [that player] controls can't block this turn") outlive the
 * resolution that created them: their filters are re-read on every projection, where the
 * ability's targets and trigger are gone. [ResolutionPlayerBaker] pins every context-bound player
 * reference to a concrete player at resolution, so the restriction keeps naming that player while
 * the set of their creatures stays live (CR 611.2c).
 */
class GroupRestrictionPlayerBakingTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
    }

    test("a target-player group restriction keeps binding that player after resolution") {
        val d = driver()
        val mine = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val context = EffectContext(
            sourceId = null,
            controllerId = d.player1,
            targets = listOf(ChosenTarget.Player(d.player2))
        )

        val result = CantBlockGroupExecutor().execute(
            d.state,
            CantBlockGroupEffect(GroupFilter(GameObjectFilter.Creature.targetPlayerControls())),
            context
        )
        d.replaceState(result.state)

        d.state.projectedState.cantBlock(theirs) shouldBe true
        d.state.projectedState.cantBlock(mine) shouldBe false

        val later = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.state.projectedState.cantBlock(later) shouldBe true
    }

    test("context-bound references are pinned, state-relative ones are left symbolic") {
        val d = driver()
        val context = EffectContext(
            sourceId = null,
            controllerId = d.player1,
            targets = listOf(ChosenTarget.Player(d.player2))
        )
        val pinned = ControllerPredicate.ControlledByReferencedPlayer(EffectTarget.SpecificEntity(d.player2))

        ResolutionPlayerBaker.bake(GameObjectFilter.Creature.targetPlayerControls(), context, d.state)
            .controllerPredicate shouldBe pinned
        ResolutionPlayerBaker.bake(
            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.ContextTarget(0)), context, d.state
        ).controllerPredicate shouldBe pinned
        ResolutionPlayerBaker.bake(GameObjectFilter.Creature.youControl(), context, d.state)
            .controllerPredicate shouldBe ControllerPredicate.ControlledByYou

        // Nothing to answer with (no combat): the reference stays symbolic rather than matching nobody.
        val defending = GameObjectFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
        ResolutionPlayerBaker.bake(defending, EffectContext(sourceId = null, controllerId = d.player1), d.state) shouldBe
            defending
    }
})
