package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Weatherlight Compleated (DMU #242).
 *
 * As long as it has four or more phyresis counters, it's a Phyrexian creature in addition to its
 * other types. Whenever a creature you control dies, put a phyresis counter on it, then draw a card
 * if it has seven or more phyresis counters on it — otherwise scry 1.
 *
 * Covers the counter-gated Layer 4 type change, the draw-vs-scry branch read *after* the counter is
 * placed, and the ruling that a creature-Weatherlight dying triggers itself and resolves off its
 * last-known counter count.
 */
class WeatherlightCompleatedScenarioTest : ScenarioTestBase() {

    private fun board(phyresis: Int, victim: String, removal: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Weatherlight Compleated")
            .withCardOnBattlefield(1, victim)
            .withCardInHand(1, removal)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val weatherlight = game.findPermanent("Weatherlight Compleated")!!
        game.state = game.state.updateEntity(weatherlight) {
            it.with(CountersComponent(mapOf(CounterType.PHYRESIS to phyresis)))
        }
        return game
    }

    private fun TestGame.phyresis(): Int =
        state.getEntity(findPermanent("Weatherlight Compleated")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PHYRESIS) ?: 0

    init {
        test("three counters: not a creature; the fourth makes it a 5/5 Phyrexian creature, and it scries") {
            val game = board(phyresis = 3, victim = "Grizzly Bears", removal = "Lightning Bolt")
            val weatherlight = game.findPermanent("Weatherlight Compleated")!!

            withClue("below four counters it is only a noncreature Vehicle") {
                game.state.projectedState.isCreature(weatherlight) shouldBe false
            }

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            val handBefore = game.handSize(1)
            game.resolveStack()

            withClue("four counters is short of seven, so the trigger scries 1") {
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            }
            game.skipSelection()
            game.resolveStack()

            game.phyresis() shouldBe 4
            game.handSize(1) shouldBe handBefore
            val projected = game.state.projectedState
            projected.isCreature(weatherlight) shouldBe true
            projected.hasType(weatherlight, "ARTIFACT") shouldBe true
            projected.hasSubtype(weatherlight, "Phyrexian") shouldBe true
            projected.hasSubtype(weatherlight, "Vehicle") shouldBe true
            projected.getPower(weatherlight) shouldBe 5
            projected.getToughness(weatherlight) shouldBe 5
        }

        test("six counters: the seventh is placed first, so the trigger draws instead of scrying") {
            val game = board(phyresis = 6, victim = "Grizzly Bears", removal = "Lightning Bolt")

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            val handBefore = game.handSize(1)
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.phyresis() shouldBe 7
            game.handSize(1) shouldBe handBefore + 1
        }

        test("as a creature it triggers on its own death and reads its last-known counters") {
            val game = board(phyresis = 7, victim = "Grizzly Bears", removal = "Doom Blade")
            val weatherlight = game.findPermanent("Weatherlight Compleated")!!
            game.state.projectedState.isCreature(weatherlight) shouldBe true

            game.castSpell(1, "Doom Blade", weatherlight).error shouldBe null
            val handBefore = game.handSize(1)
            game.resolveStack()

            game.isInGraveyard(1, "Weatherlight Compleated") shouldBe true
            withClue("it left with seven counters, so its own trigger draws a card") {
                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe handBefore + 1
            }
        }
    }
}
