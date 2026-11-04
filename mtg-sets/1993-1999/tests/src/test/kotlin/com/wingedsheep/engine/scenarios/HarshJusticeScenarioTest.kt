package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HarshJusticeScenarioTest : ScenarioTestBase() {
    private fun board(vararg extras: String) = scenario()
        .withPlayers("Attacker", "Defender")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardInHand(2, "Harsh Justice")
        .withLandsOnBattlefield(2, "Plains", 3)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Plains")
        .apply { extras.forEach { withCardOnBattlefield(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    private fun TestGame.castJusticeAndDealCombat(attacker: String = "Grizzly Bears") {
        declareAttackers(mapOf(attacker to 2)).error shouldBe null
        passPriority().error shouldBe null
        castSpell(2, "Harsh Justice").error shouldBe null
        resolveStack()
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers().error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
    }

    init {
        cardRegistry.register(card("Justice Damage Doubler") {
            typeLine = "Enchantment"
            replacementEffect(DoubleDamage(appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.youControl()
            )))
        })
        cardRegistry.register(card("Justice Noncombat Barrier") {
            typeLine = "Enchantment"
            replacementEffect(PreventDamage(amount = 1, appliesTo = EventPattern.DamageEvent(
                damageType = DamageType.NonCombat, recipient = Recipient.You
            )))
        })

        test("combat damage queues a separate trigger whose damage is noncombat and keeps the creature as source") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castJusticeAndDealCombat()
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
            game.state.stack shouldHaveSize 1
            val damage = game.resolveStack().flatMap { it.events }.filterIsInstance<DamageDealtEvent>()
            damage shouldHaveSize 1
            damage.single().sourceId shouldBe bears
            damage.single().targetId shouldBe game.player1Id
            damage.single().amount shouldBe 2
            damage.single().isCombatDamage shouldBe false
            game.getLifeTotal(1) shouldBe 18
        }

        test("the reflected damage uses the ordinary replacement choice and damaged player") {
            val game = board("Justice Damage Doubler", "Justice Noncombat Barrier")
            game.castJusticeAndDealCombat()
            game.getLifeTotal(2) shouldBe 16
            game.getLifeTotal(1) shouldBe 20
            game.state.stack shouldHaveSize 1
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            decision.playerId shouldBe game.player1Id
            val preventFirst = decision.options.indexOfFirst { it.contains("prevent", ignoreCase = true) }
            (preventFirst >= 0) shouldBe true
            game.submitDecision(OptionChosenResponse(decision.id, preventFirst)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 14
            game.state.stack shouldHaveSize 0
        }

        test("removing the damage source in response does not erase the delayed trigger") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false, isToken = true)
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(2, "Harsh Justice")
                .withLandsOnBattlefield(2, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castJusticeAndDealCombat()
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 18
        }

        for (keyword in listOf(Keyword.LIFELINK, Keyword.INFECT)) {
            val attacker = "Justice ${keyword.name} Token"
            cardRegistry.register(card(attacker) {
                typeLine = "Creature — Vampire"
                power = 2
                toughness = 2
                keywords(keyword)
            })
            test("a departed ${keyword.name} token retains its damage characteristics through a replacement choice") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, attacker, summoningSickness = false, isToken = true)
                    .withCardOnBattlefield(1, "Justice Damage Doubler")
                    .withCardOnBattlefield(1, "Justice Noncombat Barrier")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(2, "Harsh Justice")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
                val source = game.findPermanent(attacker)!!
                game.castJusticeAndDealCombat(attacker)
                game.castSpell(1, "Lightning Bolt", source).error shouldBe null
                game.resolveStack()
                game.state.getEntity(source) shouldBe null
                val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                decision.playerId shouldBe game.player1Id
                val preventFirst = decision.options.indexOfFirst { it.contains("prevent", ignoreCase = true) }
                (preventFirst >= 0) shouldBe true
                game.submitDecision(OptionChosenResponse(decision.id, preventFirst)).error shouldBe null
                game.resolveStack()
                // Combat dealt four; prevention before doubling reflects six. The old
                // token still supplies its controller, keyword and "you control" match.
                if (keyword == Keyword.LIFELINK) {
                    game.getLifeTotal(1) shouldBe 24
                    game.getLifeTotal(2) shouldBe 16
                } else {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                    game.state.getEntity(game.player1Id)?.get<CountersComponent>()?.getCount(CounterType.POISON) shouldBe 6
                    game.state.getEntity(game.player2Id)?.get<CountersComponent>()?.getCount(CounterType.POISON) shouldBe 4
                }
            }
        }

    }
}
