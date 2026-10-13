package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.KnightOfDawnsLight
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KnightOfDawnsLightScenarioTest : FunSpec({
    val gainThree = card("Knight Test Gain Three") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(3) }
    }
    val opponentGains = card("Knight Test Opponent Gains") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(3, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }
    val erase = card("Knight Test Erase") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val creature = target(TargetFilter.Creature); effect = Effects.RemoveAllAbilities(creature) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(KnightOfDawnsLight, gainThree, opponentGains, erase))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("its controller gains that much life plus 1") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, KnightOfDawnsLight.name)
        d.castSpell(d.player1, d.putCardInHand(d.player1, gainThree.name)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 24
    }

    test("an opponent's life gain is unchanged") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, KnightOfDawnsLight.name)
        d.castSpell(d.player1, d.putCardInHand(d.player1, opponentGains.name)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 23
    }

    test("after losing all abilities it no longer adds life") {
        val d = driver()
        val knight = d.putCreatureOnBattlefield(d.player1, KnightOfDawnsLight.name)
        d.castSpell(d.player1, d.putCardInHand(d.player1, erase.name), listOf(knight)).error shouldBe null
        d.bothPass()
        d.castSpell(d.player1, d.putCardInHand(d.player1, gainThree.name)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23
    }
})
