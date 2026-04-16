package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.SpellCopiedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.StormCopyEffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phase 3 of `backlog/storm-implementation-correctness.md`: per rule 707.7c the copy
 * inherits *all* decisions made for the original — modes, targets, and the broader
 * cast-time state (X, kicker, warp, evoke, sacrifices, divided damage, chosen creature
 * type, variable exile count, beheld cards, mana-spent colors, the zone cast from).
 *
 * Rule 707.10 also requires that the act of copying is not the same as casting, so
 * payment-time events (ManaSpentEvent, SpellCastEvent) must not re-fire when a copy
 * lands on the stack.
 *
 * The tests are executor-level (as in [ModalCopyPreservationTest] G2) — the full Storm
 * trigger flow is not needed to verify propagation.
 */
class StormCopyInheritsAllDecisionsTest : FunSpec({

    fun buildState(
        p1: EntityId,
        spellEntity: EntityId,
        spellComponent: SpellOnStackComponent
    ): GameState {
        val cardComponent = CardComponent(
            cardDefinitionId = "X Bolt",
            name = "X Bolt",
            manaCost = ManaCost.parse("{X}{R}"),
            typeLine = TypeLine.instant(),
            oracleText = "",
            ownerId = p1,
            spellEffect = null
        )
        return GameState(
            activePlayerId = p1,
            priorityPlayerId = p1,
            turnOrder = listOf(p1)
        )
            .withEntity(spellEntity, ComponentContainer.of(
                cardComponent,
                OwnerComponent(p1),
                ControllerComponent(p1),
                spellComponent
            ))
            .copy(stack = listOf(spellEntity))
    }

    fun runStorm(state: GameState, spellEntity: EntityId, p1: EntityId) =
        StormCopyEffectExecutor(
            cardRegistry = CardRegistry(),
            targetFinder = TargetFinder()
        ).execute(
            state,
            StormCopyEffect(
                copyCount = 1,
                spellEffect = DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller),
                spellName = "X Bolt"
            ),
            EffectContext(sourceId = spellEntity, controllerId = p1, opponentId = null)
        )

    fun copyComponent(state: GameState): SpellOnStackComponent {
        val copyId = state.stack.single { id ->
            val c = state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        return state.getEntity(copyId)!!.get<SpellOnStackComponent>()!!
    }

    test("Storm copy inherits every cast-time decision from the source SpellOnStackComponent") {
        val p1 = EntityId.generate()
        val spellEntity = EntityId.generate()
        val sacrificedCreature = EntityId.generate()
        val damageTarget = EntityId.generate()
        val beheldCard = EntityId.generate()

        val source = SpellOnStackComponent(
            casterId = p1,
            xValue = 5,
            wasKicked = true,
            wasWarped = true,
            wasEvoked = true,
            sacrificedPermanents = listOf(sacrificedCreature),
            sacrificedPermanentSubtypes = mapOf(sacrificedCreature to setOf("Goblin")),
            damageDistribution = mapOf(damageTarget to 3),
            chosenCreatureType = "Elf",
            exiledCardCount = 2,
            castFromZone = Zone.HAND,
            beheldCards = listOf(beheldCard),
            manaSpentWhite = 1,
            manaSpentBlue = 2,
            manaSpentBlack = 3,
            manaSpentRed = 4,
            manaSpentGreen = 5,
            manaSpentColorless = 6
        )

        val result = runStorm(buildState(p1, spellEntity, source), spellEntity, p1)
        result.isSuccess shouldBe true

        val copy = copyComponent(result.state)
        copy.casterId shouldBe p1
        copy.xValue shouldBe 5
        copy.wasKicked shouldBe true
        copy.wasWarped shouldBe true
        copy.wasEvoked shouldBe true
        copy.sacrificedPermanents shouldBe listOf(sacrificedCreature)
        copy.sacrificedPermanentSubtypes shouldBe mapOf(sacrificedCreature to setOf("Goblin"))
        copy.damageDistribution shouldBe mapOf(damageTarget to 3)
        copy.chosenCreatureType shouldBe "Elf"
        copy.exiledCardCount shouldBe 2
        copy.castFromZone shouldBe Zone.HAND
        copy.beheldCards shouldBe listOf(beheldCard)
        copy.manaSpentWhite shouldBe 1
        copy.manaSpentBlue shouldBe 2
        copy.manaSpentBlack shouldBe 3
        copy.manaSpentRed shouldBe 4
        copy.manaSpentGreen shouldBe 5
        copy.manaSpentColorless shouldBe 6
    }

    test("Storm copy is a distinct entity, not an alias for the source") {
        val p1 = EntityId.generate()
        val spellEntity = EntityId.generate()
        val source = SpellOnStackComponent(casterId = p1, xValue = 4)

        val result = runStorm(buildState(p1, spellEntity, source), spellEntity, p1)
        result.isSuccess shouldBe true

        val copyId = result.state.stack.single { id ->
            val c = result.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        copyId shouldNotBe spellEntity
        result.state.stack.size shouldBe 2
    }

    test("Putting a copy on the stack does not re-emit payment events (707.10)") {
        val p1 = EntityId.generate()
        val spellEntity = EntityId.generate()
        val source = SpellOnStackComponent(
            casterId = p1,
            manaSpentRed = 2,
            manaSpentGreen = 1,
            sacrificedPermanents = listOf(EntityId.generate())
        )

        val result = runStorm(buildState(p1, spellEntity, source), spellEntity, p1)
        result.isSuccess shouldBe true

        result.events.none { it is ManaSpentEvent } shouldBe true
        result.events.none { it is SpellCastEvent } shouldBe true
        result.events.any { it is SpellCopiedEvent } shouldBe true
    }
})
