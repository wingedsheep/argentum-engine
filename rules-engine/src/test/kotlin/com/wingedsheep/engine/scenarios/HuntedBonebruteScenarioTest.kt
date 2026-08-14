package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hunted Bonebrute (MKM #87) — {2}{B} 6/2 Creature — Skeleton Beast.
 *
 * "Menace"
 * "When this creature enters, target opponent creates two 1/1 white Dog creature tokens."
 * "When this creature dies, each opponent loses 3 life."
 * "Disguise {1}{B}"
 *
 * The drawback is the interesting half: the two Dogs must arrive under the *targeted opponent's*
 * control, not the caster's — a token effect that quietly defaults to the ability's controller
 * turns a drawback into an upside and nothing else would catch it. The death trigger is checked
 * separately because it is `each opponent`, not the player who received the Dogs.
 */
class HuntedBonebruteScenarioTest : ScenarioTestBase() {

    private fun dogsControlledBy(game: TestGame, playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Dog Token"
        }
    }

    init {
        context("Hunted Bonebrute") {

            test("entering gives the targeted opponent two Dogs") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Hunted Bonebrute")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .build()

                game.castSpell(1, "Hunted Bonebrute").error shouldBe null
                game.resolveStack()

                withClue("the Dogs belong to the opponent, not the Skeleton's controller") {
                    dogsControlledBy(game, 2) shouldBe 2
                    dogsControlledBy(game, 1) shouldBe 0
                }
                game.isOnBattlefield("Hunted Bonebrute") shouldBe true
            }

            test("dying drains each opponent for 3") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Hunted Bonebrute")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .build()

                val brute = game.findPermanent("Hunted Bonebrute")!!
                game.castSpell(1, "Murder", brute).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Hunted Bonebrute") shouldBe true
                withClue("each opponent — the controller's own life is untouched") {
                    game.getLifeTotal(2) shouldBe 17
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
