package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rot Farm Mortipede (MKM #102) — {3}{B} 3/4 Creature — Insect.
 *
 * "Whenever one or more creature cards leave your graveyard, this creature gets +1/+0 and gains
 *  menace and lifelink until end of turn."
 *
 * Pins the filter on the batching `CardsLeaveYourGraveyard` trigger: a *creature* card leaving
 * pumps it, a noncreature card leaving does not.
 */
class RotFarmMortipedeScenarioTest : ScenarioTestBase() {

    init {
        context("Rot Farm Mortipede — creature cards leaving your graveyard") {

            test("a creature card leaving your graveyard pumps it and grants menace and lifelink") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Rot Farm Mortipede")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .build()

                val mortipede = game.findPermanent("Rot Farm Mortipede")!!
                withClue("Printed stats before anything leaves the graveyard") {
                    game.state.projectedState.getPower(mortipede) shouldBe 3
                    game.state.projectedState.hasKeyword(mortipede, Keyword.MENACE) shouldBe false
                    game.state.projectedState.hasKeyword(mortipede, Keyword.LIFELINK) shouldBe false
                }

                game.castSpellTargetingGraveyardCard(1, "Raise Dead", 1, "Grizzly Bears")
                    .error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears left the graveyard for hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }

                val projected = game.state.projectedState
                withClue("+1/+0 and both keywords until end of turn") {
                    projected.getPower(mortipede) shouldBe 4
                    projected.getToughness(mortipede) shouldBe 4
                    projected.hasKeyword(mortipede, Keyword.MENACE) shouldBe true
                    projected.hasKeyword(mortipede, Keyword.LIFELINK) shouldBe true
                }
            }

            test("a noncreature card leaving your graveyard does not trigger it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Rot Farm Mortipede")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInHand(1, "Regrowth")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .build()

                val mortipede = game.findPermanent("Rot Farm Mortipede")!!
                game.castSpellTargetingGraveyardCard(1, "Regrowth", 1, "Lightning Bolt")
                    .error shouldBe null
                game.resolveStack()

                withClue("Lightning Bolt left the graveyard") {
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                }
                withClue("No creature card left, so no pump") {
                    game.state.projectedState.getPower(mortipede) shouldBe 3
                    game.state.projectedState.hasKeyword(mortipede, Keyword.MENACE) shouldBe false
                }
            }
        }
    }
}
