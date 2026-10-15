package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tenacious Underdog (SNC #97) — {1}{B} Creature — Human Warrior 3/2
 *
 *   Blitz—{2}{B}{B}, Pay 2 life.
 *   You may cast this card from your graveyard using its blitz ability.
 *
 * The loop the card exists for: cast it from the graveyard for its blitz cost (mana and life), it
 * attacks with haste, is sacrificed at the end step, draws a card, and is back in the graveyard
 * ready to go again.
 */
class TenaciousUnderdogScenarioTest : ScenarioTestBase() {

    init {
        context("Tenacious Underdog") {

            test("blitzed from the graveyard: pays 2 life, has haste, and is sacrificed for a card at end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Tenacious Underdog")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val underdog = game.findCardsInGraveyard(1, "Tenacious Underdog").single()
                val result = game.execute(
                    CastSpell(
                        game.player1Id, underdog,
                        useAlternativeCost = true, alternativeCostType = AlternativeCostType.BLITZ
                    )
                )
                withClue("blitz cast from the graveyard should succeed: ${result.error}") { result.error shouldBe null }
                game.getLifeTotal(1) shouldBe 18
                game.resolveStack()

                val permanent = game.findPermanent("Tenacious Underdog")
                permanent shouldNotBe null
                game.state.projectedState.hasKeyword(permanent!!, Keyword.HASTE) shouldBe true

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.findPermanent("Tenacious Underdog") shouldBe null
                game.isInGraveyard(1, "Tenacious Underdog") shouldBe true
                withClue("the blitz dies trigger draws a card") { game.handSize(1) shouldBe handBefore + 1 }
            }

            test("its normal cost can't be paid from the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Tenacious Underdog")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val underdog = game.findCardsInGraveyard(1, "Tenacious Underdog").single()
                game.execute(CastSpell(game.player1Id, underdog)).error shouldNotBe null
                game.isInGraveyard(1, "Tenacious Underdog") shouldBe true
            }
        }
    }
}
