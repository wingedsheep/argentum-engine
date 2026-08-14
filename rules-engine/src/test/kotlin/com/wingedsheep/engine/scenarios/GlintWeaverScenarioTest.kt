package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Glint Weaver (MKM #162).
 *
 * "Reach
 *  When this creature enters, distribute three +1/+1 counters among one, two, or three target
 *  creatures, then you gain life equal to the greatest toughness among creatures you control."
 *
 * The interesting claim is the **"then"**: the two halves share one resolution, and the distribute
 * half pauses for player input. A `Composite(distribute, gainLife)` is only correct if the life
 * gain survives that pause *and* reads the board after the counters land — the failure mode being a
 * silently-dropped second effect, or a life total computed from pre-counter toughness.
 *
 * Covers:
 *  - Counters land on the chosen targets and the life gain reads post-counter toughness.
 *  - Targeting is unrestricted ("target creatures", not "creatures you control"), but the life gain
 *    still only sums over creatures *you* control — pumping an opponent's creature gains nothing.
 */
class GlintWeaverScenarioTest : FunSpec({

    // Distinct toughnesses so the life total names exactly which creature was measured.
    val toughOne = CardDefinition.creature(
        name = "Test Thick Yak",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Yak")),
        power = 1,
        toughness = 4,
        oracleText = ""
    )
    val frailOne = CardDefinition.creature(
        name = "Test Frail Yak",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Yak")),
        power = 1,
        toughness = 1,
        oracleText = ""
    )

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(toughOne, frailOne))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast Glint Weaver, answer its targeting and (if offered) distribution decisions. */
    fun castAndDistribute(
        d: GameTestDriver,
        caster: EntityId,
        split: Map<EntityId, Int>
    ) {
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(caster, Color.GREEN, 7)

        val card = d.putCardInHand(caster, "Glint Weaver")
        val cast = d.castSpell(caster, card)
        withClue("casting Glint Weaver: ${cast.error}") { cast.error shouldBe null }

        // Weaver resolves and enters; its ETB trigger then asks for one-to-three targets.
        d.bothPass()
        withClue("expected the ETB trigger's target selection") {
            d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        }
        d.submitTargetSelection(caster, split.keys.toList()).error shouldBe null

        // Resolve the trigger. The engine may split evenly on its own or ask; answer either way.
        d.bothPass()
        (d.pendingDecision as? DistributeDecision)?.let { decision ->
            d.submitDecision(caster, DistributionResponse(decision.id, split))
            d.bothPass()
        }
    }

    test("counters land, then life gained equals the greatest toughness after the counters") {
        val d = setup()
        val p1 = d.activePlayer!!

        // A 1/4 and a 1/1. Loading all three counters onto the 1/1 makes it a 4/4 — tying the
        // untouched 1/4 — while loading them onto the 1/4 makes it a 4/7. Splitting 2/1 here
        // yields a 3/6 and a 2/2, so the greatest toughness must read 6, not the pre-counter 4.
        val thick = d.putCreatureOnBattlefield(p1, "Test Thick Yak")
        val frail = d.putCreatureOnBattlefield(p1, "Test Frail Yak")
        val lifeBefore = d.getLifeTotal(p1)

        castAndDistribute(d, p1, mapOf(thick to 2, frail to 1))

        withClue("three counters split 2/1 across the two targets") {
            plusOneCounters(d, thick) shouldBe 2
            plusOneCounters(d, frail) shouldBe 1
        }
        withClue("life gain reads post-counter toughness (4 + 2 = 6), not the printed 4") {
            d.getLifeTotal(p1) shouldBe lifeBefore + 6
        }
    }

    test("an opponent's creature is a legal target, but only your creatures feed the life gain") {
        val d = setup()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        // Your board is Glint Weaver alone (a 3/3). The opponent has the big body, and it takes
        // all three counters — so it becomes a 4/7 while your best toughness is still Weaver's 3.
        val theirs = d.putCreatureOnBattlefield(p2, "Test Thick Yak")
        val lifeBefore = d.getLifeTotal(p1)

        castAndDistribute(d, p1, mapOf(theirs to 3))

        withClue("all three counters went to the opponent's creature") {
            plusOneCounters(d, theirs) shouldBe 3
        }
        withClue("greatest toughness among creatures YOU control is Glint Weaver's 3") {
            d.getLifeTotal(p1) shouldBe lifeBefore + 3
        }
    }
})
