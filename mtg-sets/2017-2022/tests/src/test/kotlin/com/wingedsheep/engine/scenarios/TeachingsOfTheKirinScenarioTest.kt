package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Teachings of the Kirin // Kirin-Touched Orochi (NEO #212).
 *
 * Plays the Saga through (mill three + Spirit, a counter, the flip), then attacks with the Orochi
 * twice: once exiling a creature card for a Spirit, once exiling a noncreature card so the
 * reflexive "when you do" asks for its own target creature *after* the exile resolves.
 */
class TeachingsOfTheKirinScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.toMyNextMain() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        fun TestGame.plusOnes(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        // Modes with no legal target aren't offered, so pick the mode by its text, not its printed index.
        fun TestGame.attackAndChooseMode(modePrefix: String) {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Kirin-Touched Orochi" to 2)).error shouldBe null
            if (getPendingDecision() == null) resolveStack()
            val modeDecision = getPendingDecision() as? ChooseOptionDecision
                ?: error("expected the attack trigger's mode choice; got ${getPendingDecision()}")
            val modeIndex = modeDecision.options.indexOfFirst { it.startsWith(modePrefix) }
            withClue("mode '$modePrefix' is offered in ${modeDecision.options}") { (modeIndex >= 0) shouldBe true }
            submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = modeIndex))
            getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        }

        context("Teachings of the Kirin") {

            test("chapters I-III, then both Orochi attack modes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Teachings of the Kirin")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInGraveyard(2, "Giant Growth")
                    .apply { repeat(12) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Teachings of the Kirin").error shouldBe null
                game.resolveStack()

                withClue("chapter I milled three cards and made a Spirit") {
                    game.graveyardSize(1) shouldBe 3
                    game.findPermanents("Spirit Token").size shouldBe 1
                }
                val spirit = game.findPermanents("Spirit Token").single()

                game.toMyNextMain()
                withClue("chapter II targets a creature you control") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                    game.selectTargets(listOf(spirit)).error shouldBe null
                }
                game.resolveStack()
                game.plusOnes(spirit) shouldBe 1

                game.toMyNextMain()
                game.resolveStack()
                withClue("chapter III returned the Saga transformed") {
                    game.findPermanent("Teachings of the Kirin") shouldBe null
                    game.isOnBattlefield("Kirin-Touched Orochi") shouldBe true
                }
                val orochi = game.findPermanent("Kirin-Touched Orochi")!!

                // First attack: exile a creature card -> create a Spirit.
                game.toMyNextMain()
                game.attackAndChooseMode("Exile target creature card")
                game.selectTargets(listOf(game.findCardsInGraveyard(2, "Hill Giant").single())).error shouldBe null
                game.resolveStack()
                withClue("the creature card is exiled and a second Spirit is created") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.findPermanents("Spirit Token").size shouldBe 2
                }

                // Second attack: exile a noncreature card -> reflexive trigger targets a creature.
                game.toMyNextMain()
                game.attackAndChooseMode("Exile target noncreature card")
                game.selectTargets(listOf(game.findCardsInGraveyard(2, "Giant Growth").single())).error shouldBe null
                game.resolveStack()
                withClue("the noncreature card is exiled before the reflexive trigger chooses its target") {
                    game.isInExile(2, "Giant Growth") shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(orochi)).error shouldBe null
                game.resolveStack()
                withClue("the +1/+1 counter lands on the chosen creature") {
                    game.plusOnes(orochi) shouldBe 1
                    game.findPermanents("Spirit Token").size shouldBe 2
                }
            }
        }
    }
}
