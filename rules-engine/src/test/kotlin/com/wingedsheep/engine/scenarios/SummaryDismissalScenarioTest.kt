package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AbilityCounteredEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.stack.CounterAllOnStackExecutor
import com.wingedsheep.engine.handlers.effects.stack.ExileSpellsOnStackExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterAllOnStackEffect
import com.wingedsheep.sdk.scripting.effects.ExileSpellsOnStackEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class SummaryDismissalScenarioTest : FunSpec({
    test("exiles all other spells and counters all abilities") {
        val caster = EntityId.generate()
        val opponent = EntityId.generate()
        val summary = EntityId.generate()
        val otherSpell = EntityId.generate()
        val ability = EntityId.generate()
        val abilitySource = EntityId.generate()

        fun spellCard(id: EntityId, name: String, owner: EntityId) = id to ComponentContainer.of(
            CardComponent(
                cardDefinitionId = name,
                name = name,
                manaCost = ManaCost(emptyList()),
                typeLine = TypeLine(cardTypes = setOf(CardType.INSTANT)),
                ownerId = owner,
            ),
            SpellOnStackComponent(casterId = owner),
        )

        val entities = mapOf(
            spellCard(summary, "Summary Dismissal", caster),
            spellCard(otherSpell, "Uncounterable Spell", opponent),
            ability to ComponentContainer.of(
                TriggeredAbilityOnStackComponent(
                    sourceId = abilitySource,
                    sourceName = "Test Source",
                    controllerId = opponent,
                    effect = Effects.DrawCards(1),
                    description = "Draw a card",
                ),
            ),
        )
        val state = GameState(
            entities = entities,
            stack = listOf(otherSpell, ability, summary),
            turnOrder = listOf(caster, opponent),
        )
        val context = EffectContext(sourceId = summary, controllerId = caster)
        val registry = CardRegistry()

        val exiled = ExileSpellsOnStackExecutor(registry).execute(
            state,
            ExileSpellsOnStackEffect(),
            context,
        )
        exiled.state.stack shouldContain summary
        exiled.state.stack shouldContain ability
        exiled.state.stack shouldNotContain otherSpell
        exiled.state.getZone(ZoneKey(opponent, Zone.EXILE)) shouldContain otherSpell
        exiled.events.filterIsInstance<ZoneChangeEvent>().single().toZone shouldBe Zone.EXILE

        val cleared = CounterAllOnStackExecutor(registry).execute(
            exiled.state,
            CounterAllOnStackEffect(spells = false, abilities = true, opponentsOnly = false),
            context,
        )
        cleared.state.stack shouldBe listOf(summary)
        cleared.events.filterIsInstance<AbilityCounteredEvent>().size shouldBe 1
    }
})
