package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.RedirectDamageFromChosenSourceExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.RedirectDamageFromChosenSourceEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class SourceChoiceSnapshotLabelsTest : ScenarioTestBase() {
    init {
        fun departedChoices(faceDown: Boolean): ChooseOptionDecision {
            val game = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mountain")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val bear = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val mountain = game.findPermanent("Mountain")!!
            val before = if (faceDown) game.state.updateEntity(giant) { it.with(FaceDownComponent) } else game.state
            // This overload is used by ordinary sacrifice and tap costs.
            val snapshots = captureEntitySnapshots(listOf(bear, giant), before.projectedState, before)
            snapshots.map { it.name } shouldBe listOf("Grizzly Bears", "Hill Giant")
            var departed = before
            for (id in listOf(bear, giant)) {
                departed = departed.removeFromZone(ZoneKey(game.player2Id, Zone.BATTLEFIELD), id)
                    .addToZone(ZoneKey(game.player2Id, Zone.GRAVEYARD), id)
            }
            val abilityId = EntityId.generate()
            val ability = ActivatedAbilityOnStackComponent(
                mountain, "Mountain", game.player1Id, Effects.GainLife(1), sacrificedPermanents = snapshots
            )
            departed = departed.withEntity(abilityId, ComponentContainer.of(ability)).pushToStack(abilityId)
            val paused = RedirectDamageFromChosenSourceExecutor().execute(
                departed, RedirectDamageFromChosenSourceEffect(EffectTarget.Controller, EffectTarget.Controller),
                EffectContext(sourceId = null, controllerId = game.player1Id)
            )
            return paused.state.pendingDecision as ChooseOptionDecision
        }

        test("departed cost objects have distinguishable labels in the source decision") {
            val decision = departedChoices(faceDown = false)
            decision.options shouldContain "Grizzly Bears — departed source"
            decision.options shouldContain "Hill Giant — departed source"
            decision.options shouldNotContain "Departed source — departed source"
        }

        test("departed face-down cost objects retain a masked source label") {
            val decision = departedChoices(faceDown = true)
            decision.options shouldContain "Face-down source — departed source"
            decision.options shouldNotContain "Hill Giant — departed source"
        }
    }
}
