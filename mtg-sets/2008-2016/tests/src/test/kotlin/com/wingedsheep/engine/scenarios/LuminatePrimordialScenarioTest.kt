package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.LuminatePrimordial
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Luminate Primordial — "When this creature enters, for each opponent, exile up to one target
 * creature that player controls and that player gains life equal to its power."
 *
 * Proves the per-target pairing: each opponent gains life equal to the power of *their own*
 * exiled creature, not the first target's.
 */
class LuminatePrimordialScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val p1: EntityId, val p2: EntityId, val p3: EntityId)

    fun table(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + LuminatePrimordial)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Table(d, p1, p2, p3)
    }

    fun Table.resolveStack() {
        var guard = 0
        while (guard++ < 20 && d.pendingDecision == null && d.stackSize > 0) d.bothPass()
    }

    fun Table.castPrimordial() {
        val primordial = d.putCardInHand(p1, "Luminate Primordial")
        d.giveMana(p1, Color.WHITE, 7)
        d.castSpell(p1, primordial).error.shouldBeNull()
        resolveStack()
    }

    test("each opponent's creature is exiled and that opponent gains life equal to its own creature's power") {
        val t = table()
        val bears = t.d.putCreatureOnBattlefield(t.p2, "Grizzly Bears") // 2/2
        val courser = t.d.putCreatureOnBattlefield(t.p3, "Centaur Courser") // 3/3

        t.castPrimordial()

        val targeting = t.d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targeting.playerId shouldBe t.p1
        t.d.submitTargetSelection(t.p1, listOf(bears, courser)).error.shouldBeNull()
        t.resolveStack()

        t.d.getExile(t.p2) shouldContain bears
        t.d.getExile(t.p3) shouldContain courser
        withClue("Bears' controller gains 2, Courser's controller gains 3") {
            t.d.getLifeTotal(t.p2) shouldBe 22
            t.d.getLifeTotal(t.p3) shouldBe 23
        }
        t.d.getLifeTotal(t.p1) shouldBe 20
    }

    test("choosing no targets exiles nothing and gains no life") {
        val t = table()
        val bears = t.d.putCreatureOnBattlefield(t.p2, "Grizzly Bears")

        t.castPrimordial()

        t.d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        t.d.submitTargetSelection(t.p1, emptyList()).error.shouldBeNull()
        t.resolveStack()

        t.d.findPermanent(t.p2, "Grizzly Bears") shouldBe bears
        t.d.getLifeTotal(t.p2) shouldBe 20
    }
})
