package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Guardian of the Great Door (LCI #16) — "As an additional cost to cast this spell, tap four
 * untapped artifacts, creatures, and/or lands you control.\nFlying"
 *
 * Exercises the `AdditionalCost.TapPermanents` cast additional cost over a combined
 * artifact-or-creature-or-land filter (CR 601.2f — paid as the spell is cast), and verifies
 * that the resolved permanent has flying.
 */
class GuardianOfTheGreatDoorScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Guardian of the Great Door — tap-four additional cost") {

            test("taps the four chosen lands and resolves onto the battlefield with flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Guardian of the Great Door")
                    // Two Plains for {W}{W} mana + four more to tap as the additional cost.
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spellId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Guardian of the Great Door"
                }
                val allPlains = game.state.getBattlefield(game.player1Id).filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Plains"
                }
                // Pick four Plains for the tap additional cost; the remaining two pay {W}{W}.
                val additionalTaps = allPlains.take(4)

                val cast = game.execute(
                    CastSpell(
                        game.player1Id, spellId, emptyList(),
                        additionalCostPayment = AdditionalCostPayment(tappedPermanents = additionalTaps)
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("All four chosen permanents are tapped by the additional cost") {
                    additionalTaps.forEach { id ->
                        game.state.getEntity(id)?.has<TappedComponent>() shouldBe true
                    }
                }
                withClue("Guardian of the Great Door resolves onto the battlefield") {
                    game.isOnBattlefield("Guardian of the Great Door") shouldBe true
                }
                val guardianId = game.state.getBattlefield(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Guardian of the Great Door"
                }
                withClue("Guardian of the Great Door has flying") {
                    projector.project(game.state).hasKeyword(guardianId, Keyword.FLYING) shouldBe true
                }
            }

            test("taps mix of creatures and lands as the additional cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Guardian of the Great Door")
                    // Two Plains for {W}{W} mana, plus two Grizzly Bears and two Plains for the tap cost.
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spellId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Guardian of the Great Door"
                }
                val plains = game.state.getBattlefield(game.player1Id).filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Plains"
                }
                val bears = game.state.getBattlefield(game.player1Id).filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                // Tap two Plains + two Grizzly Bears as the additional cost.
                val additionalTaps = plains.take(2) + bears

                val cast = game.execute(
                    CastSpell(
                        game.player1Id, spellId, emptyList(),
                        additionalCostPayment = AdditionalCostPayment(tappedPermanents = additionalTaps)
                    )
                )
                withClue("Cast with mixed creature/land taps should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Guardian of the Great Door resolves onto the battlefield") {
                    game.isOnBattlefield("Guardian of the Great Door") shouldBe true
                }
            }

            test("cannot be cast when fewer than four untapped permanents are tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Guardian of the Great Door")
                    // Only two Plains — enough mana but not enough for the four-tap additional cost.
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spellId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Guardian of the Great Door"
                }

                val cast = game.execute(
                    CastSpell(
                        game.player1Id, spellId, emptyList(),
                        additionalCostPayment = AdditionalCostPayment(tappedPermanents = emptyList())
                    )
                )
                withClue("Cast should fail when the additional cost is not paid") {
                    (cast.error != null) shouldBe true
                }
                game.isOnBattlefield("Guardian of the Great Door") shouldBe false
            }
        }
    }
}
