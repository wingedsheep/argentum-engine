package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Reverent Howl (HOB) — {2}{B} Instant.
 *
 * "Choose one —
 *  • Target player draws two cards and loses 2 life.
 *  • Target creature gets +2/+2 and gains lifelink until end of turn."
 *
 * Each mode has its own target *type*, so both are exercised end to end: the draw/lose mode
 * against a player, the pump mode against a creature.
 */
class ReverentHowlScenarioTest : ScenarioTestBase() {

    init {
        context("Reverent Howl") {

            test("mode 0 — target player draws two cards and loses 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reverent Howl")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.state.getHand(game.player1Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Reverent Howl"
                }
                val target = listOf(ChosenTarget.Player(game.player2Id))

                game.execute(
                    CastSpell(
                        game.player1Id, spell, target,
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(target)
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the targeted opponent drew two cards") {
                    game.handSize(2) shouldBe 2
                    game.librarySize(2) shouldBe 1
                }
                withClue("and lost 2 life") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("the caster is untouched") {
                    game.getLifeTotal(1) shouldBe 20
                    game.handSize(1) shouldBe 0
                }
            }

            test("mode 1 — target creature gets +2/+2 and lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reverent Howl")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                withClue("Centaur Courser starts as a 3/3 without lifelink") {
                    game.state.projectedState.getPower(courser) shouldBe 3
                    game.state.projectedState.hasKeyword(courser, Keyword.LIFELINK) shouldBe false
                }

                game.castSpellWithMode(1, "Reverent Howl", modeIndex = 1, targetId = courser)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("both halves of the mode applied to the same creature") {
                    game.state.projectedState.getPower(courser) shouldBe 5
                    game.state.projectedState.getToughness(courser) shouldBe 5
                    game.state.projectedState.hasKeyword(courser, Keyword.LIFELINK) shouldBe true
                }
                withClue("mode 1 draws no cards and costs no life") {
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
