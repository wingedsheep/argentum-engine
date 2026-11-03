package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.SpellSwindle
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class SpellSwindleScenarioTest : FunSpec({
    val xSpell = card("Swindle Test X Spell") {
        manaCost = "{X}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(SpellSwindle, xSpell, PredefinedTokens.Treasure))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castTarget(): EntityId {
        val target = putCardInHand(player1, xSpell.name)
        giveMana(player1, Color.RED, 1)
        giveColorlessMana(player1, 4)
        castXSpell(player1, target, 4).error shouldBe null
        passPriority(player1)
        return target
    }

    fun GameTestDriver.swindle(target: EntityId) {
        val swindle = putCardInHand(player2, SpellSwindle.name)
        giveMana(player2, Color.BLUE, 2)
        giveColorlessMana(player2, 3)
        castSpellWithTargets(player2, swindle, listOf(ChosenTarget.Spell(target))).error shouldBe null
    }

    fun GameTestDriver.treasures() = getPermanents(player2).count { getCardName(it) == "Treasure" }

    test("Treasures use the stored stack mana value including X after the target is countered") {
        val d = driver()
        val target = d.castTarget()
        d.swindle(target)
        d.bothPass().error shouldBe null

        d.treasures() shouldBe 5
        d.state.getZone(d.player1, Zone.GRAVEYARD) shouldContain target
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("an uncounterable target still creates Treasures") {
        val d = driver()
        val target = d.castTarget()
        d.addComponent(target, CantBeCounteredComponent)
        d.swindle(target)
        d.bothPass().error shouldBe null

        d.treasures() shouldBe 5
        d.state.stack shouldContain target
        d.bothPass().error shouldBe null
        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("a target countered in response makes Spell Swindle fizzle without Treasures") {
        val d = driver()
        val target = d.castTarget()
        d.swindle(target)
        d.passPriority(d.player2)
        val counter = d.putCardInHand(d.player1, "Counterspell")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpellWithTargets(d.player1, counter, listOf(ChosenTarget.Spell(target))).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.getZone(d.player1, Zone.GRAVEYARD) shouldContain target
        d.bothPass().error shouldBe null

        d.treasures() shouldBe 0
        d.stackSize shouldBe 0
    }
})
