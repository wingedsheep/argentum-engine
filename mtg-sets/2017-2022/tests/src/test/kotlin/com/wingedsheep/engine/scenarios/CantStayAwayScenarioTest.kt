package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Can't Stay Away (MID #213) — "Return target creature card with mana value 3 or less from your
 * graveyard to the battlefield. It gains 'If this creature would die, exile it instead.'
 * Flashback {3}{W}{B}"
 */
class CantStayAwayScenarioTest : ScenarioTestBase() {

    init {
        context("Can't Stay Away") {

            test("returns the creature, which is exiled instead of dying — even on a later turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Can't Stay Away")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Can't Stay Away", 1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.findPermanent("Grizzly Bears") shouldNotBe null

                // Past this turn's cleanup: the granted ability isn't a "this turn" rider.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }

            test("a sacrificed returned creature is exiled too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Can't Stay Away")
                    .withCardInHand(1, "Sacrifice")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Can't Stay Away", 1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.findPermanent("Grizzly Bears") shouldNotBe null

                game.castSpellWithAdditionalSacrifice(1, "Sacrifice", "Grizzly Bears").error shouldBe null

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }

            test("flashback from the graveyard returns a creature and exiles Can't Stay Away") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Can't Stay Away")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInGraveyard(1, "Can't Stay Away").first()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                game.execute(
                    CastSpell(game.player1Id, spell, listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)))
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.isInExile(1, "Can't Stay Away") shouldBe true
            }

            test("can't target a creature card with mana value 4 or more") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Can't Stay Away")
                    .withCardInGraveyard(1, "Gurmag Angler")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Can't Stay Away", 1, "Gurmag Angler").error shouldNotBe null
            }
        }
    }
}
