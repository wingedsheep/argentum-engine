package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.DashedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Sprinting Warbrute (DTK #157) — {4}{R} 5/4 Ogre Berserker.
 *
 * "This creature attacks each combat if able. Dash {3}{R}"
 *
 * Pins that an untapped, able Warbrute can't be left home, and that dashing it in for {3}{R} gives
 * it haste so it attacks the turn it arrives.
 */
class SprintingWarbruteScenarioTest : ScenarioTestBase() {

    init {
        context("Sprinting Warbrute") {

            test("must attack each combat if able") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sprinting Warbrute", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val noAttack = game.execute(DeclareAttackers(game.player1Id, emptyMap()))
                withClue("declaring no attackers is illegal while the Warbrute can attack") {
                    noAttack.error shouldNotBe null
                    noAttack.error!! shouldContain "must attack"
                }
                game.declareAttackers(mapOf("Sprinting Warbrute" to 2)).error shouldBe null
            }

            test("dashed for {3}{R}, it has haste and attacks the turn it enters") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Sprinting Warbrute")
                    .withLandsOnBattlefield(1, "Mountain", 4) // enough for dash, not for {4}{R}
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.findCardsInHand(1, "Sprinting Warbrute").first()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.DASH
                    )
                )
                withClue("dash cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val warbrute = game.findPermanent("Sprinting Warbrute")!!
                game.state.getEntity(warbrute)?.has<DashedComponent>() shouldBe true
                game.state.projectedState.hasKeyword(warbrute, Keyword.HASTE) shouldBe true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sprinting Warbrute" to 2)).error shouldBe null
            }
        }
    }
}
