package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stump Stomp // Burnwillow Clearing (MH3).
 *
 * Front: "Target creature you control deals damage equal to its power to target creature or
 * planeswalker you don't control."
 * Back: "This land enters tapped. {T}: Add {R} or {G}."
 */
class StumpStompScenarioTest : ScenarioTestBase() {

    private fun TestGame.castStomp(yours: EntityId, theirs: EntityId) =
        execute(
            CastSpell(
                player1Id,
                state.getHand(player1Id).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == "Stump Stomp"
                },
                listOf(ChosenTarget.Permanent(yours), ChosenTarget.Permanent(theirs)),
            )
        )

    init {
        context("Stump Stomp — the sorcery front") {

            test("your creature deals damage equal to its power, one-sided") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stump Stomp")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castStomp(giant, bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe true
                (game.state.getEntity(giant)!!.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }

            test("the damage scales with the creature's power, not a fixed amount") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stump Stomp")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castStomp(bears, giant).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
                game.state.getEntity(giant)!!.get<DamageComponent>()!!.amount shouldBe 2
            }

            test("the second target must be one you don't control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stump Stomp")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castStomp(giant, bears).error shouldNotBe null
            }
        }

        context("Burnwillow Clearing — the land back") {

            test("played as a land, it enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stump Stomp")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null

                val land = game.findPermanent("Burnwillow Clearing")!!
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
