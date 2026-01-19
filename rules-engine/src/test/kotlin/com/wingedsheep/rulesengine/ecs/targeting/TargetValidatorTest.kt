package com.wingedsheep.rulesengine.ecs.targeting

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.layers.StateProjector
import com.wingedsheep.rulesengine.targeting.GraveyardCardFilter
import com.wingedsheep.rulesengine.targeting.TargetCardInGraveyard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class TargetValidatorTest : FunSpec({

    val player1 = EntityId.of("p1")
    val creatureDef = CardDefinition.creature("Bear", ManaCost.parse("{G}"), setOf(Subtype.BEAR), 2, 2)
    val sorceryDef = CardDefinition.sorcery("Blast", ManaCost.parse("{R}"), "")

    fun setupState(): Pair<GameState, EntityId> {
        var state = GameState.newGame(listOf(player1 to "Alice", EntityId.of("p2") to "Bob"))
        val (cardId, s2) = state.createEntity(EntityId.generate(), CardComponent(creatureDef, player1))
        // Put card in Player 1's Graveyard
        state = s2.addToZone(cardId, ZoneId.graveyard(player1))
        return state to cardId
    }

    context("TargetCardInGraveyard") {
        val (state, cardId) = setupState()
        val projector = StateProjector(state) // No modifiers needed for GY check

        test("validates correctly for Any filter") {
            val req = TargetCardInGraveyard(filter = GraveyardCardFilter.Any)
            val target = TargetValidator.ValidatorTarget.GraveyardCard(cardId, player1)

            TargetValidator.isValidTarget(target, req, state, projector, player1).shouldBeTrue()
        }

        test("validates correctly for Creature filter") {
            val req = TargetCardInGraveyard(filter = GraveyardCardFilter.Creature)
            val target = TargetValidator.ValidatorTarget.GraveyardCard(cardId, player1)

            TargetValidator.isValidTarget(target, req, state, projector, player1).shouldBeTrue()
        }

        test("fails validation for incorrect filter") {
            val req = TargetCardInGraveyard(filter = GraveyardCardFilter.Sorcery)
            val target = TargetValidator.ValidatorTarget.GraveyardCard(cardId, player1)

            // Card is a creature, not a sorcery
            TargetValidator.isValidTarget(target, req, state, projector, player1).shouldBeFalse()
        }

        test("fails validation if card is not in graveyard") {
            // Move card to hand
            val stateInHand = state.removeFromZone(cardId, ZoneId.graveyard(player1))
                .addToZone(cardId, ZoneId.hand(player1))

            val req = TargetCardInGraveyard(filter = GraveyardCardFilter.Any)
            val target = TargetValidator.ValidatorTarget.GraveyardCard(cardId, player1)

            TargetValidator.isValidTarget(target, req, stateInHand, projector, player1).shouldBeFalse()
        }
    }
})
