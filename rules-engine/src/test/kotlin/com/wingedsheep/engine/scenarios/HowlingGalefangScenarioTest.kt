package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Howling Galefang. */
class HowlingGalefangScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("Howling Galefang — haste while you own an Adventure card in exile") {
            test("no Adventure card in exile: vigilance only, no haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Howling Galefang")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fang = game.findPermanent("Howling Galefang").shouldNotBeNull()
                val projected = game.state.projectedState

                projected.hasKeyword(fang, Keyword.VIGILANCE) shouldBe true
                withClue("nothing in exile, so the conditional grant is off") {
                    projected.hasKeyword(fang, Keyword.HASTE) shouldBe false
                }
            }

            test("an Adventure card you own in exile turns haste on, however it got there") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Besotted Knight // Betroth the Beast — an Adventure card. Per the WOE ruling
                    // it does not matter that it was not cast as an Adventure.
                    .withCardInExile(1, "Besotted Knight")
                    .withCardOnBattlefield(1, "Howling Galefang")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fang = game.findPermanent("Howling Galefang").shouldNotBeNull()

                withClue("a card with an Adventure sits in your exile zone") {
                    game.state.projectedState.hasKeyword(fang, Keyword.HASTE) shouldBe true
                }
            }

            test("an Adventure card in an opponent's exile does not grant it — 'you own'") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInExile(2, "Besotted Knight")
                    .withCardOnBattlefield(1, "Howling Galefang")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fang = game.findPermanent("Howling Galefang").shouldNotBeNull()

                withClue("the exiled Adventure is the opponent's, so no haste") {
                    game.state.projectedState.hasKeyword(fang, Keyword.HASTE) shouldBe false
                }
            }

            test("a non-Adventure card in your exile does not grant haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Howling Galefang")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fang = game.findPermanent("Howling Galefang").shouldNotBeNull()

                withClue("Grizzly Bears has no Adventure") {
                    game.state.projectedState.hasKeyword(fang, Keyword.HASTE) shouldBe false
                }
            }
        }
    }
}
