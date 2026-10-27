package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.AbsorbIdentity
import com.wingedsheep.mtg.sets.definitions.khm.cards.LittjaraKinseekers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Absorb Identity (KHM #383) — {1}{U} Instant. "Return target creature to its owner's hand. You may
 * have Shapeshifters you control become copies of that creature until end of turn."
 *
 * The creature is already in its owner's hand when the copy happens, so the Shapeshifters copy it
 * as it last existed on the battlefield — its copiable values only, not its counters (ruling
 * 2021-02-05). All of them become copies or none do; the copies end at end of turn.
 */
class AbsorbIdentityScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AbsorbIdentity, LittjaraKinseekers))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }


    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    fun GameTestDriver.castAbsorb(p: EntityId, target: EntityId, accept: Boolean) {
        val spell = putCardInHand(p, "Absorb Identity")
        giveMana(p, Color.BLUE, 2)
        castSpell(p, spell, listOf(target)).error shouldBe null
        bothPass()
        submitYesNo(p, accept)
    }

    test("the Shapeshifters you control become copies of the returned creature until end of turn") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        d.replaceState(d.state.updateEntity(giant) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        })
        val kinseekers = listOf(
            d.putCreatureOnBattlefield(p, "Littjara Kinseekers"),
            d.putCreatureOnBattlefield(p, "Littjara Kinseekers"),
        )
        val bears = d.putCreatureOnBattlefield(p, "Grizzly Bears")

        d.castAbsorb(p, giant, accept = true)

        d.state.getHand(opp) shouldContain giant
        val projected = d.state.projectedState
        kinseekers.forEach { id ->
            withClue("a Kinseekers copies Hill Giant's copiable values, not its +1/+1 counters") {
                d.name(id) shouldBe "Hill Giant"
                projected.getPower(id) shouldBe 3
                projected.getToughness(id) shouldBe 3
            }
        }
        withClue("a creature that isn't a Shapeshifter is untouched") {
            d.name(bears) shouldBe "Grizzly Bears"
        }

        d.passPriorityUntil(Step.UPKEEP, opp)
        kinseekers.forEach { d.name(it) shouldBe "Littjara Kinseekers" }
    }

    test("declining leaves the Shapeshifters as they are, and the creature still returns") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val kinseekers = d.putCreatureOnBattlefield(p, "Littjara Kinseekers")

        d.castAbsorb(p, giant, accept = false)

        d.state.getHand(opp) shouldContain giant
        d.name(kinseekers) shouldBe "Littjara Kinseekers"
    }
})
