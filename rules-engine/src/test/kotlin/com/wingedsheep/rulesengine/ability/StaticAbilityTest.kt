package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.ManaSymbol
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain

class StaticAbilityTest : FunSpec({

    fun createTestPlayer(id: String): Player {
        return Player.create(PlayerId.of(id), "${id}'s Deck")
    }

    fun createTestGameState(player1: Player, player2: Player): GameState {
        return GameState.newGame(player1, player2)
    }

    fun createCreature(name: String, controllerId: String, power: Int = 2, toughness: Int = 2): CardInstance {
        val def = CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W)),
            subtypes = setOf(Subtype.HUMAN),
            power = power,
            toughness = toughness
        )
        return CardInstance.create(def, controllerId)
    }

    fun createEquipment(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.equipment(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.generic(2))),
            equipCost = ManaCost(listOf(ManaSymbol.generic(1))),
            oracleText = "Equipped creature gets +2/+0"
        )
        return CardInstance.create(def, controllerId)
    }

    fun createEnchantment(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.enchantment(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W, ManaSymbol.W)),
            oracleText = "Creatures you control get +1/+1"
        )
        return CardInstance.create(def, controllerId)
    }

    context("GrantKeyword static ability") {
        test("grants keyword to attached creature") {
            val ability = GrantKeyword(Keyword.FLYING, StaticTarget.AttachedCreature)
            ability.description shouldBe "Grants flying"
        }

        test("static ability applier grants keywords from equipment") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1")
            val equipment = createEquipment("Maul of the Skyclaves", "player1")
                .attachTo(creature.id)

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }

            val registry = AbilityRegistry()
            registry.registerStaticAbility("Maul of the Skyclaves", GrantKeyword(Keyword.FLYING))

            val grantedKeywords = StaticAbilityApplier.getGrantedKeywords(creature, state, registry)
            grantedKeywords shouldContain Keyword.FLYING
        }
    }

    context("ModifyStats static ability") {
        test("modifies stats description for positive bonuses") {
            val ability = ModifyStats(2, 2, StaticTarget.AttachedCreature)
            ability.description shouldBe "+2/+2"
        }

        test("modifies stats description for negative bonuses") {
            val ability = ModifyStats(-1, -1, StaticTarget.AttachedCreature)
            ability.description shouldBe "-1/-1"
        }

        test("modifies stats description for mixed bonuses") {
            val ability = ModifyStats(3, -2, StaticTarget.AttachedCreature)
            ability.description shouldBe "+3/-2"
        }

        test("static ability applier applies stat bonuses from equipment") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1", power = 2, toughness = 2)
            val equipment = createEquipment("Bonesplitter", "player1")
                .attachTo(creature.id)

            state = state.updateBattlefield { it.addToTop(creature).addToTop(equipment) }

            val registry = AbilityRegistry()
            registry.registerStaticAbility("Bonesplitter", ModifyStats(2, 0))

            val (powerBonus, toughnessBonus) = StaticAbilityApplier.getStatBonuses(creature, state, registry)
            powerBonus shouldBe 2
            toughnessBonus shouldBe 0
        }

        test("multiple equipment stack bonuses") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Grizzly Bears", "player1")
            val equipment1 = createEquipment("Bonesplitter", "player1")
                .attachTo(creature.id)
            val equipment2Def = CardDefinition.equipment(
                name = "Short Sword",
                manaCost = ManaCost(listOf(ManaSymbol.generic(1))),
                equipCost = ManaCost(listOf(ManaSymbol.generic(1)))
            )
            val equipment2 = CardInstance.create(equipment2Def, "player1")
                .attachTo(creature.id)

            state = state.updateBattlefield {
                it.addToTop(creature).addToTop(equipment1).addToTop(equipment2)
            }

            val registry = AbilityRegistry()
            registry.registerStaticAbility("Bonesplitter", ModifyStats(2, 0))
            registry.registerStaticAbility("Short Sword", ModifyStats(1, 1))

            val (powerBonus, toughnessBonus) = StaticAbilityApplier.getStatBonuses(creature, state, registry)
            powerBonus shouldBe 3
            toughnessBonus shouldBe 1
        }
    }

    context("GlobalEffect static ability") {
        test("all creatures get bonus from global enchantment") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player2")
            val enchantment = createEnchantment("Glorious Anthem", "player1")

            state = state.updateBattlefield {
                it.addToTop(creature1).addToTop(creature2).addToTop(enchantment)
            }

            val registry = AbilityRegistry()
            registry.registerStaticAbility(
                "Glorious Anthem",
                GlobalEffect(GlobalEffectType.YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE, CreatureFilter.YouControl)
            )

            // Player 1's creature gets the bonus
            val (p1Power, p1Tough) = StaticAbilityApplier.getStatBonuses(creature1, state, registry)
            p1Power shouldBe 1
            p1Tough shouldBe 1

            // Player 2's creature doesn't get the bonus
            val (p2Power, p2Tough) = StaticAbilityApplier.getStatBonuses(creature2, state, registry)
            p2Power shouldBe 0
            p2Tough shouldBe 0
        }

        test("global enchantment grants keywords to all creatures") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player2")
            val enchantmentDef = CardDefinition.enchantment(
                name = "Levitation",
                manaCost = ManaCost(listOf(ManaSymbol.Colored(com.wingedsheep.rulesengine.core.Color.BLUE), ManaSymbol.Colored(com.wingedsheep.rulesengine.core.Color.BLUE))),
                oracleText = "All creatures have flying"
            )
            val enchantment = CardInstance.create(enchantmentDef, "player1")

            state = state.updateBattlefield {
                it.addToTop(creature1).addToTop(creature2).addToTop(enchantment)
            }

            val registry = AbilityRegistry()
            registry.registerStaticAbility(
                "Levitation",
                GlobalEffect(GlobalEffectType.ALL_CREATURES_HAVE_FLYING, CreatureFilter.All)
            )

            // Both creatures get flying
            val keywords1 = StaticAbilityApplier.getGrantedKeywords(creature1, state, registry)
            val keywords2 = StaticAbilityApplier.getGrantedKeywords(creature2, state, registry)

            keywords1 shouldContain Keyword.FLYING
            keywords2 shouldContain Keyword.FLYING
        }
    }

    context("CreatureFilter") {
        test("YouControl filter matches controller's creatures only") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player2")

            CreatureFilter.YouControl.matches(creature1, PlayerId.of("player1"), state) shouldBe true
            CreatureFilter.YouControl.matches(creature2, PlayerId.of("player1"), state) shouldBe false
        }

        test("OpponentsControl filter matches opponent's creatures only") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player2")

            CreatureFilter.OpponentsControl.matches(creature1, PlayerId.of("player1"), state) shouldBe false
            CreatureFilter.OpponentsControl.matches(creature2, PlayerId.of("player1"), state) shouldBe true
        }

        test("All filter matches all creatures") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player2")

            CreatureFilter.All.matches(creature1, PlayerId.of("player1"), state) shouldBe true
            CreatureFilter.All.matches(creature2, PlayerId.of("player1"), state) shouldBe true
        }

        test("WithKeyword filter matches creatures with keyword") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val flyingDef = CardDefinition.creature(
                name = "Flying Creature",
                manaCost = ManaCost(listOf(ManaSymbol.W)),
                subtypes = setOf(Subtype("Bird")),
                power = 1,
                toughness = 1,
                keywords = setOf(Keyword.FLYING)
            )
            val flyingCreature = CardInstance.create(flyingDef, "player1")
            val groundCreature = createCreature("Ground Creature", "player1")

            val filter = CreatureFilter.WithKeyword(Keyword.FLYING)
            filter.matches(flyingCreature, PlayerId.of("player1"), state) shouldBe true
            filter.matches(groundCreature, PlayerId.of("player1"), state) shouldBe false
        }
    }
})
