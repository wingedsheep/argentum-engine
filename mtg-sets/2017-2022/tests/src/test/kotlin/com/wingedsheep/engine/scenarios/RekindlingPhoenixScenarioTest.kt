package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rekindling Phoenix (RIX #111) — "Flying. When this creature dies, create a 0/1 red Elemental
 * creature token with 'At the beginning of your upkeep, sacrifice this token and return target
 * card named Rekindling Phoenix from your graveyard to the battlefield. It gains haste until end
 * of turn.'"
 *
 * Pins the token's granted, targeted upkeep trigger: it targets the Phoenix in the graveyard,
 * sacrifices the token, and the returned Phoenix keeps the haste grant across the zone change.
 */
class RekindlingPhoenixScenarioTest : ScenarioTestBase() {

    init {
        test("dies into an Elemental that returns it with haste on your next upkeep") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Rekindling Phoenix")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .apply { repeat(3) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Lightning Bolt", game.findPermanent("Rekindling Phoenix")!!).error shouldBe null
            game.resolveStack()

            withClue("the Phoenix died and left a 0/1 Elemental behind") {
                game.isInGraveyard(1, "Rekindling Phoenix") shouldBe true
                game.findPermanent("Elemental Token") shouldNotBe null
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            val phoenixCard = game.findCardsInGraveyard(1, "Rekindling Phoenix").single()
            if (game.getPendingDecision() is ChooseTargetsDecision) {
                game.selectTargets(listOf(phoenixCard))
            }
            game.resolveStack()

            val phoenix = game.findPermanent("Rekindling Phoenix")
            withClue("the Phoenix is back on the battlefield") { phoenix shouldNotBe null }
            withClue("it gained haste until end of turn") {
                game.state.projectedState.hasKeyword(phoenix!!, Keyword.HASTE) shouldBe true
            }
            withClue("the Elemental was sacrificed") { game.findPermanent("Elemental Token") shouldBe null }
        }
    }
}
