package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.TheCelestus
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for The Celestus (MID #252).
 *
 * "If it's neither day nor night, it becomes day as The Celestus enters.
 *  {T}: Add one mana of any color.
 *  {3}, {T}: If it's night, it becomes day. Otherwise, it becomes night. Activate only as a sorcery.
 *  Whenever day becomes night or night becomes day, you gain 1 life. You may draw a card. If you do,
 *  discard a card."
 */
class TheCelestusScenarioTest : ScenarioTestBase() {

    private val toggle = TheCelestus.activatedAbilities[1].id

    private fun TestGame.activateToggle() =
        execute(ActivateAbility(playerId = player1Id, sourceId = findPermanent("The Celestus")!!, abilityId = toggle))

    private fun celestusOnBoard(step: Step = Step.PRECOMBAT_MAIN, phase: Phase = Phase.PRECOMBAT_MAIN) =
        scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Celestus")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withCardInHand(1, "Grizzly Bears")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(phase, step)
            .build()

    init {
        context("The Celestus") {

            test("entering while it's neither day nor night makes it day without triggering") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "The Celestus")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state.dayNight shouldBe null

                game.castSpell(1, "The Celestus").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("The Celestus") shouldBe true
                withClue("it becomes day as The Celestus enters") { game.state.dayNight shouldBe DayNight.DAY }
                withClue("the first designation isn't a day/night change, so no life gain") {
                    game.getLifeTotal(1) shouldBe 20
                }
                game.hasPendingDecision() shouldBe false
            }

            test("during the day the toggle makes it night; the trigger gains 1 life and loots") {
                val game = celestusOnBoard()
                game.state = game.state.copy(dayNight = DayNight.DAY)

                game.activateToggle().error shouldBe null
                game.resolveStack()

                game.state.dayNight shouldBe DayNight.NIGHT
                game.getLifeTotal(1) shouldBe 21
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                val discard = game.getPendingDecision()
                discard.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(discard.options.take(1)).error shouldBe null
                game.resolveStack()

                withClue("drew one, discarded one") {
                    game.handSize(1) shouldBe 1
                    game.graveyardSize(1) shouldBe 1
                }
            }

            test("at night the toggle makes it day; declining the loot still gains 1 life") {
                val game = celestusOnBoard()
                game.state = game.state.copy(dayNight = DayNight.NIGHT)

                game.activateToggle().error shouldBe null
                game.resolveStack()

                game.state.dayNight shouldBe DayNight.DAY
                game.getLifeTotal(1) shouldBe 21
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.graveyardSize(1) shouldBe 0
            }

            test("the toggle can only be activated as a sorcery") {
                val game = celestusOnBoard(step = Step.BEGIN_COMBAT, phase = Phase.COMBAT)
                game.state = game.state.copy(dayNight = DayNight.DAY)

                game.activateToggle().error shouldNotBe null
                game.state.dayNight shouldBe DayNight.DAY
            }
        }
    }
}
