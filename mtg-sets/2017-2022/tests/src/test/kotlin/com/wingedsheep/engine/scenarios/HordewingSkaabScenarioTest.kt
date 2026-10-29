package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mic.cards.HordewingSkaab
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hordewing Skaab (MIC #15) — {4}{U} Creature — Zombie Horror, 3/3.
 *
 *   Flying
 *   Other Zombies you control have flying.
 *   Whenever one or more Zombies you control deal combat damage to one or more of your opponents,
 *   you may draw cards equal to the number of opponents dealt damage this way. If you do, discard
 *   that many cards.
 */
class HordewingSkaabScenarioTest : FunSpec({

    fun driverWith(players: Int): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(HordewingSkaab))
        val ids = d.initMultiplayer(List(players) { Deck.of("Grizzly Bears" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe ids[0]
        return d to ids
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    /**
     * Walks combat to the postcombat main phase, answering the may prompt with [accept] and
     * discarding the first cards offered. Returns how many times the may prompt was asked and
     * how many cards were discarded.
     */
    fun GameTestDriver.finishCombat(accept: Boolean): Pair<Int, Int> {
        var asked = 0
        var discarded = 0
        var guard = 0
        while (state.step != Step.POSTCOMBAT_MAIN && guard++ < 60) {
            val decision = pendingDecision
            when {
                decision is YesNoDecision -> {
                    submitYesNo(decision.playerId, accept).error shouldBe null
                    asked++
                }
                decision is SelectCardsDecision -> {
                    val picks = decision.options.take(decision.minSelections)
                    submitCardSelection(decision.playerId, picks).error shouldBe null
                    discarded += picks.size
                }
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
        }
        return asked to discarded
    }

    test("other Zombies you control have flying") {
        val (d, p) = driverWith(2)
        d.ready(p[0], "Hordewing Skaab")
        val zombie = d.ready(p[0], "Black Creature")
        val bears = d.ready(p[0], "Grizzly Bears")

        d.state.projectedState.hasKeyword(zombie, Keyword.FLYING) shouldBe true
        d.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
    }

    test("Zombies hitting two opponents draw two, then discard two — one trigger") {
        val (d, p) = driverWith(4)
        val skaab = d.ready(p[0], "Hordewing Skaab")
        val zombie = d.ready(p[0], "Black Creature")
        val bears = d.ready(p[0], "Grizzly Bears")
        val hand = d.getHandSize(p[0])
        val graveyard = d.getGraveyard(p[0]).size

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(skaab to p[1], zombie to p[2], bears to p[3])).error shouldBe null

        val (asked, discarded) = d.finishCombat(accept = true)

        withClue("a single trigger for the batch") { asked shouldBe 1 }
        withClue("the non-Zombie's opponent isn't counted") { discarded shouldBe 2 }
        d.getHandSize(p[0]) shouldBe hand
        d.getGraveyard(p[0]).size shouldBe graveyard + 2
        d.getLifeTotal(p[3]) shouldBe 18
    }

    test("one opponent hit by two Zombies counts once") {
        val (d, p) = driverWith(3)
        val skaab = d.ready(p[0], "Hordewing Skaab")
        val zombie = d.ready(p[0], "Black Creature")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(skaab to p[1], zombie to p[1])).error shouldBe null

        val (asked, discarded) = d.finishCombat(accept = true)

        asked shouldBe 1
        discarded shouldBe 1
        d.getHandSize(p[0]) shouldBe hand
    }

    test("declining draws and discards nothing") {
        val (d, p) = driverWith(2)
        val skaab = d.ready(p[0], "Hordewing Skaab")
        val hand = d.getHandSize(p[0])

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(skaab to p[1])).error shouldBe null

        val (asked, discarded) = d.finishCombat(accept = false)

        asked shouldBe 1
        discarded shouldBe 0
        d.getHandSize(p[0]) shouldBe hand
    }

    test("a non-Zombie connecting alone doesn't trigger") {
        val (d, p) = driverWith(2)
        d.ready(p[0], "Hordewing Skaab")
        val bears = d.ready(p[0], "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(bears to p[1])).error shouldBe null

        val (asked, _) = d.finishCombat(accept = true)
        asked shouldBe 0
        d.getLifeTotal(p[1]) shouldBe 18
    }
})
