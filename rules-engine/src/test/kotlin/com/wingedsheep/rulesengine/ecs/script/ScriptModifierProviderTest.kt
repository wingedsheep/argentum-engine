package com.wingedsheep.rulesengine.ecs.script

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.AttachedToComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.layers.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ScriptModifierProviderTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val swordDef = CardDefinition.equipment(
        name = "Trusty Sword",
        manaCost = ManaCost.parse("{2}"),
        equipCost = ManaCost.parse("{1}")
    )

    val anthemDef = CardDefinition.enchantment(
        name = "Glorious Anthem",
        manaCost = ManaCost.parse("{1}{W}{W}")
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    beforeTest {
        Modifier.resetTimestamps()
    }

    context("equipment stat modifiers") {
        test("generates P/T modifier for equipped creature") {
            // Setup: Create a creature with equipment attached
            var state = newGame()

            // Create bear
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Create sword attached to bear
            val (swordId, state3) = state.createEntity(
                EntityId.generate(),
                CardComponent(swordDef, player1Id),
                ControllerComponent(player1Id),
                AttachedToComponent(bearId)
            )
            state = state3.addToZone(swordId, ZoneId.BATTLEFIELD)

            // Register static ability for the sword
            val registry = AbilityRegistry()
            registry.registerStaticAbility(
                "Trusty Sword",
                ModifyStats(2, 0, StaticTarget.AttachedCreature)
            )

            // Get modifiers
            val provider = ScriptModifierProvider(registry)
            val modifiers = provider.getModifiers(state)

            modifiers shouldHaveSize 1
            val modifier = modifiers.first()
            modifier.layer shouldBe Layer.PT_MODIFY
            modifier.sourceId shouldBe swordId
            modifier.modification.shouldBeInstanceOf<Modification.ModifyPT>()
            val modPT = modifier.modification as Modification.ModifyPT
            modPT.powerDelta shouldBe 2
            modPT.toughnessDelta shouldBe 0
            modifier.filter.shouldBeInstanceOf<ModifierFilter.Specific>()
            (modifier.filter as ModifierFilter.Specific).entityId shouldBe bearId
        }
    }

    context("equipment keyword modifiers") {
        test("generates keyword modifier for equipped creature") {
            var state = newGame()

            // Create bear
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Create sword attached to bear
            val (swordId, state3) = state.createEntity(
                EntityId.generate(),
                CardComponent(swordDef, player1Id),
                ControllerComponent(player1Id),
                AttachedToComponent(bearId)
            )
            state = state3.addToZone(swordId, ZoneId.BATTLEFIELD)

            // Register keyword grant ability
            val registry = AbilityRegistry()
            registry.registerStaticAbility(
                "Trusty Sword",
                GrantKeyword(Keyword.TRAMPLE, StaticTarget.AttachedCreature)
            )

            val provider = ScriptModifierProvider(registry)
            val modifiers = provider.getModifiers(state)

            modifiers shouldHaveSize 1
            val modifier = modifiers.first()
            modifier.layer shouldBe Layer.ABILITY
            modifier.modification.shouldBeInstanceOf<Modification.AddKeyword>()
            (modifier.modification as Modification.AddKeyword).keyword shouldBe Keyword.TRAMPLE
        }
    }

    context("global effect modifiers") {
        test("generates modifier for all creatures get +1/+1") {
            var state = newGame()

            // Create anthem enchantment
            val (anthemId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(anthemDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(anthemId, ZoneId.BATTLEFIELD)

            // Register global effect
            val registry = AbilityRegistry()
            registry.registerStaticAbility(
                "Glorious Anthem",
                GlobalEffect(GlobalEffectType.ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE)
            )

            val provider = ScriptModifierProvider(registry)
            val modifiers = provider.getModifiers(state)

            modifiers shouldHaveSize 1
            val modifier = modifiers.first()
            modifier.layer shouldBe Layer.PT_MODIFY
            modifier.modification.shouldBeInstanceOf<Modification.ModifyPT>()
            val modPT = modifier.modification as Modification.ModifyPT
            modPT.powerDelta shouldBe 1
            modPT.toughnessDelta shouldBe 1
            modifier.filter.shouldBeInstanceOf<ModifierFilter.All>()
        }

        test("generates modifier for all creatures have flying") {
            var state = newGame()

            val levitationDef = CardDefinition.enchantment(
                name = "Levitation",
                manaCost = ManaCost.parse("{2}{U}{U}")
            )

            val (levitationId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(levitationDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(levitationId, ZoneId.BATTLEFIELD)

            val registry = AbilityRegistry()
            registry.registerStaticAbility(
                "Levitation",
                GlobalEffect(GlobalEffectType.ALL_CREATURES_HAVE_FLYING)
            )

            val provider = ScriptModifierProvider(registry)
            val modifiers = provider.getModifiers(state)

            modifiers shouldHaveSize 1
            val modifier = modifiers.first()
            modifier.layer shouldBe Layer.ABILITY
            modifier.modification.shouldBeInstanceOf<Modification.AddKeyword>()
            (modifier.modification as Modification.AddKeyword).keyword shouldBe Keyword.FLYING
        }
    }

    context("integration with StateProjector") {
        test("projector applies modifiers from scripts") {
            var state = newGame()

            // Create bear
            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Create sword attached to bear
            val (swordId, state3) = state.createEntity(
                EntityId.generate(),
                CardComponent(swordDef, player1Id),
                ControllerComponent(player1Id),
                AttachedToComponent(bearId)
            )
            state = state3.addToZone(swordId, ZoneId.BATTLEFIELD)

            // Register abilities
            val registry = AbilityRegistry()
            registry.registerStaticAbilities(
                "Trusty Sword",
                listOf(
                    ModifyStats(2, 1, StaticTarget.AttachedCreature),
                    GrantKeyword(Keyword.TRAMPLE, StaticTarget.AttachedCreature)
                )
            )

            // Create projector with script modifier provider
            val provider = ScriptModifierProvider(registry)
            val projector = StateProjector.forState(state, provider)

            val bearView = projector.getView(bearId)!!

            bearView.power shouldBe 4  // 2 base + 2 from sword
            bearView.toughness shouldBe 3  // 2 base + 1 from sword
            bearView.hasKeyword(Keyword.TRAMPLE) shouldBe true
        }
    }

    context("no abilities") {
        test("returns empty list when no abilities registered") {
            var state = newGame()

            val (bearId, state2) = state.createEntity(
                EntityId.generate(),
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(bearId, ZoneId.BATTLEFIELD)

            val registry = AbilityRegistry()
            val provider = ScriptModifierProvider(registry)
            val modifiers = provider.getModifiers(state)

            modifiers.shouldBeEmpty()
        }
    }
})
