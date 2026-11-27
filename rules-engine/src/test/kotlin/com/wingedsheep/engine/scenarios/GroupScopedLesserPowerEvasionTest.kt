package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Group-scoped `CantBeBlockedByCreaturesWithLessPower` (Champion of Lambholt): creatures with power
 * less than the *host's* power can't block the creatures its group filter covers. The threshold is
 * the host's projected power, not the attacker's; the host is in its own group unless `excludeSelf`;
 * the opponent's creatures aren't covered.
 */
class GroupScopedLesserPowerEvasionTest : FunSpec({

    val champion = card("Lesser Power Test Champion") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Human"
        power = 4; toughness = 4
        staticAbility {
            ability = CantBeBlockedByCreaturesWithLessPower(GroupFilter(GameObjectFilter.Creature.youControl()))
        }
    }
    val captain = card("Lesser Power Test Captain") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Human"
        power = 4; toughness = 4
        staticAbility {
            ability = CantBeBlockedByCreaturesWithLessPower(
                GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
            )
        }
    }
    val squire = card("Lesser Power Test Squire") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1; toughness = 1
    }
    val ogre = card("Lesser Power Test Ogre") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        power = 3; toughness = 3
    }
    val giant = card("Lesser Power Test Giant") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Giant"
        power = 4; toughness = 4
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(champion, captain, squire, ogre, giant))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.creature(owner: EntityId, name: String) =
        putCreatureOnBattlefield(owner, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.blockError(attackers: List<EntityId>, blocker: EntityId, blocked: EntityId): String? {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, attackers, player2).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        return declareBlockers(player2, mapOf(blocker to listOf(blocked))).error
    }

    test("the threshold is the host's power, not the attacker's") {
        val d = driver()
        d.creature(d.player1, champion.name)
        val small = d.creature(d.player1, squire.name)
        val blocker = d.creature(d.player2, ogre.name)
        withClue("Ogre's power 3 is less than the Champion's 4, though more than the 1/1 attacker's") {
            d.blockError(listOf(small), blocker, small) shouldNotBe null
        }
    }

    test("a blocker with power equal to the host's can block") {
        val d = driver()
        d.creature(d.player1, champion.name)
        val small = d.creature(d.player1, squire.name)
        val blocker = d.creature(d.player2, giant.name)
        d.blockError(listOf(small), blocker, small) shouldBe null
    }

    test("the host is covered by its own group clause, unless excludeSelf") {
        val d = driver()
        val host = d.creature(d.player1, champion.name)
        val blocker = d.creature(d.player2, ogre.name)
        d.blockError(listOf(host), blocker, host) shouldNotBe null

        val d2 = driver()
        val other = d2.creature(d2.player1, captain.name)
        val blocker2 = d2.creature(d2.player2, ogre.name)
        withClue("'other creatures you control' leaves the host out") {
            d2.blockError(listOf(other), blocker2, other) shouldBe null
        }
    }

    test("the opponent's attackers aren't covered") {
        val d = driver()
        d.creature(d.player2, champion.name)
        val small = d.creature(d.player1, squire.name)
        val blocker = d.creature(d.player2, ogre.name)
        // Player 2 controls the Champion; the blocking side's own Champion doesn't restrict its blocks.
        d.blockError(listOf(small), blocker, small) shouldBe null
    }
})
