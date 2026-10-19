package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Wood Elemental (LEG #215).
 *
 * {3}{G} Creature — Elemental, &#42;/&#42;.
 * "As this creature enters, sacrifice any number of untapped Forests. Wood Elemental's power and
 *  toughness are each equal to the number of Forests sacrificed as it entered."
 *
 * The sacrifice count is stamped on the permanent as it enters and read back by the CDA during
 * projection; only *untapped* Forests are offered, and the sacrifice also happens when it is put onto
 * the battlefield by an effect rather than cast.
 */
class WoodElementalScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Wood Elemental") {

            test("power and toughness equal the untapped Forests sacrificed as it entered") {
                // Four Forests pay for it and are tapped by the time it resolves; three stay untapped.
                val game = scenario()
                    .withPlayers("Druid", "Opponent")
                    .withCardInHand(1, "Wood Elemental")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wood Elemental").error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the three untapped Forests are offered — the four that paid are tapped") {
                    decision.options.size shouldBe 3
                }

                game.selectCards(decision.options.take(2)).error shouldBe null
                game.resolveStack()

                val elemental = game.findPermanent("Wood Elemental").shouldNotBeNull()
                val projected = projector.project(game.state)
                withClue("P/T are read back from the recorded sacrifice count during projection") {
                    projected.getPower(elemental) shouldBe 2
                    projected.getToughness(elemental) shouldBe 2
                }
                withClue("the two chosen Forests were sacrificed") {
                    game.findPermanents("Forest").size shouldBe 5
                    game.graveyardSize(1) shouldBe 2
                }
            }

            test("sacrificing nothing makes it a 0/0 that dies to state-based actions") {
                val game = scenario()
                    .withPlayers("Druid", "Opponent")
                    .withCardInHand(1, "Wood Elemental")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wood Elemental").error shouldBe null
                game.resolveStack()
                game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("a 0/0 is put into the graveyard (CR 704.5f)") {
                    game.findPermanent("Wood Elemental").shouldBeNull()
                    game.isInGraveyard(1, "Wood Elemental") shouldBe true
                }
                withClue("no Forest was sacrificed") {
                    game.findPermanents("Forest").size shouldBe 6
                }
            }

            test("put onto the battlefield by Resurrection, it still sacrifices Forests as it enters") {
                val game = scenario()
                    .withPlayers("Druid", "Opponent")
                    .withCardInHand(1, "Resurrection")
                    .withCardInGraveyard(1, "Wood Elemental")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elementalCard = game.state.getGraveyard(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Wood Elemental"
                }
                game.castSpellTargetingGraveyardCard(1, "Resurrection", listOf(elementalCard)).error shouldBe null
                game.resolveStack()

                // Auto-pay may tap Forests for Resurrection's generic cost; whatever stays untapped is offered.
                val untappedForests = game.findPermanents("Forest").filter {
                    game.state.getEntity(it)?.has<TappedComponent>() != true
                }
                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("exactly the untapped Forests are offered") {
                    decision.options.toSet() shouldBe untappedForests.toSet()
                }
                game.selectCards(decision.options).error shouldBe null
                game.resolveStack()

                val elemental = game.findPermanent("Wood Elemental").shouldNotBeNull()
                val projected = projector.project(game.state)
                projected.getPower(elemental) shouldBe untappedForests.size
                projected.getToughness(elemental) shouldBe untappedForests.size
                game.findPermanents("Forest").size shouldBe 5 - untappedForests.size
            }
        }
    }
}
