package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m15.cards.SiegeDragon
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Siege Dragon (M15 #162) — "Whenever this creature attacks, if defending player controls no
 * Walls, it deals 2 damage to each creature without flying that player controls."
 *
 * Three-player games: p[0] attacks p[1]. "Defending player" is the attacked player only, so a
 * Wall controlled by the other opponent neither stops the trigger nor do that opponent's
 * creatures take damage; a Wall controlled by the attacked player stops it entirely.
 */
class SiegeDragonScenarioTest : FunSpec({

    val TestWall = CardDefinition(
        name = "Test Wall",
        manaCost = ManaCost.parse("{1}"),
        typeLine = TypeLine(
            cardTypes = setOf(CardType.CREATURE),
            subtypes = setOf(Subtype.WALL),
        ),
        keywords = setOf(Keyword.DEFENDER),
        creatureStats = CreatureStats(0, 4),
    )

    fun threePlayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(SiegeDragon, TestWall))
        val players = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe players[0]
        return d to players
    }

    fun GameTestDriver.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.attackAndResolve(p: List<EntityId>, dragon: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(p[0], listOf(dragon), p[1]).error shouldBe null
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) passPriority(state.priorityPlayerId!!)
    }

    test("deals 2 to each non-flyer the defending player controls; another opponent's Wall doesn't matter") {
        val (d, p) = threePlayer()
        val dragon = d.putCreatureOnBattlefield(p[0], "Siege Dragon").also { d.removeSummoningSickness(it) }
        val ownCourser = d.putCreatureOnBattlefield(p[0], "Centaur Courser")
        val defenderCourser = d.putCreatureOnBattlefield(p[1], "Centaur Courser")
        val defenderBirds = d.putCreatureOnBattlefield(p[1], "Birds of Paradise")
        val otherWall = d.putCreatureOnBattlefield(p[2], "Test Wall")
        val otherCourser = d.putCreatureOnBattlefield(p[2], "Centaur Courser")

        d.attackAndResolve(p, dragon)

        d.damageOn(defenderCourser) shouldBe 2
        d.findPermanent(p[1], "Birds of Paradise") shouldBe defenderBirds
        d.damageOn(defenderBirds) shouldBe 0
        d.damageOn(otherCourser) shouldBe 0
        d.damageOn(otherWall) shouldBe 0
        d.damageOn(ownCourser) shouldBe 0
    }

    test("does nothing when the defending player controls a Wall") {
        val (d, p) = threePlayer()
        val dragon = d.putCreatureOnBattlefield(p[0], "Siege Dragon").also { d.removeSummoningSickness(it) }
        val defenderWall = d.putCreatureOnBattlefield(p[1], "Test Wall")
        val defenderCourser = d.putCreatureOnBattlefield(p[1], "Centaur Courser")

        d.attackAndResolve(p, dragon)

        d.damageOn(defenderCourser) shouldBe 0
        d.damageOn(defenderWall) shouldBe 0
    }
})
