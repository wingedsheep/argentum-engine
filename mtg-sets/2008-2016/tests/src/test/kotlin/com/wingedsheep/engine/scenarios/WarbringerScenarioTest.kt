package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DashedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Warbringer (DTK #168) — {3}{R} 3/3 Orc Berserker. "Dash costs you pay cost {2} less (as long as
 * this creature is on the battlefield). Dash {2}{R}"
 *
 * On the battlefield it takes {2} off every dash cost its controller pays — generic mana only, so
 * Screamreach Brawler's {1}{R} dash costs {R} — while the same cards' mana costs are untouched. In
 * hand it discounts nothing, including its own dash.
 */
class WarbringerScenarioTest : ScenarioTestBase() {

    init {
        context("Warbringer") {

            fun TestGame.offerCost(player: Int, name: String, actionType: String): String? {
                val card = findCardsInHand(player, name).first()
                return getLegalActions(player)
                    .firstOrNull { (it.action as? CastSpell)?.cardId == card && it.actionType == actionType }
                    ?.manaCostString
            }

            test("on the battlefield it discounts a dash cast to {R}, and the dash cast resolves for one Mountain") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Warbringer")
                    .withCardInHand(1, "Screamreach Brawler")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the dash offer loses {1} of generic — it can't take the {R}") {
                    game.offerCost(1, "Screamreach Brawler", "CastWithDash") shouldBe "{R}"
                }
                withClue("the normal cast pays the mana cost, which Warbringer doesn't touch") {
                    game.offerCost(1, "Screamreach Brawler", "CastSpell") shouldBe "{2}{R}"
                }

                val brawler = game.findCardsInHand(1, "Screamreach Brawler").first()
                val cast = game.execute(
                    CastSpell(game.player1Id, brawler, useAlternativeCost = true, alternativeCostType = AlternativeCostType.DASH)
                )
                withClue("dash cast for {R}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                val permanent = game.findPermanent("Screamreach Brawler")
                permanent shouldNotBe null
                game.state.getEntity(permanent!!)?.has<DashedComponent>() shouldBe true
            }

            test("in hand it discounts nothing, its own dash included; a second one on the battlefield does") {
                val alone = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Warbringer")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                alone.offerCost(1, "Warbringer", "CastWithDash") shouldBe "{2}{R}"

                val paired = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Warbringer")
                    .withCardInHand(1, "Warbringer")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                paired.offerCost(1, "Warbringer", "CastWithDash") shouldBe "{R}"
            }

            test("an opponent's Warbringer doesn't discount your dash costs") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(2, "Warbringer")
                    .withCardInHand(1, "Screamreach Brawler")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.offerCost(1, "Screamreach Brawler", "CastWithDash") shouldBe "{1}{R}"
            }
        }
    }
}
