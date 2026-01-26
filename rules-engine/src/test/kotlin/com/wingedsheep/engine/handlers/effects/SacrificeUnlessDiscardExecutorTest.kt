package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.removal.SacrificeUnlessDiscardExecutor
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.SacrificeUnlessDiscardEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SacrificeUnlessDiscardExecutorTest : FunSpec({

    test("executor type should match SacrificeUnlessDiscardEffect") {
        val executor = SacrificeUnlessDiscardExecutor()
        executor.effectType shouldBe SacrificeUnlessDiscardEffect::class
    }

    test("effect registry should contain SacrificeUnlessDiscardEffect executor") {
        val registry = EffectExecutorRegistry()
        registry.executorCount() shouldNotBe 0
    }

    test("effect class should be identical") {
        val effect = SacrificeUnlessDiscardEffect(CardFilter.CreatureCard)
        val executor = SacrificeUnlessDiscardExecutor()
        effect::class shouldBe executor.effectType
    }

    test("registry execute should find and use SacrificeUnlessDiscardEffect executor") {
        val registry = EffectExecutorRegistry()
        val effect: Effect = SacrificeUnlessDiscardEffect(CardFilter.CreatureCard)

        // Create a minimal game state with player, source on battlefield, and creature in hand
        val playerId = EntityId.of("player-1")
        val sourceId = EntityId.of("mercenary-knight")
        val creatureInHandId = EntityId.of("creature-in-hand")

        val sourceCard = CardComponent(
            cardDefinitionId = "Mercenary Knight",
            name = "Mercenary Knight",
            manaCost = ManaCost.parse("{2}{B}"),
            typeLine = TypeLine.parse("Creature — Human Mercenary Knight"),
            oracleText = "When Mercenary Knight enters the battlefield, sacrifice it unless you discard a creature card.",
            colors = emptySet(),
            baseKeywords = emptySet(),
            baseStats = null,
            ownerId = playerId,
            spellEffect = null
        )

        val creatureCard = CardComponent(
            cardDefinitionId = "Grizzly Bears",
            name = "Grizzly Bears",
            manaCost = ManaCost.parse("{1}{G}"),
            typeLine = TypeLine.parse("Creature — Bear"),
            oracleText = "",
            colors = emptySet(),
            baseKeywords = emptySet(),
            baseStats = null,
            ownerId = playerId,
            spellEffect = null
        )

        val sourceContainer = ComponentContainer.of(
            sourceCard,
            OwnerComponent(playerId),
            ControllerComponent(playerId)
        )

        val creatureContainer = ComponentContainer.of(
            creatureCard,
            OwnerComponent(playerId),
            ControllerComponent(playerId)
        )

        var state = GameState()
            .withEntity(sourceId, sourceContainer)
            .withEntity(creatureInHandId, creatureContainer)

        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val handZone = ZoneKey(playerId, ZoneType.HAND)

        state = state.copy(
            zones = mapOf(
                battlefieldZone to listOf(sourceId),
                handZone to listOf(creatureInHandId)
            )
        )

        val context = EffectContext(
            sourceId = sourceId,
            controllerId = playerId,
            opponentId = EntityId.of("player-2"),
            targets = emptyList()
        )

        // Execute the effect through the registry
        val result = registry.execute(state, effect, context)

        // The result should be paused (awaiting decision to discard)
        result.isPaused shouldBe true
        result.pendingDecision shouldNotBe null
    }
})
