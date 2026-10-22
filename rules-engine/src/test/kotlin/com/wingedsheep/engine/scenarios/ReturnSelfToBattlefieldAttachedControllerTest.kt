package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Who controls an Aura that [Effects.ReturnSelfToBattlefieldAttached] returns onto another
 * player's permanent. With no controller clause the Aura follows its host (the Dragon-aura
 * cycle's "attached to that creature"); `controllerOverride` is the printed "under your control"
 * (Vengeful Strangler), and keeps the Aura with the ability's controller.
 */
class ReturnSelfToBattlefieldAttachedControllerTest : ScenarioTestBase() {
    init {
        fun recursiveAura(name: String, controllerOverride: EffectTarget?) = card(name) {
            typeLine = "Enchantment — Aura"
            auraTarget = TargetObject(filter = TargetFilter.Creature)
            activatedAbility {
                cost = AbilityCost.Free
                val creature = target(TargetFilter.Creature)
                effect = Effects.ReturnSelfToBattlefieldAttached(creature, controllerOverride = controllerOverride)
                activateFromZone = Zone.GRAVEYARD
            }
        }
        cardRegistry.register(recursiveAura("Host-Following Aura", controllerOverride = null))
        cardRegistry.register(recursiveAura("Loyal Aura", controllerOverride = EffectTarget.Controller))

        fun returnOntoOpponentsBears(auraName: String): Pair<TestGame, Pair<EntityId, EntityId>> {
            val game = scenario().withPlayers("A", "B")
                .withCardInGraveyard(1, auraName)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val aura = game.findCardsInGraveyard(1, auraName).single()
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard(auraName)!!.activatedAbilities.single().id
            game.execute(
                ActivateAbility(game.player1Id, aura, abilityId, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()
            return game to (aura to bears)
        }

        test("with no controller clause the returned Aura follows its host's controller") {
            val (game, ids) = returnOntoOpponentsBears("Host-Following Aura")
            val (aura, bears) = ids
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.getController(aura) shouldBe game.player2Id
        }

        test("controllerOverride keeps the Aura under the ability's controller on an opponent's creature") {
            val (game, ids) = returnOntoOpponentsBears("Loyal Aura")
            val (aura, bears) = ids
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
            withClue("'under your control' — player A's Aura on player B's creature") {
                game.state.projectedState.getController(aura) shouldBe game.player1Id
            }
        }
    }
}
