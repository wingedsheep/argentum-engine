package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.HundredHandedOne
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hundred-Handed One (THS #20) — "As long as this creature is monstrous, it has reach and can block
 * an additional ninety-nine creatures each combat."
 *
 * Both halves sit behind the monstrous condition: before monstrosity it blocks one ground creature
 * like any other; after monstrosity resolves (here at instant speed in the declare attackers step)
 * it blocks a whole team, flyer included.
 */
class HundredHandedOneScenarioTest : FunSpec({

    val abilityId = HundredHandedOne.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HundredHandedOne)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.attackers(): List<EntityId> =
        listOf("Grizzly Bears", "Grizzly Bears", "Grizzly Bears", "Birds of Paradise").map {
            putCreatureOnBattlefield(player1, it).also { id -> removeSummoningSickness(id) }
        }

    test("not monstrous: blocks only one creature and has no reach") {
        val d = driver()
        val p2 = d.getOpponent(d.player1)
        val team = d.attackers()
        val giant = d.putCreatureOnBattlefield(p2, "Hundred-Handed One")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, team, p2).outcome shouldBe Outcome.Done
        d.bothPass()

        d.state.projectedState.hasKeyword(giant, Keyword.REACH) shouldBe false
        withClue("two ground attackers exceed a single block") {
            d.declareBlockers(p2, mapOf(giant to team.take(2))).error shouldNotBe null
        }
        withClue("no reach, so the flyer can't be blocked") {
            d.declareBlockers(p2, mapOf(giant to listOf(team.last()))).error shouldNotBe null
        }
        d.declareBlockers(p2, mapOf(giant to listOf(team.first()))).error shouldBe null
    }

    test("monstrous: reach, three counters, and blocks every attacker") {
        val d = driver()
        val p2 = d.getOpponent(d.player1)
        val team = d.attackers()
        val giant = d.putCreatureOnBattlefield(p2, "Hundred-Handed One")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, team, p2).outcome shouldBe Outcome.Done
        d.passPriority(d.player1)

        d.giveMana(p2, Color.WHITE, 6)
        d.submitSuccess(ActivateAbility(p2, giant, abilityId))
        d.bothPass()

        d.state.projectedState.getPower(giant) shouldBe 6
        d.state.projectedState.getToughness(giant) shouldBe 8
        d.state.projectedState.hasKeyword(giant, Keyword.REACH) shouldBe true

        d.bothPass()
        withClue("one plus ninety-nine covers all four attackers, the flyer via reach") {
            d.declareBlockers(p2, mapOf(giant to team)).error shouldBe null
        }
    }
})
