package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Friendly Fire (FRF #101, {3}{R} Instant).
 *
 *   Target creature's controller reveals a card at random from their hand. Friendly Fire deals
 *   damage to that creature and that player equal to the revealed card's mana value.
 *
 * The hand searched and the player damaged are the *creature's controller*, not the caster.
 */
class FriendlyFireScenarioTest : ScenarioTestBase() {

    init {
        test("reveals from the creature controller's hand and damages both creature and controller") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Friendly Fire")
                .withCardInHand(1, "Grizzly Bears") // caster's hand must not be the one revealed
                .withCardInHand(2, "Hill Giant") // mana value 4
                .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val casterLife = game.getLifeTotal(1)
            val opponentLife = game.getLifeTotal(2)

            game.castSpell(1, "Friendly Fire", game.findPermanent("Craw Wurm")!!)
            game.resolveStack()

            withClue("4 damage kills the 6/4") { game.isInGraveyard(2, "Craw Wurm").shouldBeTrue() }
            withClue("its controller takes the same 4") { game.getLifeTotal(2) shouldBe opponentLife - 4 }
            withClue("the caster is untouched") { game.getLifeTotal(1) shouldBe casterLife }
            withClue("the revealed card stays in hand") { game.handSize(2) shouldBe 1 }
        }

        test("an empty hand reveals nothing and deals no damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Friendly Fire")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val opponentLife = game.getLifeTotal(2)
            val wurm = game.findPermanent("Craw Wurm")!!

            game.castSpell(1, "Friendly Fire", wurm)
            game.resolveStack()

            game.isOnBattlefield("Craw Wurm").shouldBeTrue()
            withClue("no damage on the creature") {
                (game.state.getEntity(wurm)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }
            game.getLifeTotal(2) shouldBe opponentLife
        }

        test("targeting your own creature reveals from your hand and damages you") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Friendly Fire")
                .withCardInHand(1, "Grizzly Bears") // mana value 2
                .withCardInHand(2, "Hill Giant")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val casterLife = game.getLifeTotal(1)
            val opponentLife = game.getLifeTotal(2)
            val wurm = game.findPermanent("Craw Wurm")!!

            game.castSpell(1, "Friendly Fire", wurm)
            game.resolveStack()

            game.state.getEntity(wurm)?.get<DamageComponent>()?.amount shouldBe 2
            game.getLifeTotal(1) shouldBe casterLife - 2
            game.getLifeTotal(2) shouldBe opponentLife
        }
    }
}
