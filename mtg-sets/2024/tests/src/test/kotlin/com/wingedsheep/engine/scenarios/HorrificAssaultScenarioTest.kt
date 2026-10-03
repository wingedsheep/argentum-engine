package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Horrific Assault (MH3) — "Target creature you control deals damage equal to its power to target
 * creature or planeswalker you don't control. If you control an Eldrazi, you gain 3 life."
 */
class HorrificAssaultScenarioTest : ScenarioTestBase() {

    private fun TestGame.castAssault(yours: EntityId, theirs: EntityId) =
        execute(
            CastSpell(
                player1Id,
                state.getHand(player1Id).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == "Horrific Assault"
                },
                listOf(ChosenTarget.Permanent(yours), ChosenTarget.Permanent(theirs)),
            )
        )

    init {
        test("deals damage equal to power one-sided; no Eldrazi means no life gain") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Horrific Assault")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Hill Giant") // 3/3
                .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castAssault(giant, bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            (game.state.getEntity(giant)!!.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            game.getLifeTotal(1) shouldBe 20
        }

        test("with an Eldrazi you gain 3 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Horrific Assault")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Eldrazi Linebreaker") // 3/3 Eldrazi
                .withCardOnBattlefield(2, "Hill Giant") // 3/3
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val eldrazi = game.findPermanent("Eldrazi Linebreaker")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castAssault(eldrazi, giant).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.getLifeTotal(1) shouldBe 23
        }

        test("if your creature is gone, no damage is dealt but the life gain still happens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Horrific Assault")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Eldrazi Linebreaker")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castAssault(bears, giant).error shouldBe null
            // Bolt our own creature in response: the first target becomes illegal.
            val bolt = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Bolt"
            }
            game.execute(CastSpell(game.player1Id, bolt, listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe true
            (game.state.getEntity(giant)!!.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
