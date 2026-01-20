package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Rule 613.8 dependency resolution.
 *
 * Key scenarios:
 * 1. Effect A says "Green creatures get +1/+1"
 * 2. Effect B says "All creatures become green"
 * 3. B must be applied before A, regardless of timestamps
 */
class DependencyResolverTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    context("DependencyResolver.analyzeFilterDependencies") {
        test("Self filter has no dependencies") {
            val resolver = DependencyResolver(newGame())
            val modifier = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = EntityId.generate(),
                timestamp = 1,
                modification = Modification.ModifyPT(1, 1),
                filter = ModifierFilter.Self
            )

            // Self filter doesn't depend on any characteristics
            val result = resolver.sortWithDependencies(listOf(modifier))
            result shouldBe listOf(modifier)
        }

        test("All(Creatures) filter depends on CREATURE type") {
            val resolver = DependencyResolver(newGame())

            // Create two modifiers that might have dependencies
            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            // Effect A: "Creatures get +1/+1" (depends on creature type)
            val modifierA = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceA,
                timestamp = 1,  // Earlier timestamp
                modification = Modification.AddSubtype(Subtype.BEAR),
                filter = ModifierFilter.All(EntityCriteria.Creatures)
            )

            // Effect B: "This becomes a creature" (modifies type)
            val modifierB = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceB,
                timestamp = 2,  // Later timestamp
                modification = Modification.AddType(CardType.CREATURE),
                filter = ModifierFilter.Self
            )

            // B should come before A due to dependency, despite having later timestamp
            val result = resolver.sortWithDependencies(listOf(modifierA, modifierB))
            result.indexOf(modifierB) shouldBe 0  // B first
            result.indexOf(modifierA) shouldBe 1  // A second
        }
    }

    context("Color-based dependencies") {
        test("AddColor must be applied before color-dependent filter") {
            val resolver = DependencyResolver(newGame())

            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            // Effect A: "Green creatures get +1/+1" (depends on green color)
            // This is in layer 7c but filter checks color
            val modifierA = Modifier(
                layer = Layer.COLOR,
                sourceId = sourceA,
                timestamp = 1,  // Earlier
                modification = Modification.AddColor(Color.WHITE),  // Some effect on green things
                filter = ModifierFilter.All(EntityCriteria.WithColor(Color.GREEN))
            )

            // Effect B: "All creatures become green" (modifies color)
            val modifierB = Modifier(
                layer = Layer.COLOR,
                sourceId = sourceB,
                timestamp = 2,  // Later
                modification = Modification.SetColors(setOf(Color.GREEN)),
                filter = ModifierFilter.All(EntityCriteria.Creatures)
            )

            val result = resolver.sortWithDependencies(listOf(modifierA, modifierB))

            // B should come first because A depends on green color
            result.indexOf(modifierB) shouldBe 0
            result.indexOf(modifierA) shouldBe 1
        }
    }

    context("Keyword-based dependencies") {
        test("AddKeyword must be applied before keyword-dependent filter") {
            val resolver = DependencyResolver(newGame())

            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            // Effect A: "Creatures with flying get +1/+1" (depends on flying keyword)
            val modifierA = Modifier(
                layer = Layer.ABILITY,
                sourceId = sourceA,
                timestamp = 1,
                modification = Modification.AddKeyword(Keyword.VIGILANCE),
                filter = ModifierFilter.All(EntityCriteria.WithKeyword(Keyword.FLYING))
            )

            // Effect B: "All creatures gain flying"
            val modifierB = Modifier(
                layer = Layer.ABILITY,
                sourceId = sourceB,
                timestamp = 2,
                modification = Modification.AddKeyword(Keyword.FLYING),
                filter = ModifierFilter.All(EntityCriteria.Creatures)
            )

            val result = resolver.sortWithDependencies(listOf(modifierA, modifierB))

            // B should come first
            result.indexOf(modifierB) shouldBe 0
            result.indexOf(modifierA) shouldBe 1
        }
    }

    context("Subtype-based dependencies") {
        test("AddSubtype must be applied before subtype-dependent filter") {
            val resolver = DependencyResolver(newGame())

            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            // Effect A: "Elves get +1/+1" (depends on Elf subtype)
            val modifierA = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceA,
                timestamp = 1,
                modification = Modification.AddSubtype(Subtype.WARRIOR),
                filter = ModifierFilter.All(EntityCriteria.WithSubtype(Subtype.ELF))
            )

            // Effect B: "All creatures become Elves"
            val modifierB = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceB,
                timestamp = 2,
                modification = Modification.AddSubtype(Subtype.ELF),
                filter = ModifierFilter.All(EntityCriteria.Creatures)
            )

            val result = resolver.sortWithDependencies(listOf(modifierA, modifierB))

            // B should come first
            result.indexOf(modifierB) shouldBe 0
            result.indexOf(modifierA) shouldBe 1
        }
    }

    context("No dependency cases") {
        test("unrelated modifiers keep timestamp order") {
            val resolver = DependencyResolver(newGame())

            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            // Effect A: Self-modification (no filter dependency)
            val modifierA = Modifier(
                layer = Layer.ABILITY,
                sourceId = sourceA,
                timestamp = 1,
                modification = Modification.AddKeyword(Keyword.FLYING),
                filter = ModifierFilter.Self
            )

            // Effect B: Self-modification (no filter dependency)
            val modifierB = Modifier(
                layer = Layer.ABILITY,
                sourceId = sourceB,
                timestamp = 2,
                modification = Modification.AddKeyword(Keyword.HASTE),
                filter = ModifierFilter.Self
            )

            val result = resolver.sortWithDependencies(listOf(modifierA, modifierB))

            // Should maintain timestamp order
            result shouldBe listOf(modifierA, modifierB)
        }

        test("different layers have no dependency") {
            val resolver = DependencyResolver(newGame())

            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            // Effect A in Layer 5 (Color)
            val modifierA = Modifier(
                layer = Layer.COLOR,
                sourceId = sourceA,
                timestamp = 1,
                modification = Modification.AddColor(Color.GREEN),
                filter = ModifierFilter.Self
            )

            // Effect B in Layer 6 (Ability)
            val modifierB = Modifier(
                layer = Layer.ABILITY,
                sourceId = sourceB,
                timestamp = 2,
                modification = Modification.AddKeyword(Keyword.FLYING),
                filter = ModifierFilter.All(EntityCriteria.WithColor(Color.GREEN))
            )

            // These are in different layers, so dependency resolution within a layer
            // doesn't matter - they'll be applied in layer order regardless
            val colorResult = resolver.sortWithDependencies(listOf(modifierA))
            val abilityResult = resolver.sortWithDependencies(listOf(modifierB))

            colorResult shouldBe listOf(modifierA)
            abilityResult shouldBe listOf(modifierB)
        }
    }

    context("Complex dependency chains") {
        test("transitive dependencies are resolved") {
            val resolver = DependencyResolver(newGame())

            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()
            val sourceC = EntityId.generate()

            // A depends on B, B depends on C
            // A: "Warriors get vigilance" (depends on Warrior subtype)
            val modifierA = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceA,
                timestamp = 1,
                modification = Modification.AddSubtype(Subtype.KNIGHT),
                filter = ModifierFilter.All(EntityCriteria.WithSubtype(Subtype.WARRIOR))
            )

            // B: "Elves become Warriors" (modifies Warrior, depends on Elf)
            val modifierB = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceB,
                timestamp = 2,
                modification = Modification.AddSubtype(Subtype.WARRIOR),
                filter = ModifierFilter.All(EntityCriteria.WithSubtype(Subtype.ELF))
            )

            // C: "All creatures become Elves" (modifies Elf)
            val modifierC = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceC,
                timestamp = 3,
                modification = Modification.AddSubtype(Subtype.ELF),
                filter = ModifierFilter.All(EntityCriteria.Creatures)
            )

            val result = resolver.sortWithDependencies(listOf(modifierA, modifierB, modifierC))

            // Order should be: C -> B -> A
            result.indexOf(modifierC) shouldBe 0
            result.indexOf(modifierB) shouldBe 1
            result.indexOf(modifierA) shouldBe 2
        }
    }

    context("StateProjector integration") {
        test("projector applies self-modifying effects correctly") {
            var state = newGame()

            // Create a green creature
            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Effect: "This creature gets +1/+1" (self-targeting, no dependencies)
            val pumpSelf = Modifier(
                layer = Layer.PT_MODIFY,
                sourceId = creatureId,
                timestamp = 1,
                modification = Modification.ModifyPT(1, 1),
                filter = ModifierFilter.Self
            )

            val projector = StateProjector(state, listOf(pumpSelf))
            val view = projector.getView(creatureId)

            view!!.power shouldBe 3  // 2 + 1
            view.toughness shouldBe 3  // 2 + 1
        }

        test("same-layer dependencies with self-filter are resolved") {
            var state = newGame()

            // Create a creature
            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            // Effect A: "This creature becomes an Elf in addition to its other types"
            val becomeElf = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceA,
                timestamp = 1,
                modification = Modification.AddSubtype(Subtype.ELF),
                filter = ModifierFilter.Specific(creatureId)
            )

            // Effect B: "This creature also becomes a Warrior"
            val becomeWarrior = Modifier(
                layer = Layer.TYPE,
                sourceId = sourceB,
                timestamp = 2,
                modification = Modification.AddSubtype(Subtype.WARRIOR),
                filter = ModifierFilter.Specific(creatureId)
            )

            // Both target the same creature with Specific filter
            // Should apply in timestamp order since no characteristic dependency
            val projector = StateProjector(state, listOf(becomeElf, becomeWarrior))
            val view = projector.getView(creatureId)

            // Creature should have original Bear plus Elf and Warrior
            view!!.subtypes shouldContain Subtype.BEAR
            view.subtypes shouldContain Subtype.ELF
            view.subtypes shouldContain Subtype.WARRIOR
        }

        test("color modifications are applied in layer 5") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            // Effect: "This creature becomes blue"
            val makeBlue = Modifier(
                layer = Layer.COLOR,
                sourceId = EntityId.generate(),
                timestamp = 1,
                modification = Modification.SetColors(setOf(Color.BLUE)),
                filter = ModifierFilter.Specific(creatureId)
            )

            val projector = StateProjector(state, listOf(makeBlue))
            val view = projector.getView(creatureId)

            // Creature should be blue (not green)
            view!!.colors shouldContain Color.BLUE
            view.colors.contains(Color.GREEN) shouldBe false
        }
    }

    context("Dependency resolution limitations") {
        /**
         * Note: Full CR 613.8 dependency resolution requires re-evaluating target
         * resolution after each layer is applied, which is a complex iterative process.
         *
         * The current implementation correctly orders modifiers WITHIN a layer based
         * on dependencies (e.g., if effect A affects "green creatures" and effect B
         * makes creatures green, B is applied first within the same layer).
         *
         * However, cross-layer target resolution (where Layer 5 color changes affect
         * Layer 7c P/T modifiers) requires the full iterative approach which is
         * noted as future work.
         */
        test("documents dependency resolution scope") {
            // This test documents the current implementation scope
            val resolver = DependencyResolver(newGame())

            // Within-layer dependency ordering is implemented
            val sourceA = EntityId.generate()
            val sourceB = EntityId.generate()

            val modifierA = Modifier(
                layer = Layer.COLOR,
                sourceId = sourceA,
                timestamp = 1,
                modification = Modification.AddColor(Color.WHITE),
                filter = ModifierFilter.All(EntityCriteria.WithColor(Color.GREEN))
            )

            val modifierB = Modifier(
                layer = Layer.COLOR,
                sourceId = sourceB,
                timestamp = 2,
                modification = Modification.SetColors(setOf(Color.GREEN)),
                filter = ModifierFilter.All(EntityCriteria.Creatures)
            )

            // B should come before A due to dependency
            val result = resolver.sortWithDependencies(listOf(modifierA, modifierB))
            result.indexOf(modifierB) shouldBe 0
            result.indexOf(modifierA) shouldBe 1
        }
    }
})
