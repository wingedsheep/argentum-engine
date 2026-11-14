package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.ChandrasFury
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chandra's Fury — "Chandra's Fury deals 4 damage to target player or planeswalker and 1 damage to each
 * creature that player or that planeswalker's controller controls." Only the player or planeswalker is
 * targeted, so hexproof creatures still take the 1 damage.
 */
class ChandrasFuryScenarioTest : FunSpec({
    val hexproofBear = card("Fury Test Hexproof Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.HEXPROOF)
    }
    val walker = card("Fury Test Planeswalker") {
        manaCost = "{3}{U}"
        typeLine = "Planeswalker — Jace"
        startingLoyalty = 10
    }
    val animatedWalker = card("Fury Test Animated Planeswalker") {
        manaCost = "{3}{U}"
        typeLine = "Artifact Creature Planeswalker — Golem"
        startingLoyalty = 10
        power = 1
        toughness = 30
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ChandrasFury, hexproofBear, walker, animatedWalker))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun putWalker(d: GameTestDriver, player: EntityId, name: String): EntityId =
        d.putPermanentOnBattlefield(player, name).also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 10)))
        }

    fun loyalty(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    fun damage(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun cast(d: GameTestDriver, target: ChosenTarget) {
        val me = d.activePlayer!!
        val fury = d.putCardInHand(me, "Chandra's Fury")
        d.giveMana(me, Color.RED, 5)
        d.castSpellWithTargets(me, fury, listOf(target)).outcome shouldBe Outcome.Done
        d.bothPass()
    }

    test("deals 4 to the target player and 1 to each creature they control, hexproof included") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val mine = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        val theirs = d.putPermanentOnBattlefield(opp, "Grizzly Bears")
        val hexproof = d.putPermanentOnBattlefield(opp, hexproofBear.name)

        cast(d, ChosenTarget.Player(opp))

        d.getLifeTotal(opp) shouldBe 16
        d.getLifeTotal(me) shouldBe 20
        damage(d, theirs) shouldBe 1
        damage(d, hexproof) shouldBe 1
        damage(d, mine) shouldBe 0
    }

    test("targeting a planeswalker damages it and its controller's creatures, not the player") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val mine = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        val theirs = d.putPermanentOnBattlefield(opp, "Grizzly Bears")
        val jace = putWalker(d, opp, walker.name)

        cast(d, ChosenTarget.Permanent(jace))

        loyalty(d, jace) shouldBe 6
        d.getLifeTotal(opp) shouldBe 20
        damage(d, theirs) shouldBe 1
        damage(d, mine) shouldBe 0
    }

    test("a target planeswalker that is also a creature is dealt both the 4 and the 1") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val target = putWalker(d, opp, animatedWalker.name)

        cast(d, ChosenTarget.Permanent(target))

        loyalty(d, target) shouldBe 5
        d.getLifeTotal(opp) shouldBe 20
    }
})
