package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.combat.PreventNextDamageLeavingAmountExecutor
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageLeavingAmountEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.shouldBe

class DamageRemainderSourceIdentityTest : ScenarioTestBase() {
    init {
        val shooter = card("Remainder Identity Shooter") {
            manaCost = "{0}"
            typeLine = "Creature — Human"
            power = 1
            toughness = 4
            activatedAbility {
                cost = Costs.Free
                effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent))
            }
        }
        cardRegistry.register(shooter)

        fun board() = scenario().withPlayers("Player", "Opponent")
            .withCardInHand(2, shooter.name)
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        fun shieldSpell(game: TestGame): com.wingedsheep.sdk.model.EntityId {
            game.castSpell(2, shooter.name).error shouldBe null
            val spellId = game.state.stack.last()
            val result = PreventNextDamageLeavingAmountExecutor(services.dynamicAmountEvaluator).execute(
                game.state,
                PreventNextDamageLeavingAmountEffect(DynamicAmount.Fixed(1), eligibleSource = GameObjectFilter.Creature),
                EffectContext(sourceId = null, controllerId = game.player1Id)
            )
            result.error shouldBe null
            game.state = result.state
            game.selectCards(listOf(spellId)).error shouldBe null
            return spellId
        }

        test("shield follows the chosen permanent spell through its normal resolution") {
            val game = board()
            val spellId = shieldSpell(game)
            val spellRef = game.state.objectRef(spellId)!!
            game.resolveStack()
            val permanentRef = game.state.objectRef(spellId)!!
            (permanentRef == spellRef) shouldBe false
            val shield = game.state.floatingEffects.single()
            (permanentRef in shield.referencedObjects) shouldBe true
            (spellRef in shield.referencedObjects) shouldBe false
            (shield.effect.modification as SerializableModification.PreventNextDamageLeavingAmount)
                .permanentSpell shouldBe false

            game.execute(ActivateAbility(game.player2Id, spellId, shooter.script.activatedAbilities.single().id))
                .error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 19
            game.state.floatingEffects.none {
                it.effect.modification is SerializableModification.PreventNextDamageLeavingAmount
            } shouldBe true
            game.execute(ActivateAbility(game.player2Id, spellId, shooter.script.activatedAbilities.single().id))
                .error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 16
        }

        test("following spell resolution does not follow a later battlefield visit") {
            val game = board()
            val spellId = shieldSpell(game)
            game.resolveStack()
            val battlefield = ZoneKey(game.player2Id, Zone.BATTLEFIELD)
            val exile = ZoneKey(game.player2Id, Zone.EXILE)
            game.state = game.state.removeFromZone(battlefield, spellId).addToZone(exile, spellId)
                .removeFromZone(exile, spellId).addToZone(battlefield, spellId)
            val result = DamageUtils.applyDamagePreventionShields(
                game.state, game.player1Id, 3, sourceId = spellId,
                predicateEvaluator = services.predicateEvaluator
            )
            result.remainingDamage shouldBe 3
        }
    }
}
