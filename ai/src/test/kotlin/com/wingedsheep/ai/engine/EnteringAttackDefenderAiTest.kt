package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The AI answers the CR 508.4 "what does this token attack?" question raised by
 * [EnteringAttackDefender.CONTROLLER_CHOOSES] with a legal defender, through the generic
 * target-decision path, until every token has entered attacking.
 */
class EnteringAttackDefenderAiTest : FunSpec({

    val captain = card("AI Test Choosing Captain") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = Effects.CreateToken(
                power = 1, toughness = 1, colors = setOf(Color.RED), creatureTypes = setOf("Soldier"),
                count = 2, tapped = true, attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            )
        }
    }
    val walker = card("AI Test Defending Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }
    val cards = TestCards.all + listOf(captain, walker)
    val registry = CardRegistry().apply { register(cards) }

    test("the AI picks a legal defender for each token") {
        val d = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val attacker = d.putCreatureOnBattlefield(d.player1, captain.name).also { d.removeSummoningSickness(it) }
        val pw = d.putPermanentOnBattlefield(d.player2, walker.name)
        d.addComponent(pw, CountersComponent().withAdded(CounterType.LOYALTY, 3))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).error shouldBe null
        repeat(10) { if (d.pendingDecision == null && d.stackSize > 0) d.bothPass() }

        val ai = AIPlayer.create(registry, d.player1, AiProfile.PRODUCTION)
        repeat(2) {
            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val response = ai.respondToDecision(d.state, decision).shouldBeInstanceOf<TargetsResponse>()
            val pick = response.selectedTargets.getValue(0)
            pick shouldHaveSize 1
            pick.single() shouldBeIn listOf(d.player2, pw)
            d.submitDecision(d.player1, response).error shouldBe null
        }

        d.pendingDecision shouldBe null
        val tokens = d.getCreatures(d.player1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens shouldHaveSize 2
        tokens.forEach { d.state.getEntity(it)?.get<AttackingComponent>()?.defenderId shouldBeIn listOf(d.player2, pw) }
    }
})
