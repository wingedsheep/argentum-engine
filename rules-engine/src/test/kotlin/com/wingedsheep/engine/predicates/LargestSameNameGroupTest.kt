package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.values.Aggregation.LARGEST_SAME_NAME_GROUP] — "eight or more
 * artifacts with the same name as one another" (Mechanized Production): the biggest same-named
 * bucket among the matched permanents, not their total, not their distinct-name count.
 */
class LargestSameNameGroupTest : FunSpec({

    val amountEvaluator = PredicateEvaluator(cardRegistry = null).amounts
    val you = EntityId.generate()
    val opponent = EntityId.generate()

    data class Perm(
        val name: String,
        val controller: EntityId,
        val type: CardType = CardType.ARTIFACT,
        val faceDown: Boolean = false,
    )

    fun stateWith(vararg perms: Perm): GameState {
        var state = GameState()
            .withEntity(you, ComponentContainer())
            .withEntity(opponent, ComponentContainer())
        perms.forEach { p ->
            val id = EntityId.generate()
            var container = ComponentContainer()
                .with(
                    CardComponent(
                        cardDefinitionId = p.name,
                        name = p.name,
                        manaCost = ManaCost.parse("{1}"),
                        typeLine = TypeLine(cardTypes = setOf(p.type)),
                        ownerId = p.controller
                    )
                )
                .with(OwnerComponent(p.controller))
                .with(ControllerComponent(p.controller))
            if (p.faceDown) container = container.with(FaceDownComponent)
            state = state.withEntity(id, container).addToZone(ZoneKey(p.controller, Zone.BATTLEFIELD), id)
        }
        return state
    }

    fun GameState.largestGroup(filter: GameObjectFilter) = amountEvaluator.evaluate(
        this,
        DynamicAmounts.battlefield(Player.You, filter).largestSameNameGroup(),
        EffectContext(sourceId = null, controllerId = you)
    )

    fun GameState.largestArtifactGroup() = largestGroup(GameObjectFilter.Artifact)

    test("the biggest same-named bucket, not the total") {
        stateWith(
            Perm("Thopter", you), Perm("Thopter", you), Perm("Thopter", you),
            Perm("Servo", you), Perm("Servo", you),
        ).largestArtifactGroup() shouldBe 3
    }

    test("zero when nothing matches") {
        stateWith(Perm("Grizzly Bears", you, CardType.CREATURE)).largestArtifactGroup() shouldBe 0
    }

    test("only the filter's matches join a bucket — a same-named nonartifact doesn't") {
        stateWith(
            Perm("Thopter", you), Perm("Thopter", you),
            Perm("Thopter", you, CardType.ENCHANTMENT),
        ).largestArtifactGroup() shouldBe 2
    }

    test("an opponent's same-named artifacts don't count toward yours") {
        stateWith(
            Perm("Thopter", you), Perm("Thopter", opponent), Perm("Thopter", opponent),
        ).largestArtifactGroup() shouldBe 1
    }

    // Over Any, so the face-down one is still matched — a face-down permanent loses its types,
    // and an Artifact filter would drop it before its name was ever read.
    test("a face-down permanent has no name and shares one with nothing (CR 201.2a)") {
        val state = stateWith(
            Perm("Morph", you, CardType.CREATURE), Perm("Morph", you, CardType.CREATURE),
            Perm("Morph", you, CardType.CREATURE, faceDown = true),
            Perm("Morph", you, CardType.CREATURE, faceDown = true),
            Perm("Morph", you, CardType.CREATURE, faceDown = true),
        )
        state.largestGroup(GameObjectFilter.Any) shouldBe 2
    }
})
