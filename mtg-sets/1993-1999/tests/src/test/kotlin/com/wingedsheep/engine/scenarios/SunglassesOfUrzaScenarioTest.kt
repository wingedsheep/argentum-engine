package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.mechanics.mana.ManaSpendingRules
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

class SunglassesOfUrzaScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Test Red Toll") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { effect = Effects.PayOrSuffer(Costs.pay.Mana("{R}"), Effects.LoseLife(3)) }
        })
        fun board() = scenario().withPlayers().withCardOnBattlefield(1, "Sunglasses of Urza")
            .withCardInHand(1, "Lightning Bolt").withCardInHand(1, "Test Red Toll")
            .withCardOnBattlefield(1, "Shivan Dragon")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardInHand(2, "Shatter").withLandsOnBattlefield(2, "Mountain", 2)
            .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Mountain")
            .withActivePlayer(1).build()
        test("white land auto pays a red spell while mana spent stays white") {
            val game = board()
            val result = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            result.error shouldBe null
            val spent = result.events.filterIsInstance<ManaSpentEvent>().single()
            spent.white shouldBe 1
            spent.red shouldBe 0
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
        }
        for (fromPool in listOf(true, false)) {
            test("${if (fromPool) "floating" else "explicitly selected"} white mana pays a red spell") {
                val game = board()
                val strategy = if (fromPool) {
                    game.state = game.state.updateEntity(game.player1Id) { it.with(ManaPoolComponent(white = 1)) }
                    PaymentStrategy.FromPool
                } else PaymentStrategy.Explicit(listOf(game.findPermanent("Plains")!!))
                val result = game.execute(CastSpell(game.player1Id, game.state.getHand(game.player1Id).first { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Lightning Bolt" },
                    targets = listOf(ChosenTarget.Player(game.player2Id)), paymentStrategy = strategy))
                result.error shouldBe null
                result.events.filterIsInstance<ManaSpentEvent>().last().white shouldBe 1
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17
            }
        }
        test("white pays red activated ability costs") {
            val game = board()
            val id = game.findPermanent("Shivan Dragon")!!
            val ability = cardRegistry.requireCard("Shivan Dragon").script.activatedAbilities.single()
            game.execute(ActivateAbility(game.player1Id, id, ability.id)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(id) shouldBe 6
        }
        test("resolution payment opens a usable white-source window for a red cost") {
            val game = board()
            game.castSpell(1, "Test Red Toll").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            val decision = game.state.pendingDecision as SelectManaSourcesDecision
            decision.autoPaySuggestion shouldBe listOf(game.findPermanent("Plains")!!)
            game.submitDecision(ManaSourcesSelectedResponse(decision.id, decision.autoPaySuggestion, autoPay = false)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
        }
        test("permission ends when Sunglasses leaves the battlefield") {
            val game = board()
            game.state = game.state.withPriority(game.player2Id)
            game.castSpell(2, "Shatter", game.findPermanent("Sunglasses of Urza")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Sunglasses of Urza") shouldBe true
            game.state = game.state.withPriority(game.player1Id)
            ManaSpendingRules.colors(game.state, game.player1Id) shouldBe emptyMap()
            (game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error != null) shouldBe true
        }
        test("text changes rewrite both color words without changing actual mana") {
            val game = board()
            val id = game.findPermanent("Sunglasses of Urza")!!
            game.state = game.state.updateEntity(id) { it.with(TextReplacementComponent(listOf(
                TextReplacement("red", "blue", TextReplacementCategory.COLOR_WORD)
            ))) }
            ManaSpendingRules.colors(game.state, game.player1Id)[Color.BLUE] shouldBe setOf(Color.BLUE, Color.WHITE)
            ManaSpendingRules.colors(game.state, game.player1Id)[Color.RED] shouldBe null
        }
    }
}
