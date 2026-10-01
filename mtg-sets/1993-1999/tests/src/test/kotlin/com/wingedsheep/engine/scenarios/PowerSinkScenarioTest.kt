package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PowerSinkScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Sink blank land") { typeLine = "Land" })
        cardRegistry.register(card("Sink colorless land") { typeLine = "Land"; activatedAbility { cost = AbilityCost.Tap; manaAbility = true; effect = Effects.AddColorlessMana(1) } })
        cardRegistry.register(card("Sink mana artifact") { typeLine = "Artifact"; activatedAbility { cost = AbilityCost.Tap; manaAbility = true; effect = Effects.AddMana(Color.BLUE) } })
        cardRegistry.register(card("Sink test spell") { manaCost = "{0}"; typeLine = "Sorcery"; spell { effect = Effects.GainLife(3) } })
        cardRegistry.register(card("Sink uncounterable spell") { manaCost = "{0}"; typeLine = "Sorcery"; cantBeCountered = true; spell { effect = Effects.GainLife(3) } })

        cardRegistry.register(card("Sink land grant") {
            typeLine = "Enchantment"
            staticAbility { ability = GrantActivatedAbility(
                ActivatedAbility(AbilityId("sink_grant_mana"), AbilityCost.Tap, Effects.AddColorlessMana(1), isManaAbility = true),
                GroupFilter(GameObjectFilter.Land)
            ) }
        })

        fun board(targetName: String = "Sink test spell", mana: Int = 0): TestGame {
            val game = scenario().withPlayers().withCardInHand(1, "Power Sink")
                .withLandsOnBattlefield(1, "Island", 8)
                .withCardInHand(2, targetName).withCardOnBattlefield(2, "Sink blank land")
                .withCardOnBattlefield(2, "Sink colorless land").withCardOnBattlefield(2, "Sink mana artifact")
                .withCardOnBattlefield(2, "Forest").withActivePlayer(2).build()
            game.castSpell(2, targetName).error shouldBe null
            game.passPriority().error shouldBe null
            game.state = game.state.updateEntity(game.player2Id) { it.with(ManaPoolComponent(colorless = mana)) }
            return game
        }
        fun cast(game: TestGame, x: Int) {
            val card = game.state.getHand(game.player1Id).single { game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Power Sink" }
            val target = game.state.stack.single()
            game.execute(CastSpell(game.player1Id, card, listOf(ChosenTarget.Spell(target)), xValue = x)).error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }
        }
        fun tapped(game: TestGame, name: String) = game.state.getEntity(game.findPermanent(name)!!)!!.has<TappedComponent>()

        test("unable to pay counters and taps only mana lands, then empties floating mana") {
            val game = board(mana = 1)
            cast(game, 6)
            game.state.pendingDecision shouldBe null
            game.state.stack shouldBe emptyList()
            game.state.getGraveyard(game.player2Id).any { game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Sink test spell" } shouldBe true
            tapped(game, "Forest") shouldBe true
            tapped(game, "Sink colorless land") shouldBe true
            tapped(game, "Sink blank land") shouldBe false
            tapped(game, "Sink mana artifact") shouldBe false
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!.total shouldBe 0
            game.getLifeTotal(2) shouldBe 20
        }
        test("declining an affordable payment takes the same tap and mana-loss branch") {
            val game = board(mana = 2)
            cast(game, 2)
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            tapped(game, "Forest") shouldBe true
            tapped(game, "Sink blank land") shouldBe false
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!.total shouldBe 0
            game.getLifeTotal(2) shouldBe 20
        }
        test("paying leaves unused lands and excess floating mana alone and allows the spell to resolve") {
            val game = board(mana = 3)
            cast(game, 2)
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(true).error shouldBe null
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!.total shouldBe 1
            tapped(game, "Forest") shouldBe false
            tapped(game, "Sink colorless land") shouldBe false
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 23
        }
        test("payment can activate untapped land and artifact mana sources") {
            val game = scenario().withPlayers().withCardInHand(1, "Power Sink")
                .withLandsOnBattlefield(1, "Island", 8).withCardInHand(2, "Sink test spell")
                .withCardOnBattlefield(2, "Sink colorless land").withCardOnBattlefield(2, "Sink mana artifact")
                .withCardOnBattlefield(2, "Forest").withActivePlayer(2).build()
            game.castSpell(2, "Sink test spell").error shouldBe null
            game.passPriority().error shouldBe null
            game.state = game.state.updateEntity(game.player2Id) { it.with(ManaPoolComponent()) }
            cast(game, 3)
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(true).error shouldBe null
            tapped(game, "Forest") shouldBe true
            tapped(game, "Sink colorless land") shouldBe true
            tapped(game, "Sink mana artifact") shouldBe true
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!.total shouldBe 0
            game.resolveStack().forEach { it.error shouldBe null }
            game.getLifeTotal(2) shouldBe 23
        }

        test("a land granted a mana ability is tapped by the decline branch") {
            val game = scenario().withPlayers().withCardInHand(1, "Power Sink")
                .withLandsOnBattlefield(1, "Island", 8).withCardInHand(2, "Sink test spell")
                .withCardOnBattlefield(2, "Sink blank land").withCardOnBattlefield(2, "Sink land grant")
                .withActivePlayer(2).build()
            game.castSpell(2, "Sink test spell").error shouldBe null
            game.passPriority().error shouldBe null
            cast(game, 6)
            tapped(game, "Sink blank land") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }

        test("zero payment can be accepted without tapping or emptying mana") {
            val game = board(mana = 2)
            cast(game, 0)
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            tapped(game, "Forest") shouldBe false
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!.total shouldBe 2
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 23
        }
        test("uncounterable target still taps mana lands and loses mana when payment is declined") {
            val game = board("Sink uncounterable spell", 1)
            cast(game, 6)
            game.state.pendingDecision shouldBe null
            tapped(game, "Forest") shouldBe true
            game.state.getEntity(game.player2Id)!!.get<ManaPoolComponent>()!!.total shouldBe 0
            game.getLifeTotal(2) shouldBe 23
        }
    }
}
