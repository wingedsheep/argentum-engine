package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sth.cards.GravePact
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Grave Pact (STH #60) — "Whenever a creature you control dies, each other player sacrifices a
 * creature of their choice."
 *
 * Played as a team game (two teams of two) to pin the Two-Headed Giant ruling: "each other player"
 * includes your teammate, and a creature your teammate controls dying doesn't trigger it.
 */
class GravePactScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver().apply { registerCards(TestCards.all + GravePact) }
        val players = d.initMultiplayer(
            List(4) { Deck.of("Swamp" to 40) },
            format = Format.TeamVsTeam(),
            teams = listOf(listOf(0, 2), listOf(1, 3)),
            startingPlayer = 0,
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players
    }

    fun GameTestDriver.castDoomBladeOn(caster: EntityId, victim: EntityId) {
        val blade = putCardInHand(caster, "Doom Blade")
        giveColorlessMana(caster, 1)
        giveMana(caster, Color.BLACK, 1)
        castSpellWithTargets(caster, blade, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        var guard = 0
        while ((stackSize > 0 || isPaused) && guard++ < 40) {
            if (isPaused) autoResolveDecision() else passPriority(state.priorityPlayerId!!)
        }
    }

    test("your creature dies: every other player sacrifices, your teammate included") {
        val (d, players) = setup()
        val (p1, p2, p3, p4) = players
        d.putPermanentOnBattlefield(p1, "Grave Pact")
        val mine = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val others = listOf(p2, p3, p4).associateWith { d.putCreatureOnBattlefield(it, "Grizzly Bears") }

        d.castDoomBladeOn(p1, mine)

        d.getGraveyard(p1).contains(mine) shouldBe true
        others.forEach { (player, bears) ->
            withClue("$player's creature is sacrificed") { d.getGraveyard(player).contains(bears) shouldBe true }
        }
    }

    test("a creature your teammate controls dying doesn't trigger it") {
        val (d, players) = setup()
        val (p1, p2, p3, p4) = players
        d.putPermanentOnBattlefield(p1, "Grave Pact")
        val mine = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val teammates = d.putCreatureOnBattlefield(p3, "Grizzly Bears")
        val opponents = listOf(p2, p4).map { d.putCreatureOnBattlefield(it, "Grizzly Bears") }

        d.castDoomBladeOn(p1, teammates)

        d.getGraveyard(p3).contains(teammates) shouldBe true
        d.getCreatures(p1) shouldBe listOf(mine)
        opponents.forEach { d.state.getBattlefield().contains(it) shouldBe true }
    }
})
