package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [Player.EachOtherPlayer] — "each other player": every player but you, teammates included. In a
 * team game (CR 102.3, Two-Headed Giant's ruling on Grave Pact) that is a different set from
 * [Player.EachOpponent], which skips your teammate; this pins the difference in every plural slot
 * the reference fans out in — a `PlayerRef` effect target, a `ForEachPlayer` loop and a player count.
 */
class EachOtherPlayerReferenceTest : FunSpec({

    val othersLose = card("Others Lose One") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOtherPlayer)) }
    }
    val opponentsLose = card("Opponents Lose One") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }
    val othersGainLoop = card("Others Gain Two") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.ForEachPlayer(Player.EachOtherPlayer, Effects.GainLife(2)) }
    }
    val countOthers = card("Count Others") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(DynamicAmounts.playerCount(Player.EachOtherPlayer)) }
    }

    fun teamGame(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(othersLose, opponentsLose, othersGainLoop, countOthers))
        }
        val players = d.initMultiplayer(
            List(4) { Deck.of("Swamp" to 40) },
            format = Format.TeamVsTeam(),
            teams = listOf(listOf(0, 2), listOf(1, 3)),
            startingPlayer = 0,
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players
    }

    fun GameTestDriver.castAndResolve(caster: EntityId, name: String) {
        castSpell(caster, putCardInHand(caster, name)).error shouldBe null
        var guard = 0
        while (stackSize > 0 && guard++ < 20) passPriority(state.priorityPlayerId!!)
    }

    test("a PlayerRef effect reaches every other player, the teammate included") {
        val (d, players) = teamGame()
        val (p1, p2, p3, p4) = players
        d.castAndResolve(p1, "Others Lose One")
        d.getLifeTotal(p1) shouldBe 20
        listOf(p2, p3, p4).forEach { d.getLifeTotal(it) shouldBe 19 }
    }

    test("each opponent, by contrast, skips the teammate") {
        val (d, players) = teamGame()
        val (p1, p2, p3, p4) = players
        d.castAndResolve(p1, "Opponents Lose One")
        d.getLifeTotal(p3) shouldBe 20
        listOf(p2, p4).forEach { d.getLifeTotal(it) shouldBe 19 }
    }

    test("a ForEachPlayer loop runs once per other player, rebinding you to each") {
        val (d, players) = teamGame()
        val (p1, p2, p3, p4) = players
        d.castAndResolve(p1, "Others Gain Two")
        d.getLifeTotal(p1) shouldBe 20
        listOf(p2, p3, p4).forEach { d.getLifeTotal(it) shouldBe 22 }
    }

    test("a player count counts every other player still in the game") {
        val (d, players) = teamGame()
        d.castAndResolve(players[0], "Count Others")
        d.getLifeTotal(players[0]) shouldBe 23
    }
})
