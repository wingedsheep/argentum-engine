package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fell Horseman // Deathly Ride (WOE #92).
 *
 * Creature face: {3}{B} 3/3 Zombie Knight — "When this creature dies, put it on the bottom of its
 * owner's library."
 * Adventure face: Deathly Ride {1}{B}, Sorcery — Adventure — "Return target creature card from
 * your graveyard to your hand."
 */
class FellHorsemanScenarioTest : ScenarioTestBase() {

    init {
        test("dying puts it on the bottom of its owner's library instead of leaving it in the graveyard") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Fell Horseman")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val horseman = game.findPermanent("Fell Horseman")!!

            game.castSpell(2, "Lightning Bolt", horseman).error shouldBe null
            game.resolveStack()

            withClue("the dies trigger relocated the card out of the graveyard") {
                game.isInGraveyard(1, "Fell Horseman") shouldBe false
            }
            withClue("it went to the bottom of its owner's library") {
                game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)).last() shouldBe horseman
            }
        }

        test("Deathly Ride returns a creature card from your graveyard and exiles the card for later") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Fell Horseman")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val horseman = game.findCardsInHand(1, "Fell Horseman").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

            // faceIndex = 0 is the Adventure face (CR 715).
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = horseman,
                    targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                    faceIndex = 0
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears came back to hand") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
            withClue("the Adventure exiled itself rather than going to the graveyard (CR 715.3d)") {
                game.isInExile(1, "Fell Horseman") shouldBe true
                game.isInGraveyard(1, "Fell Horseman") shouldBe false
            }
            withClue("the creature face is castable from exile") {
                game.state.mayPlayPermissions.any {
                    horseman in it.cardIds && it.controllerId == game.player1Id
                } shouldBe true
            }
        }
    }
}
