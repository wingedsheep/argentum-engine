package com.wingedsheep.rulesengine.sets.ecl

import com.wingedsheep.rulesengine.ability.OnSpellCast
import com.wingedsheep.rulesengine.ability.SpellTypeFilter
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.event.CardDefinitionAbilityRegistry
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.event.GameEvent
import com.wingedsheep.rulesengine.ecs.event.TriggerDetector
import com.wingedsheep.rulesengine.ecs.layers.StateProjector
import com.wingedsheep.rulesengine.ecs.script.EffectExecutor
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AdeptWatershaperTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val executor = EffectExecutor()

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    // A dummy non-creature spell to trigger Prowess
    val optDef = CardDefinition.instant(
        name = "Opt",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "Scry 1. Draw a card."
    )

    context("Adept Watershaper Script") {
        val script = LorwynEclipsedSet.getCardScript("Adept Watershaper")!!
        val definition = LorwynEclipsedSet.getCardDefinition("Adept Watershaper")!!

        test("has correct stats and keywords") {
            definition.creatureStats?.basePower shouldBe 1
            definition.creatureStats?.baseToughness shouldBe 1
            definition.keywords shouldContain Keyword.PROWESS
        }

        test("has Prowess trigger structure") {
            script.triggeredAbilities shouldHaveSize 1
            val trigger = script.triggeredAbilities.first().trigger

            trigger.shouldBeInstanceOf<OnSpellCast>()
            trigger.controllerOnly shouldBe true
            trigger.spellType shouldBe SpellTypeFilter.NONCREATURE
        }
    }

    context("Game Integration") {

        test("Activated ability grants Flying") {
            // 1. Setup
            var state = GameState.newGame(listOf(player1Id to "Alice", EntityId.of("player2") to "Bob"))
            val (bearId, state1) = state.createEntity(EntityId.generate(), CardComponent(bearDef, player1Id), ControllerComponent(player1Id))
            val (sourceId, state2) = state1.createEntity(EntityId.generate(), CardComponent(LorwynEclipsedSet.getCardDefinition("Adept Watershaper")!!, player1Id), ControllerComponent(player1Id))
            state = state2.addToZone(bearId, ZoneId.BATTLEFIELD).addToZone(sourceId, ZoneId.BATTLEFIELD)

            // 2. Execute
            val script = LorwynEclipsedSet.getCardScript("Adept Watershaper")!!
            val ability = script.activatedAbilities.first()
            val context = ExecutionContext(player1Id, sourceId, targets = listOf(ChosenTarget.Permanent(bearId)))
            val result = executor.execute(state, ability.effect, context)

            // 3. Verify
            val projector = StateProjector(result.state, result.temporaryModifiers)
            projector.getView(bearId)!!.hasKeyword(Keyword.FLYING) shouldBe true
        }

        test("Prowess triggers on non-creature spell cast") {
            // 1. Setup Game State
            var state = GameState.newGame(listOf(player1Id to "Alice", EntityId.of("player2") to "Bob"))

            // Create Watershaper
            val watershaperDef = LorwynEclipsedSet.getCardDefinition("Adept Watershaper")!!
            val watershaperScript = LorwynEclipsedSet.getCardScript("Adept Watershaper")!!
            val (watershaperId, state1) = state.createEntity(EntityId.generate(), CardComponent(watershaperDef, player1Id), ControllerComponent(player1Id))
            state = state1.addToZone(watershaperId, ZoneId.BATTLEFIELD)

            // Create Trigger Detector with registry
            val abilityRegistry = CardDefinitionAbilityRegistry()
            abilityRegistry.register(watershaperDef.name, watershaperScript.triggeredAbilities)
            val triggerDetector = TriggerDetector()

            // 2. Simulate Spell Cast Event
            val spellId = EntityId.generate()
            val castEvent = GameEvent.SpellCast(
                spellId = spellId,
                spellName = "Opt",
                casterId = player1Id,
                isCreatureSpell = false,
                isInstantOrSorcery = true
            )

            // 3. Detect Triggers
            val pendingTriggers = triggerDetector.detectTriggers(state, listOf(castEvent), abilityRegistry)

            pendingTriggers shouldHaveSize 1
            pendingTriggers.first().sourceId shouldBe watershaperId

            // 4. Resolve Trigger (Execute Effect)
            val trigger = pendingTriggers.first()
            val context = ExecutionContext(player1Id, watershaperId)
            val result = executor.execute(state, trigger.ability.effect, context)

            // 5. Verify State Projection (+1/+1)
            val projector = StateProjector(result.state, result.temporaryModifiers)
            val view = projector.getView(watershaperId)

            view!!.power shouldBe 2 // 1 base + 1 prowess
            view.toughness shouldBe 2 // 1 base + 1 prowess
        }

        test("Prowess does NOT trigger on creature spell") {
            // 1. Setup Game State
            var state = GameState.newGame(listOf(player1Id to "Alice", EntityId.of("player2") to "Bob"))
            val watershaperDef = LorwynEclipsedSet.getCardDefinition("Adept Watershaper")!!
            val watershaperScript = LorwynEclipsedSet.getCardScript("Adept Watershaper")!!
            val (watershaperId, state1) = state.createEntity(EntityId.generate(), CardComponent(watershaperDef, player1Id), ControllerComponent(player1Id))
            state = state1.addToZone(watershaperId, ZoneId.BATTLEFIELD)

            val abilityRegistry = CardDefinitionAbilityRegistry()
            abilityRegistry.register(watershaperDef.name, watershaperScript.triggeredAbilities)
            val triggerDetector = TriggerDetector()

            // 2. Simulate Creature Spell Cast
            val spellId = EntityId.generate()
            val castEvent = GameEvent.SpellCast(
                spellId = spellId,
                spellName = "Grizzly Bears",
                casterId = player1Id,
                isCreatureSpell = true, // Creature!
                isInstantOrSorcery = false
            )

            // 3. Detect Triggers
            val pendingTriggers = triggerDetector.detectTriggers(state, listOf(castEvent), abilityRegistry)

            pendingTriggers shouldHaveSize 0
        }
    }
})
