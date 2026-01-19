package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ManaAbilityTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): GameState = GameState.newGame(
        listOf(
            player1Id to "Alice",
            player2Id to "Bob"
        )
    )

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)

    /**
     * Create an AbilitiesComponent with a basic land mana ability.
     */
    fun createForestAbilities(): AbilitiesComponent = AbilitiesComponent(
        activatedAbilities = listOf(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.GREEN),
                isManaAbility = true
            )
        )
    )

    context("mana ability execution") {
        test("tapping a forest adds green mana to pool") {
            var state = newGame()

            // Create a forest on the battlefield with abilities
            val forestId = EntityId.generate()
            val (_, s1) = state.createEntity(
                forestId,
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id),
                createForestAbilities()
            )
            val s2 = s1.addToZone(forestId, ZoneId.BATTLEFIELD)

            // Verify no mana in pool initially
            val initialMana = s2.getEntity(player1Id)?.get<ManaPoolComponent>()
            if (initialMana != null) {
                initialMana.pool.get(Color.GREEN) shouldBe 0
            }

            // Activate the mana ability
            val result = GameEngine.executeAction(
                s2,
                ActivateManaAbility(
                    sourceEntityId = forestId,
                    abilityIndex = 0,
                    playerId = player1Id
                )
            )
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val finalState = (result as GameActionResult.Success).state

            // Forest should be tapped
            finalState.getEntity(forestId)?.get<TappedComponent>().shouldNotBeNull()

            // Player should have 1 green mana
            val manaPool = finalState.getEntity(player1Id)?.get<ManaPoolComponent>()
            manaPool.shouldNotBeNull()
            manaPool.pool.get(Color.GREEN) shouldBe 1
        }

        test("cannot activate mana ability if already tapped") {
            var state = newGame()

            // Create a tapped forest on the battlefield
            val forestId = EntityId.generate()
            val (_, s1) = state.createEntity(
                forestId,
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id),
                createForestAbilities(),
                TappedComponent  // Already tapped
            )
            val s2 = s1.addToZone(forestId, ZoneId.BATTLEFIELD)

            // Try to activate the mana ability
            val result = GameEngine.executeAction(
                s2,
                ActivateManaAbility(
                    sourceEntityId = forestId,
                    abilityIndex = 0,
                    playerId = player1Id
                )
            )
            result.shouldBeInstanceOf<GameActionResult.Failure>()

            val failure = result as GameActionResult.Failure
            failure.reason shouldBe "Permanent is already tapped"
        }

        test("cannot activate opponent's mana ability") {
            var state = newGame()

            // Create a forest controlled by player2
            val forestId = EntityId.generate()
            val (_, s1) = state.createEntity(
                forestId,
                CardComponent(forestDef, player2Id),
                ControllerComponent(player2Id),
                createForestAbilities()
            )
            val s2 = s1.addToZone(forestId, ZoneId.BATTLEFIELD)

            // Player1 tries to activate player2's forest
            val result = GameEngine.executeAction(
                s2,
                ActivateManaAbility(
                    sourceEntityId = forestId,
                    abilityIndex = 0,
                    playerId = player1Id
                )
            )
            result.shouldBeInstanceOf<GameActionResult.Failure>()

            val failure = result as GameActionResult.Failure
            failure.reason shouldBe "Player does not control this permanent"
        }

        test("cannot activate mana ability of permanent not on battlefield") {
            var state = newGame()

            // Create a forest in hand (not on battlefield)
            val forestId = EntityId.generate()
            val (_, s1) = state.createEntity(
                forestId,
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id),
                createForestAbilities()
            )
            val s2 = s1.addToZone(forestId, ZoneId.hand(player1Id))

            // Try to activate the mana ability
            val result = GameEngine.executeAction(
                s2,
                ActivateManaAbility(
                    sourceEntityId = forestId,
                    abilityIndex = 0,
                    playerId = player1Id
                )
            )
            result.shouldBeInstanceOf<GameActionResult.Failure>()

            val failure = result as GameActionResult.Failure
            failure.reason shouldBe "Mana ability source must be on the battlefield"
        }
    }

    context("mana ability does not use the stack") {
        test("mana ability resolves immediately without touching stack") {
            var state = newGame()

            // Create a forest on the battlefield
            val forestId = EntityId.generate()
            val (_, s1) = state.createEntity(
                forestId,
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id),
                createForestAbilities()
            )
            val s2 = s1.addToZone(forestId, ZoneId.BATTLEFIELD)

            // Stack should be empty
            s2.getStack().isEmpty().shouldBeTrue()

            // Activate the mana ability
            val result = GameEngine.executeAction(
                s2,
                ActivateManaAbility(
                    sourceEntityId = forestId,
                    abilityIndex = 0,
                    playerId = player1Id
                )
            )
            result.shouldBeInstanceOf<GameActionResult.Success>()

            val finalState = (result as GameActionResult.Success).state

            // Stack should still be empty (mana ability doesn't use stack)
            finalState.getStack().isEmpty().shouldBeTrue()

            // But the effect should have resolved (mana added)
            val manaPool = finalState.getEntity(player1Id)?.get<ManaPoolComponent>()
            manaPool.shouldNotBeNull()
            manaPool.pool.get(Color.GREEN) shouldBe 1
        }
    }

    context("AbilitiesComponent") {
        test("correctly identifies mana abilities") {
            val manaAbility = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.GREEN),
                isManaAbility = true
            )

            val regularAbility = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = DrawCardsEffect(1),
                isManaAbility = false
            )

            val abilities = AbilitiesComponent(
                activatedAbilities = listOf(manaAbility, regularAbility)
            )

            abilities.hasManaAbilities.shouldBeTrue()
            abilities.manaAbilities.size shouldBe 1
            abilities.manaAbilities.first() shouldBe manaAbility
            abilities.nonManaActivatedAbilities.size shouldBe 1
            abilities.nonManaActivatedAbilities.first() shouldBe regularAbility
        }

        test("getManaAbility returns ability at correct index") {
            val ability1 = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.GREEN),
                isManaAbility = true
            )
            val ability2 = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.BLUE),
                isManaAbility = true
            )

            val abilities = AbilitiesComponent(
                activatedAbilities = listOf(ability1, ability2)
            )

            abilities.getManaAbility(0) shouldBe ability1
            abilities.getManaAbility(1) shouldBe ability2
            abilities.getManaAbility(2).shouldBeNull()
        }
    }

    context("isManaAbility flag") {
        test("activated abilities default to isManaAbility = false") {
            val ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = DrawCardsEffect(1)
            )

            ability.isManaAbility.shouldBeFalse()
        }

        test("mana abilities have isManaAbility = true") {
            val ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = AddManaEffect(Color.RED),
                isManaAbility = true
            )

            ability.isManaAbility.shouldBeTrue()
        }
    }

    context("CardScriptBuilder mana abilities") {
        test("manaAbility builder creates correct ability") {
            val script = cardScript("Test Land") {
                manaAbility(AddManaEffect(Color.BLUE))
            }

            script.activatedAbilities.size shouldBe 1
            val ability = script.activatedAbilities.first()
            ability.isManaAbility.shouldBeTrue()
            ability.cost shouldBe AbilityCost.Tap
            ability.effect.shouldBeInstanceOf<AddManaEffect>()
            (ability.effect as AddManaEffect).color shouldBe Color.BLUE
        }
    }
})
