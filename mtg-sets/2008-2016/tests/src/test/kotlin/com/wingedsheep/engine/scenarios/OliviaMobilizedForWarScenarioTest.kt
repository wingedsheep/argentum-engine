package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Olivia, Mobilized for War (SOI #248): "Whenever another creature you control enters, you may
 * discard a card. If you do, put a +1/+1 counter on that creature, it gains haste until end of
 * turn, and it becomes a Vampire in addition to its other types."
 *
 * The rider acts on the triggering creature after the discard's card choice resumes the
 * ability, so the test proves the triggering entity survives that pause, and that only haste
 * wears off at end of turn while the counter and the Vampire subtype stay.
 */
class OliviaMobilizedForWarScenarioTest : ScenarioTestBase() {
    init {
        fun setup() = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Olivia, Mobilized for War")
            .withCardInHand(1, "Grizzly Bears")
            .withCardInHand(1, "Island")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("discarding gives the entering creature a counter, haste, and the Vampire type") {
            val game = setup()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            val island = game.findCardsInHand(1, "Island").singleOrNull()
            if (game.hasPendingDecision() && island != null) game.selectCards(listOf(island)).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("the discarded card is in the graveyard") { game.isInGraveyard(1, "Island") shouldBe true }
            withClue("a +1/+1 counter on the entering creature") {
                game.state.getEntity(bears)!!.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.getPower(bears) shouldBe 3
            }
            withClue("it gains haste") { game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true }
            withClue("it becomes a Vampire in addition to its other types") {
                game.state.projectedState.hasSubtype(bears, "Vampire") shouldBe true
                game.state.projectedState.hasSubtype(bears, "Bear") shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            withClue("haste ends at end of turn") {
                game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
            }
            withClue("the counter and the Vampire subtype stay") {
                game.state.projectedState.hasSubtype(bears, "Vampire") shouldBe true
                game.state.projectedState.getPower(bears) shouldBe 3
            }
        }

        test("declining keeps the card and leaves the creature unchanged") {
            val game = setup()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.isInHand(1, "Island") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
            game.state.projectedState.hasSubtype(bears, "Vampire") shouldBe false
        }
    }
}
