package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.VesuvanDuplimancy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class VesuvanDuplimancyScenarioTest : FunSpec({
    val hero = card("Duplimancy Test Hero") {
        manaCost = "{1}"
        typeLine = "Legendary Creature — Human Knight"
        power = 3
        toughness = 3
    }
    val pump = card("Duplimancy Test Pump") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ModifyStats(1, 1, t)
        }
    }
    val kill = card("Duplimancy Test Kill") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }
    val soldiers = card("Duplimancy Test Soldiers") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreateToken(
                power = 1, toughness = 1, colors = setOf(Color.WHITE), creatureTypes = setOf("Soldier")
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(VesuvanDuplimancy, hero, pump, kill, soldiers))
        initMirrorMatch(deck = Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        putPermanentOnBattlefield(player1, VesuvanDuplimancy.name)
    }

    fun GameTestDriver.named(name: String): List<EntityId> =
        getPermanents(player1).filter { getCardName(it) == name }

    fun GameTestDriver.isToken(id: EntityId) = state.getEntity(id)?.has<TokenComponent>() == true

    fun GameTestDriver.isLegendary(id: EntityId) =
        state.projectedState.getTypes(id).contains("LEGENDARY")

    /** Player 1 pumps [targetId]; player 2 kills it while Vesuvan's trigger is on the stack. */
    fun GameTestDriver.pumpThenOpponentKills(targetId: EntityId) {
        giveMana(player1, Color.GREEN, 1)
        castSpell(player1, putCardInHand(player1, pump.name), listOf(targetId)).error shouldBe null
        stackSize shouldBe 2
        passPriority(player1)
        giveMana(player2, Color.BLACK, 1)
        castSpell(player2, putCardInHand(player2, kill.name), listOf(targetId)).error shouldBe null
        while (stackSize > 0) bothPass()
    }

    test("a spell targeting only my legendary creature makes a nonlegendary token copy of it") {
        val d = driver()
        val heroId = d.putCreatureOnBattlefield(d.player1, hero.name)
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, pump.name), listOf(heroId)).error shouldBe null
        while (d.stackSize > 0) d.bothPass()

        val heroes = d.named(hero.name)
        heroes.size shouldBe 2
        val token = heroes.single { d.isToken(it) }
        d.isLegendary(token) shouldBe false
        d.isLegendary(heroId) shouldBe true
    }

    test("a spell targeting an opponent's creature does not trigger") {
        val d = driver()
        val theirs = d.putCreatureOnBattlefield(d.player2, hero.name)
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, pump.name), listOf(theirs)).error shouldBe null
        d.stackSize shouldBe 1
        while (d.stackSize > 0) d.bothPass()
        d.named(hero.name).size shouldBe 0
    }

    test("a target that died before resolution is still copied as it last existed") {
        val d = driver()
        val heroId = d.putCreatureOnBattlefield(d.player1, hero.name)
        d.pumpThenOpponentKills(heroId)

        d.getGraveyard(d.player1) shouldContain heroId
        val copies = d.named(hero.name)
        copies.size shouldBe 1
        d.isToken(copies.single()) shouldBe true
        d.isLegendary(copies.single()) shouldBe false
    }

    test("a token target that ceased to exist before resolution is still copied") {
        val d = driver()
        d.giveMana(d.player1, Color.WHITE, 1)
        d.castSpell(d.player1, d.putCardInHand(d.player1, soldiers.name)).error shouldBe null
        while (d.stackSize > 0) d.bothPass()
        val soldier = d.getPermanents(d.player1).single { d.isToken(it) }
        val soldierName = d.getCardName(soldier)!!

        d.pumpThenOpponentKills(soldier)

        d.getPermanents(d.player1) shouldNotContain soldier
        val copy = d.named(soldierName).single()
        d.state.projectedState.getPower(copy) shouldBe 1
    }
})
