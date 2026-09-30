package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ChannelScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Test mana payment") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell { effect = Effects.PayOrSuffer(Costs.pay.Mana("{2}"), Effects.LoseLife(3)) }
        })
        fun board(life: Int = 20) = scenario().withPlayers().withLifeTotal(1, life)
            .withCardInHand(1, "Channel").withCardInHand(1, "Test mana payment")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(1).build()

        test("repeated life payments add colorless immediately after the source enters graveyard") {
            val game = board()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Channel") shouldBe true
            val id = game.state.playerActionPermissions.single().id
            repeat(3) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            game.getLifeTotal(1) shouldBe 17
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 3
            game.state.stack shouldBe emptyList()
            game.state.priorityPlayerId shouldBe game.player1Id
        }
        test("last life may be paid and the player then loses") {
            val game = board(1)
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            game.execute(TakePlayerAction(game.player1Id, game.state.playerActionPermissions.single().id)).error shouldBe null
            game.getLifeTotal(1) shouldBe 0
            game.state.gameOver shouldBe true
        }
        test("cannot use another player's permission or use it without priority") {
            val game = board()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            val id = game.state.playerActionPermissions.single().id
            game.execute(TakePlayerAction(game.player2Id, id)).error!! shouldContain "another player"
            game.state = game.state.withPriority(game.player2Id)
            game.execute(TakePlayerAction(game.player1Id, id)).error!! shouldContain "priority"
            game.getLifeTotal(1) shouldBe 20
        }
        test("life-funded mana opens a resolution payment window and restores it after each action") {
            val game = board()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            val id = game.state.playerActionPermissions.single().id
            services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{2}")) shouldBe true
            game.castSpell(1, "Test mana payment").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            val window = game.state.pendingDecision as SelectManaSourcesDecision
            services.legalActionEnumerator.enumerateManaAbilities(game.state, game.player1Id)
                .any { it.action == TakePlayerAction(game.player1Id, id) } shouldBe true
            repeat(2) {
                game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
                game.state.pendingDecision!!.id shouldBe window.id
            }
            game.submitDecision(ManaSourcesSelectedResponse(window.id, emptyList(), autoPay = false)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 18
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 0
        }
        test("casting opens a mana window and never automatically pays life") {
            cardRegistry.register(card("Test costly spell") {
                manaCost = "{2}"; typeLine = "Instant"
                staticAbility {
                    ability = ModifySpellCost(SpellCostTarget.SelfCast, CostModification.ReduceGeneric(1),
                        CostGating.OnlyIf(Conditions.CompareAmounts(DynamicAmount.YourLifeTotal, ComparisonOperator.LT, 20)))
                }
                spell { effect = Effects.GainLife(3) }
            })
            val game = scenario().withPlayers().withCardInHand(1, "Channel")
                .withCardInHand(1, "Test costly spell").withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1).build()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            val id = game.state.playerActionPermissions.single().id
            game.castSpell(1, "Test costly spell").error shouldBe null
            val window = game.state.pendingDecision as SelectManaSourcesDecision
            game.getLifeTotal(1) shouldBe 20
            game.state.stack shouldBe emptyList()
            repeat(2) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            game.submitDecision(ManaSourcesSelectedResponse(window.id)).error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 0
        }
        test("ability activation can produce mana before committing its costs") {
            val testCard = card("Test costly activation") {
                typeLine = "Artifact"
                staticAbility {
                    ability = ReduceActivatedAbilityCost(GroupFilter(GameObjectFilter.Artifact.youControl()),
                        DynamicAmount.Subtract(DynamicAmount.Fixed(20), DynamicAmount.YourLifeTotal))
                }
                activatedAbility { cost = Costs.Mana("{2}"); effect = Effects.GainLife(3) }
            }
            cardRegistry.register(testCard)
            val game = scenario().withPlayers().withCardInHand(1, "Channel")
                .withCardOnBattlefield(1, "Test costly activation").withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1).build()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            val id = game.state.playerActionPermissions.single().id
            val ability = testCard.script.activatedAbilities.single()
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Test costly activation")!!, ability.id)).error shouldBe null
            val window = game.state.pendingDecision as SelectManaSourcesDecision
            repeat(2) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            game.submitDecision(ManaSourcesSelectedResponse(window.id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 0
        }
        test("multiple Channel permissions share one life budget in affordability") {
            val game = board()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            val grant = game.state.playerActionPermissions.single()
            game.state = game.state.copy(playerActionPermissions = listOf(grant, grant.copy(id = "second")))
            services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{21}")) shouldBe false
            services.manaSolver.canPay(game.state, game.player1Id, ManaCost.parse("{20}")) shouldBe true
        }
        test("cancelling the casting window keeps the spell in hand and spends no life") {
            cardRegistry.register(card("Test cancellable spell") { manaCost = "{2}"; typeLine = "Artifact" })
            val game = scenario().withPlayers().withCardInHand(1, "Channel")
                .withCardInHand(1, "Test cancellable spell").withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1).build()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Test cancellable spell").error shouldBe null
            val window = game.state.pendingDecision as SelectManaSourcesDecision
            game.submitDecision(ManaSourcesSelectedResponse(window.id, declined = true)).error shouldBe null
            game.isInHand(1, "Test cancellable spell") shouldBe true
            game.state.stack shouldBe emptyList()
            game.getLifeTotal(1) shouldBe 20
        }

        test("end of turn removes the permission and previously floated mana") {
            val game = board()
            game.castSpell(1, "Channel").error shouldBe null
            game.resolveStack()
            val id = game.state.playerActionPermissions.single().id
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.passUntilPhase(com.wingedsheep.sdk.core.Phase.BEGINNING, com.wingedsheep.sdk.core.Step.UPKEEP)
            game.state.playerActionPermissions shouldBe emptyList()
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 0
        }
    }
}
