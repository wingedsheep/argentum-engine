package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.DestinedConfrontation
import com.wingedsheep.mtg.sets.definitions.tla.cards.ZhaoRuthlessAdmiral
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Destined Confrontation (TLA) — {2}{W}{W} Sorcery.
 *
 * "Each player chooses any number of creatures they control with total power 4 or less, then
 * sacrifices all other creatures they control."
 *
 * Exercises the per-player keep-then-sacrifice composition (a `ForEachPlayerEffect` of
 * gather → `SelectFromCollectionEffect(ChooseAnyNumber, TotalPowerAtMost(4))` → sacrifice the
 * remainder) and, crucially, the new aggregate **total-power** selection cap: each player keeps
 * any subset of their own creatures whose combined projected power is at most 4, and everything
 * unkept is sacrificed. The cap is enforced server-side, not cosmetic — an over-4 selection has
 * its excess creatures trimmed (in response order) into the sacrificed remainder.
 */
class DestinedConfrontationScenarioTest : FunSpec({

    // Vanilla bears with fixed power, so projected power == printed power and the cap math is exact.
    fun bear(name: String, power: Int): CardDefinition = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Bear")),
        power = power,
        toughness = power.coerceAtLeast(1)
    )

    val powerOne = bear("Power One Bear", 1)
    val powerTwo = bear("Power Two Bear", 2)
    val powerThree = bear("Power Three Bear", 3)
    val powerFour = bear("Power Four Bear", 4)
    val powerFive = bear("Power Five Bear", 5)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                DestinedConfrontation, ZhaoRuthlessAdmiral,
                powerOne, powerTwo, powerThree, powerFour, powerFive
            )
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    /**
     * Cast Destined Confrontation (already in [caster]'s hand) and drive it to completion,
     * answering each player's keep-selection from [keepByPlayer]. Players with no creatures
     * (or whose every keep is illegal) are never prompted — the loop just resolves the stack.
     */
    fun GameTestDriver.castAndResolve(
        caster: EntityId,
        spellId: EntityId,
        keepByPlayer: Map<EntityId, List<EntityId>>
    ) {
        giveMana(caster, Color.WHITE, 4)
        castSpell(caster, spellId)
        var guard = 0
        while ((pendingDecision != null || state.stack.isNotEmpty()) && guard < 100) {
            val decision = pendingDecision
            if (decision is SelectCardsDecision) {
                submitCardSelection(decision.playerId, keepByPlayer[decision.playerId] ?: emptyList())
            } else {
                bothPass()
            }
            guard++
        }
    }

    test("each player keeps a legal subset (total power <= 4) and sacrifices the rest") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player: powers 1, 3, 5. Keep {1,3} (total 4); sacrifice the 5.
        val myOne = driver.putCreatureOnBattlefield(me, "Power One Bear")
        val myThree = driver.putCreatureOnBattlefield(me, "Power Three Bear")
        val myFive = driver.putCreatureOnBattlefield(me, "Power Five Bear")

        // Opponent: powers 2, 4. Keep the single 4 (a 4-power creature alone is legal); sacrifice the 2.
        val oppTwo = driver.putCreatureOnBattlefield(opponent, "Power Two Bear")
        val oppFour = driver.putCreatureOnBattlefield(opponent, "Power Four Bear")

        val spell = driver.putCardInHand(me, "Destined Confrontation")
        driver.castAndResolve(
            caster = me,
            spellId = spell,
            keepByPlayer = mapOf(
                me to listOf(myOne, myThree),
                opponent to listOf(oppFour)
            )
        )

        val battlefield = driver.state.getBattlefield().toSet()

        // Kept creatures (total power <= 4) remain.
        battlefield.contains(myOne) shouldBe true
        battlefield.contains(myThree) shouldBe true
        battlefield.contains(oppFour) shouldBe true

        // The rest were sacrificed to their owner's graveyard.
        battlefield.contains(myFive) shouldBe false
        battlefield.contains(oppTwo) shouldBe false
        driver.getGraveyard(me).contains(myFive) shouldBe true
        driver.getGraveyard(opponent).contains(oppTwo) shouldBe true
    }

    test("a player who keeps nothing sacrifices all of their creatures") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myOne = driver.putCreatureOnBattlefield(me, "Power One Bear")
        val myTwo = driver.putCreatureOnBattlefield(me, "Power Two Bear")

        val spell = driver.putCardInHand(me, "Destined Confrontation")
        // Keep nothing — both creatures must be sacrificed (the opponent has none, so no prompt for them).
        driver.castAndResolve(caster = me, spellId = spell, keepByPlayer = mapOf(me to emptyList()))

        val battlefield = driver.state.getBattlefield().toSet()
        battlefield.contains(myOne) shouldBe false
        battlefield.contains(myTwo) shouldBe false
        driver.getGraveyard(me).contains(myOne) shouldBe true
        driver.getGraveyard(me).contains(myTwo) shouldBe true
    }

    test("the total-power cap is enforced: an over-4 selection has its excess creature trimmed and sacrificed") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two 1-power bears and one 4-power bear. {1,1,4} totals 6; the greedy count ceiling is 2,
        // so the player may submit two creatures — but the pair {1,4} totals 5, over the cap.
        val oneA = driver.putCreatureOnBattlefield(me, "Power One Bear")
        val oneB = driver.putCreatureOnBattlefield(me, "Power One Bear")
        val four = driver.putCreatureOnBattlefield(me, "Power Four Bear")

        val spell = driver.putCardInHand(me, "Destined Confrontation")
        // Attempt to keep {1-power, 4-power} = total power 5 (illegal). In response order the 1 is
        // accepted (running 1) and the 4 is rejected (running 5 > 4), so only the 1-power is kept.
        driver.castAndResolve(caster = me, spellId = spell, keepByPlayer = mapOf(me to listOf(oneA, four)))

        val battlefield = driver.state.getBattlefield().toSet()
        // Only the accepted 1-power bear survives; the over-cap 4-power and the unchosen 1-power are sacrificed.
        battlefield.contains(oneA) shouldBe true
        battlefield.contains(four) shouldBe false
        battlefield.contains(oneB) shouldBe false
        driver.getGraveyard(me).contains(four) shouldBe true
        driver.getGraveyard(me).contains(oneB) shouldBe true
    }

    // Regression: a per-permanent sacrifice trigger (Zhao's "whenever you sacrifice another
    // permanent") firing during the CASTER's iteration used to drop the OPPONENT's sacrificed
    // collection — the deferred PendingTriggersContinuation was inserted between the opponent's
    // SelectFromCollection producer and its MoveCollection consumer, so the consumer resumed with an
    // empty collection and the opponent kept everything. Both players must still sacrifice correctly.
    test("Zhao in play: the caster's sacrifice trigger does not swallow the opponent's sacrifice") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Caster: Zhao (3, kept) + a 5-power bear (sacrificed → fires Zhao's per-permanent trigger).
        val zhao = driver.putCreatureOnBattlefield(me, "Zhao, Ruthless Admiral")
        val myFive = driver.putCreatureOnBattlefield(me, "Power Five Bear")

        // Opponent: a 2 (kept) and a 5 (must be sacrificed). This is the one the bug used to spare.
        val oppTwo = driver.putCreatureOnBattlefield(opponent, "Power Two Bear")
        val oppFive = driver.putCreatureOnBattlefield(opponent, "Power Five Bear")

        val spell = driver.putCardInHand(me, "Destined Confrontation")
        driver.castAndResolve(
            caster = me,
            spellId = spell,
            keepByPlayer = mapOf(
                me to listOf(zhao),
                opponent to listOf(oppTwo)
            )
        )

        val battlefield = driver.state.getBattlefield().toSet()

        // Kept creatures survive.
        battlefield.contains(zhao) shouldBe true
        battlefield.contains(oppTwo) shouldBe true

        // Both sacrifices went through — the opponent's 5 is gone despite Zhao firing on the caster's.
        battlefield.contains(myFive) shouldBe false
        battlefield.contains(oppFive) shouldBe false
        driver.getGraveyard(me).contains(myFive) shouldBe true
        driver.getGraveyard(opponent).contains(oppFive) shouldBe true

        // And Zhao's trigger actually resolved: sacrificing one permanent pumped it +1/+0 → 4/4.
        driver.state.projectedState.getPower(zhao) shouldBe 4
    }

    test("a single creature with power greater than 4 is never a legal keep and is sacrificed") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A lone 5-power creature can't be kept (the cap's count ceiling is 0), so no keep prompt is
        // offered and it is sacrificed outright.
        val five = driver.putCreatureOnBattlefield(me, "Power Five Bear")

        val spell = driver.putCardInHand(me, "Destined Confrontation")
        driver.castAndResolve(caster = me, spellId = spell, keepByPlayer = emptyMap())

        driver.state.getBattlefield().contains(five) shouldBe false
        driver.getGraveyard(me).contains(five) shouldBe true
    }
})
