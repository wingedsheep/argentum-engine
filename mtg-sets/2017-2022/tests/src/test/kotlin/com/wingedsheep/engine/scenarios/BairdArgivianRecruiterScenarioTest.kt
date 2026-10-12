package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.BairdArgivianRecruiter
import com.wingedsheep.mtg.sets.definitions.lrw.cards.MirrorEntity
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Baird, Argivian Recruiter (DMU #195) — "At the beginning of your end step, if you control a
 * creature with power greater than its base power, create a 1/1 white Soldier creature token."
 *
 * Base power is the layers 1–7b value, not the printed one: under Mirror Entity's "base power and
 * toughness X/X" a 2/2 at 4/4 is *not* above its base, and a +1/+1 counter on top of that is.
 */
class BairdArgivianRecruiterScenarioTest : FunSpec({

    val entityAbility = MirrorEntity.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BairdArgivianRecruiter + MirrorEntity)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(d.player1, "Baird, Argivian Recruiter")
        return d
    }

    fun GameTestDriver.soldiers(): Int = getCreatures(player1).count { getCardName(it) == "Soldier Token" || getCardName(it) == "Soldier" }

    fun GameTestDriver.throughEndStep() {
        passPriorityUntil(Step.END)
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    fun GameTestDriver.addPlusOne(id: EntityId) =
        addComponent(id, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))

    test("no creature above its base power: no token") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.throughEndStep()
        d.soldiers() shouldBe 0
    }

    test("a +1/+1 counter puts a creature above its base power: one token") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addPlusOne(bears)
        d.throughEndStep()
        d.soldiers() shouldBe 1
    }

    test("an opponent's pumped creature doesn't count") {
        val d = driver()
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(d.player1), "Grizzly Bears")
        d.addPlusOne(theirs)
        d.throughEndStep()
        d.soldiers() shouldBe 0
    }

    test("power set by a base-setting effect is the base, not above it") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Mirror Entity")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.WHITE, 4)
        val entity = d.getCreatures(d.player1).first { d.getCardName(it) == "Mirror Entity" }
        (d.submit(ActivateAbility(d.player1, entity, entityAbility, xValue = 4)).outcome is Outcome.Done) shouldBe true
        d.bothPass()

        withClue("Mirror Entity made the 2/2 a base 4/4") {
            d.state.projectedState.getPower(bears) shouldBe 4
            d.state.projectedState.getBasePower(bears) shouldBe 4
        }
        d.throughEndStep()
        withClue("4/4s with base 4 are not above their base — the printed 2 isn't the base") {
            d.soldiers() shouldBe 0
        }
    }

    test("a counter on a creature whose base was set still counts") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Mirror Entity")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.WHITE, 4)
        val entity = d.getCreatures(d.player1).first { d.getCardName(it) == "Mirror Entity" }
        d.submit(ActivateAbility(d.player1, entity, entityAbility, xValue = 4))
        d.bothPass()
        d.addPlusOne(bears)
        d.state.projectedState.getPower(bears) shouldBe 5
        d.throughEndStep()
        d.soldiers() shouldBe 1
    }
})
