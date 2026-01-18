package com.wingedsheep.rulesengine.targeting

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TargetingTest : FunSpec({

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

    fun createCreatureWithKeyword(name: String, controllerId: String, keyword: Keyword): CardInstance {
        val def = CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W)),
            subtypes = setOf(Subtype.HUMAN),
            power = 2,
            toughness = 2,
            keywords = setOf(keyword)
        )
        return CardInstance.create(def, controllerId)
    }

    fun createCreatureWithColor(name: String, controllerId: String, color: Color): CardInstance {
        // Colors are derived from mana cost, so use appropriate symbol
        val manaSymbol = when (color) {
            Color.WHITE -> ManaSymbol.W
            Color.BLUE -> ManaSymbol.U
            Color.BLACK -> ManaSymbol.B
            Color.RED -> ManaSymbol.R
            Color.GREEN -> ManaSymbol.G
        }
        val def = CardDefinition.creature(
            name = name,
            manaCost = ManaCost(listOf(manaSymbol)),
            subtypes = setOf(Subtype.HUMAN),
            power = 2,
            toughness = 2
        )
        return CardInstance.create(def, controllerId)
    }

    fun createArtifact(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.artifact(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.generic(3)))
        )
        return CardInstance.create(def, controllerId)
    }

    fun createEnchantment(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.enchantment(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.W, ManaSymbol.W))
        )
        return CardInstance.create(def, controllerId)
    }

    fun createInstant(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.instant(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.R)),
            oracleText = "Test instant"
        )
        return CardInstance.create(def, controllerId)
    }

    fun createSorcery(name: String, controllerId: String): CardInstance {
        val def = CardDefinition.sorcery(
            name = name,
            manaCost = ManaCost(listOf(ManaSymbol.R)),
            oracleText = "Test sorcery"
        )
        return CardInstance.create(def, controllerId)
    }

    val player1Id = PlayerId.of("player1")
    val player2Id = PlayerId.of("player2")

    context("TargetPlayer requirement") {
        test("any player is a valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetPlayer()

            requirement.isValidTarget(Target.player(player1Id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.player(player2Id), state, player1Id, null) shouldBe true
        }

        test("card target is not valid for player requirement") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetPlayer()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe false
        }

        test("non-existent player is not valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetPlayer()

            requirement.isValidTarget(Target.player("player3"), state, player1Id, null) shouldBe false
        }
    }

    context("TargetOpponent requirement") {
        test("opponent is a valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetOpponent()

            requirement.isValidTarget(Target.player(player2Id), state, player1Id, null) shouldBe true
        }

        test("self is not a valid target for opponent requirement") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetOpponent()

            requirement.isValidTarget(Target.player(player1Id), state, player1Id, null) shouldBe false
        }
    }

    context("TargetCreature requirement") {
        test("creature on battlefield is valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetCreature()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe true
        }

        test("player is not valid target for creature requirement") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetCreature()

            requirement.isValidTarget(Target.player(player1Id), state, player1Id, null) shouldBe false
        }

        test("artifact is not valid target for creature requirement") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val artifact = createArtifact("Sol Ring", "player1")
            state = state.updateBattlefield { it.addToTop(artifact) }

            val requirement = TargetCreature()

            requirement.isValidTarget(Target.card(artifact.id), state, player1Id, null) shouldBe false
        }

        test("creature you control filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val myCreature = createCreature("My Bear", "player1")
            val oppCreature = createCreature("Opp Bear", "player2")
            state = state.updateBattlefield { it.addToTop(myCreature).addToTop(oppCreature) }

            val requirement = TargetCreature(filter = CreatureTargetFilter.YouControl)

            requirement.isValidTarget(Target.card(myCreature.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(oppCreature.id), state, player1Id, null) shouldBe false
        }

        test("creature opponent controls filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val myCreature = createCreature("My Bear", "player1")
            val oppCreature = createCreature("Opp Bear", "player2")
            state = state.updateBattlefield { it.addToTop(myCreature).addToTop(oppCreature) }

            val requirement = TargetCreature(filter = CreatureTargetFilter.OpponentControls)

            requirement.isValidTarget(Target.card(myCreature.id), state, player1Id, null) shouldBe false
            requirement.isValidTarget(Target.card(oppCreature.id), state, player1Id, null) shouldBe true
        }

        test("tapped creature filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val tappedCreature = createCreature("Tapped Bear", "player1").tap()
            val untappedCreature = createCreature("Untapped Bear", "player1")
            state = state.updateBattlefield { it.addToTop(tappedCreature).addToTop(untappedCreature) }

            val requirement = TargetCreature(filter = CreatureTargetFilter.Tapped)

            requirement.isValidTarget(Target.card(tappedCreature.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(untappedCreature.id), state, player1Id, null) shouldBe false
        }

        test("untapped creature filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val tappedCreature = createCreature("Tapped Bear", "player1").tap()
            val untappedCreature = createCreature("Untapped Bear", "player1")
            state = state.updateBattlefield { it.addToTop(tappedCreature).addToTop(untappedCreature) }

            val requirement = TargetCreature(filter = CreatureTargetFilter.Untapped)

            requirement.isValidTarget(Target.card(tappedCreature.id), state, player1Id, null) shouldBe false
            requirement.isValidTarget(Target.card(untappedCreature.id), state, player1Id, null) shouldBe true
        }

        test("creature with keyword filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val flyingCreature = createCreatureWithKeyword("Bird", "player1", Keyword.FLYING)
            val nonFlyingCreature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(flyingCreature).addToTop(nonFlyingCreature) }

            val requirement = TargetCreature(filter = CreatureTargetFilter.WithKeyword(Keyword.FLYING))

            requirement.isValidTarget(Target.card(flyingCreature.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(nonFlyingCreature.id), state, player1Id, null) shouldBe false
        }

        test("creature with power at most filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val smallCreature = createCreature("Small", "player1", power = 2, toughness = 2)
            val bigCreature = createCreature("Big", "player1", power = 5, toughness = 5)
            state = state.updateBattlefield { it.addToTop(smallCreature).addToTop(bigCreature) }

            val requirement = TargetCreature(filter = CreatureTargetFilter.WithPowerAtMost(3))

            requirement.isValidTarget(Target.card(smallCreature.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(bigCreature.id), state, player1Id, null) shouldBe false
        }

        test("creature with power at least filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val smallCreature = createCreature("Small", "player1", power = 2, toughness = 2)
            val bigCreature = createCreature("Big", "player1", power = 5, toughness = 5)
            state = state.updateBattlefield { it.addToTop(smallCreature).addToTop(bigCreature) }

            val requirement = TargetCreature(filter = CreatureTargetFilter.WithPowerAtLeast(4))

            requirement.isValidTarget(Target.card(smallCreature.id), state, player1Id, null) shouldBe false
            requirement.isValidTarget(Target.card(bigCreature.id), state, player1Id, null) shouldBe true
        }

        test("combined filter (And)") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val tappedSmall = createCreature("Tapped Small", "player1", power = 2, toughness = 2).tap()
            val untappedSmall = createCreature("Untapped Small", "player1", power = 2, toughness = 2)
            val tappedBig = createCreature("Tapped Big", "player1", power = 5, toughness = 5).tap()
            state = state.updateBattlefield {
                it.addToTop(tappedSmall).addToTop(untappedSmall).addToTop(tappedBig)
            }

            val requirement = TargetCreature(
                filter = CreatureTargetFilter.And(
                    listOf(CreatureTargetFilter.Tapped, CreatureTargetFilter.WithPowerAtMost(3))
                )
            )

            requirement.isValidTarget(Target.card(tappedSmall.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(untappedSmall.id), state, player1Id, null) shouldBe false
            requirement.isValidTarget(Target.card(tappedBig.id), state, player1Id, null) shouldBe false
        }
    }

    context("TargetPermanent requirement") {
        test("any permanent is valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            val artifact = createArtifact("Sol Ring", "player1")
            val enchantment = createEnchantment("Glorious Anthem", "player1")
            state = state.updateBattlefield {
                it.addToTop(creature).addToTop(artifact).addToTop(enchantment)
            }

            val requirement = TargetPermanent()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(artifact.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(enchantment.id), state, player1Id, null) shouldBe true
        }

        test("nonland permanent filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            val landDef = CardDefinition.basicLand("Forest", Subtype.FOREST)
            val land = CardInstance.create(landDef, "player1")
            state = state.updateBattlefield { it.addToTop(creature).addToTop(land) }

            val requirement = TargetPermanent(filter = PermanentTargetFilter.NonLand)

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(land.id), state, player1Id, null) shouldBe false
        }

        test("artifact filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            val artifact = createArtifact("Sol Ring", "player1")
            state = state.updateBattlefield { it.addToTop(creature).addToTop(artifact) }

            val requirement = TargetPermanent(filter = PermanentTargetFilter.Artifact)

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe false
            requirement.isValidTarget(Target.card(artifact.id), state, player1Id, null) shouldBe true
        }
    }

    context("AnyTarget requirement") {
        test("player is valid any target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = AnyTarget()

            requirement.isValidTarget(Target.player(player1Id), state, player1Id, null) shouldBe true
        }

        test("creature is valid any target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = AnyTarget()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe true
        }

        test("non-creature permanent is not valid any target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val artifact = createArtifact("Sol Ring", "player1")
            state = state.updateBattlefield { it.addToTop(artifact) }

            val requirement = AnyTarget()

            requirement.isValidTarget(Target.card(artifact.id), state, player1Id, null) shouldBe false
        }
    }

    context("TargetCreatureOrPlayer requirement") {
        test("creature is valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetCreatureOrPlayer()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe true
        }

        test("player is valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetCreatureOrPlayer()

            requirement.isValidTarget(Target.player(player2Id), state, player1Id, null) shouldBe true
        }

        test("artifact is not valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val artifact = createArtifact("Sol Ring", "player1")
            state = state.updateBattlefield { it.addToTop(artifact) }

            val requirement = TargetCreatureOrPlayer()

            requirement.isValidTarget(Target.card(artifact.id), state, player1Id, null) shouldBe false
        }
    }

    context("TargetCardInGraveyard requirement") {
        test("card in graveyard is valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Dead Bear", "player1")
            state = state.updatePlayer(player1Id) { player ->
                player.copy(graveyard = player.graveyard.addToTop(creature))
            }

            val requirement = TargetCardInGraveyard()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe true
        }

        test("card on battlefield is not valid graveyard target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Living Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetCardInGraveyard()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe false
        }

        test("creature card filter in graveyard") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Dead Bear", "player1")
            val instant = createInstant("Lightning Bolt", "player1")
            state = state.updatePlayer(player1Id) { player ->
                player.copy(graveyard = player.graveyard.addToTop(creature).addToTop(instant))
            }

            val requirement = TargetCardInGraveyard(filter = GraveyardCardFilter.Creature)

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(instant.id), state, player1Id, null) shouldBe false
        }
    }

    context("TargetSpell requirement") {
        test("spell on stack is valid target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val instant = createInstant("Lightning Bolt", "player1")
            state = state.updateStack { it.addToTop(instant) }

            val requirement = TargetSpell()

            requirement.isValidTarget(Target.card(instant.id), state, player1Id, null) shouldBe true
        }

        test("creature on battlefield is not valid spell target") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetSpell()

            requirement.isValidTarget(Target.card(creature.id), state, player1Id, null) shouldBe false
        }

        test("creature spell filter") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creatureSpell = createCreature("Bear", "player1")
            val instant = createInstant("Lightning Bolt", "player1")
            state = state.updateStack { it.addToTop(creatureSpell).addToTop(instant) }

            val requirement = TargetSpell(filter = SpellTargetFilter.Creature)

            requirement.isValidTarget(Target.card(creatureSpell.id), state, player1Id, null) shouldBe true
            requirement.isValidTarget(Target.card(instant.id), state, player1Id, null) shouldBe false
        }
    }

    context("TargetOther requirement") {
        test("excludes the source") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val source = createCreature("Source", "player1")
            val other = createCreature("Other", "player1")
            state = state.updateBattlefield { it.addToTop(source).addToTop(other) }

            val requirement = TargetOther(TargetCreature())

            requirement.isValidTarget(Target.card(source.id), state, player1Id, source.id) shouldBe false
            requirement.isValidTarget(Target.card(other.id), state, player1Id, source.id) shouldBe true
        }
    }

    context("TargetSelection") {
        test("isComplete when all required targets selected") {
            val requirement = TargetCreature(count = 2)
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card("card1"))
                .addTarget(0, Target.card("card2"))

            selection.isComplete shouldBe true
        }

        test("not complete when targets missing") {
            val requirement = TargetCreature(count = 2)
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card("card1"))

            selection.isComplete shouldBe false
        }

        test("optional targets allow empty selection") {
            val requirement = TargetCreature(count = 2, optional = true)
            val selection = TargetSelection.forRequirement(requirement)

            selection.isComplete shouldBe true
        }

        test("allTargets returns flat list") {
            val requirements = listOf(TargetCreature(), TargetPlayer())
            val selection = TargetSelection.forRequirements(requirements)
                .addTarget(0, Target.card("card1"))
                .addTarget(1, Target.player("player1"))

            selection.allTargets shouldHaveSize 2
        }

        test("removeTarget works correctly") {
            val target = Target.card("card1")
            val selection = TargetSelection.forRequirement(TargetCreature())
                .addTarget(0, target)
                .removeTarget(0, target)

            selection.selectedTargets[0] shouldBe emptyList()
        }

        test("clearTargets clears specific requirement") {
            val selection = TargetSelection.forRequirements(listOf(TargetCreature(), TargetPlayer()))
                .addTarget(0, Target.card("card1"))
                .addTarget(1, Target.player("player1"))
                .clearTargets(0)

            selection.selectedTargets[0] shouldBe null
            selection.selectedTargets[1]!! shouldHaveSize 1
        }
    }

    context("TargetValidator.validateSelection") {
        test("valid selection passes validation") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetCreature()
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card(creature.id))

            val result = TargetValidator.validateSelection(selection, state, player1Id, null)

            result shouldBe TargetValidator.ValidationResult.Valid
        }

        test("not enough targets fails validation") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetCreature(count = 2)
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card("card1"))

            val result = TargetValidator.validateSelection(selection, state, player1Id, null)

            result.shouldBeInstanceOf<TargetValidator.ValidationResult.Invalid>()
        }

        test("too many targets fails validation") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player1")
            state = state.updateBattlefield { it.addToTop(creature1).addToTop(creature2) }

            val requirement = TargetCreature(count = 1)
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card(creature1.id))
                .addTarget(0, Target.card(creature2.id))

            val result = TargetValidator.validateSelection(selection, state, player1Id, null)

            result.shouldBeInstanceOf<TargetValidator.ValidationResult.Invalid>()
        }

        test("invalid target fails validation") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val artifact = createArtifact("Sol Ring", "player1")
            state = state.updateBattlefield { it.addToTop(artifact) }

            val requirement = TargetCreature()
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card(artifact.id))

            val result = TargetValidator.validateSelection(selection, state, player1Id, null)

            result.shouldBeInstanceOf<TargetValidator.ValidationResult.Invalid>()
        }
    }

    context("TargetValidator.validateOnResolution") {
        test("spell with valid target resolves") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetCreature()
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card(creature.id))

            val result = TargetValidator.validateOnResolution(selection, state, player1Id, null)

            result.valid shouldBe true
            result.shouldFizzle shouldBe false
            result.validTargets[0]!! shouldHaveSize 1
        }

        test("spell fizzles when all targets are now illegal") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            // Target a creature that no longer exists
            val requirement = TargetCreature()
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card(CardId("nonexistent")))

            val result = TargetValidator.validateOnResolution(selection, state, player1Id, null)

            result.valid shouldBe false
            result.shouldFizzle shouldBe true
        }

        test("spell resolves with partial valid targets") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Living Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            // Multiple targets, one still valid, one not
            val requirement = TargetCreature(count = 2, optional = true)
            val selection = TargetSelection.forRequirement(requirement)
                .addTarget(0, Target.card(creature.id))
                .addTarget(0, Target.card(CardId("dead")))

            val result = TargetValidator.validateOnResolution(selection, state, player1Id, null)

            result.valid shouldBe true
            result.shouldFizzle shouldBe false
            result.validTargets[0]!! shouldHaveSize 1
            result.validTargets[0]!!.first() shouldBe Target.card(creature.id)
        }

        test("spell with no targets always resolves") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val selection = TargetSelection.none()

            val result = TargetValidator.validateOnResolution(selection, state, player1Id, null)

            result.valid shouldBe true
            result.shouldFizzle shouldBe false
        }
    }

    context("TargetValidator.getLegalTargets") {
        test("returns all legal creature targets") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature1 = createCreature("Bear 1", "player1")
            val creature2 = createCreature("Bear 2", "player2")
            val artifact = createArtifact("Sol Ring", "player1")
            state = state.updateBattlefield {
                it.addToTop(creature1).addToTop(creature2).addToTop(artifact)
            }

            val requirement = TargetCreature()
            val legalTargets = TargetValidator.getLegalTargets(requirement, state, player1Id, null)

            legalTargets shouldHaveSize 2
            legalTargets shouldContain Target.card(creature1.id)
            legalTargets shouldContain Target.card(creature2.id)
            legalTargets shouldNotContain Target.card(artifact.id)
        }

        test("returns all legal player targets") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetPlayer()
            val legalTargets = TargetValidator.getLegalTargets(requirement, state, player1Id, null)

            legalTargets shouldHaveSize 2
            legalTargets shouldContain Target.player(player1Id)
            legalTargets shouldContain Target.player(player2Id)
        }

        test("returns cards from graveyard") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val deadCreature = createCreature("Dead Bear", "player1")
            state = state.updatePlayer(player1Id) { player ->
                player.copy(graveyard = player.graveyard.addToTop(deadCreature))
            }

            val requirement = TargetCardInGraveyard()
            val legalTargets = TargetValidator.getLegalTargets(requirement, state, player1Id, null)

            legalTargets shouldHaveSize 1
            legalTargets shouldContain Target.card(deadCreature.id)
        }

        test("returns spells on stack") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val spell = createInstant("Lightning Bolt", "player1")
            state = state.updateStack { it.addToTop(spell) }

            val requirement = TargetSpell()
            val legalTargets = TargetValidator.getLegalTargets(requirement, state, player1Id, null)

            legalTargets shouldHaveSize 1
            legalTargets shouldContain Target.card(spell.id)
        }
    }

    context("TargetValidator.hasLegalTargets") {
        test("returns true when legal targets exist") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirement = TargetCreature()

            TargetValidator.hasLegalTargets(requirement, state, player1Id, null) shouldBe true
        }

        test("returns false when no legal targets exist") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirement = TargetCreature()

            TargetValidator.hasLegalTargets(requirement, state, player1Id, null) shouldBe false
        }
    }

    context("TargetValidator.canTarget") {
        test("returns true when all mandatory requirements have legal targets") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val creature = createCreature("Bear", "player1")
            state = state.updateBattlefield { it.addToTop(creature) }

            val requirements = listOf(TargetCreature(), TargetPlayer())

            TargetValidator.canTarget(requirements, state, player1Id, null) shouldBe true
        }

        test("returns false when mandatory requirement has no legal targets") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirements = listOf(TargetCreature())  // No creatures on battlefield

            TargetValidator.canTarget(requirements, state, player1Id, null) shouldBe false
        }

        test("returns true when optional requirement has no legal targets") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            val state = createTestGameState(player1, player2)

            val requirements = listOf(TargetCreature(optional = true))

            TargetValidator.canTarget(requirements, state, player1Id, null) shouldBe true
        }
    }

    context("Hexproof and Shroud") {
        test("creature with shroud cannot be targeted") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val shroudCreature = createCreatureWithKeyword("Shroud Bear", "player1", Keyword.SHROUD)
            state = state.updateBattlefield { it.addToTop(shroudCreature) }

            shroudCreature.canBeTargetedBy(player1Id) shouldBe false
            shroudCreature.canBeTargetedBy(player2Id) shouldBe false
        }

        test("creature with hexproof cannot be targeted by opponent") {
            val player1 = createTestPlayer("player1")
            val player2 = createTestPlayer("player2")
            var state = createTestGameState(player1, player2)

            val hexproofCreature = createCreatureWithKeyword("Hexproof Bear", "player1", Keyword.HEXPROOF)
            state = state.updateBattlefield { it.addToTop(hexproofCreature) }

            hexproofCreature.canBeTargetedBy(player1Id) shouldBe true
            hexproofCreature.canBeTargetedBy(player2Id) shouldBe false
        }
    }

    context("Target conversion") {
        test("Target converts to ChosenTarget and back") {
            val cardTarget = Target.card("card1")
            val playerTarget = Target.player("player1")

            cardTarget.toChosenTarget().toTarget() shouldBe cardTarget
            playerTarget.toChosenTarget().toTarget() shouldBe playerTarget
        }
    }

    context("TargetingInfo") {
        test("tracks source and selection") {
            val info = TargetingInfo(
                sourceId = CardId("spell1"),
                sourceName = "Lightning Bolt",
                controllerId = player1Id,
                requirements = listOf(TargetCreatureOrPlayer())
            )

            info.isComplete shouldBe false
            info.allSelectedTargets.shouldBeEmpty()

            val updated = info.withTarget(0, Target.player(player2Id))
            updated.isComplete shouldBe true
            updated.allSelectedTargets shouldHaveSize 1
        }

        test("can remove targets") {
            val target = Target.player(player2Id)
            val info = TargetingInfo(
                sourceId = CardId("spell1"),
                sourceName = "Lightning Bolt",
                controllerId = player1Id,
                requirements = listOf(TargetCreatureOrPlayer())
            ).withTarget(0, target).withoutTarget(0, target)

            info.isComplete shouldBe false
            info.allSelectedTargets.shouldBeEmpty()
        }
    }
})
