package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * `DynamicAmount.TotalManaSpent` read by a permanent's own enters trigger ("where X is the amount
 * of mana spent to cast her" — Toph, Greatest Earthbender): the payment recorded on the permanent
 * when it resolved, generic paid with colorless included, and 0 once the object re-enters without
 * being cast (CR 400.7 — the returned card is a new object with no memory of its earlier cast).
 */
class TotalManaSpentOnPermanentTest : ScenarioTestBase() {
    init {
        val echo = "Mana Echo Bear"
        cardRegistry.register(card(echo) {
            manaCost = "{2}{G}"
            typeLine = "Creature — Bear"
            power = 2
            toughness = 2
            triggeredAbility {
                trigger = Triggers.self.enters()
                effect = Effects.GainLife(DynamicAmounts.totalManaSpent())
            }
        })
        val raise = "Test Raise"
        cardRegistry.register(card(raise) {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                val creature = target(
                    TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = Effects.PutOntoBattlefield(creature)
            }
        })

        fun setUp() = scenario().withPlayers("Player1", "Player2")
            .withCardInHand(1, echo).withCardInHand(1, "Lightning Bolt").withCardInHand(1, raise)
            .withCardInHand(1, "Cloudshift")
            .withLandsOnBattlefield(1, "Forest", 1)
            .withLandsOnBattlefield(1, "Snow-Covered Wastes", 2)
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        fun cast() = setUp().also { game ->
            game.castSpell(1, echo).error shouldBe null
            game.resolveStack()
        }

        test("enters trigger reads the whole payment, generic paid with colorless included") {
            cast().getLifeTotal(1) shouldBe 23
        }

        test("the same card returned without being cast spent no mana") {
            val game = cast()
            val bear = game.findPermanent(echo)!!
            game.state = game.state.withEntity(game.player1Id,
                game.state.getEntity(game.player1Id)!!.with(ManaPoolComponent(red = 1)))
            game.castSpell(1, "Lightning Bolt", targetId = bear).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, echo) shouldBe true

            val card = game.state.getGraveyard(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == echo
            }
            val raiseId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == raise
            }
            game.execute(CastSpell(game.player1Id, raiseId,
                listOf(ChosenTarget.Card(card, game.player1Id, Zone.GRAVEYARD)))).error shouldBe null
            game.resolveStack()

            game.findPermanent(echo) shouldBe card
            game.getLifeTotal(1) shouldBe 23
        }

        test("a blink in response leaves the original trigger its payment; the new object's reads 0") {
            val game = setUp()
            game.castSpell(1, echo).error shouldBe null
            game.passPriority()
            game.passPriority() // the bear resolves; its enters trigger waits on the stack
            game.state.stack.size shouldBe 1

            game.state = game.state.withEntity(game.player1Id,
                game.state.getEntity(game.player1Id)!!.with(ManaPoolComponent(white = 1)))
            game.castSpell(1, "Cloudshift", targetId = game.findPermanent(echo)!!).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 23
        }
    }
}
