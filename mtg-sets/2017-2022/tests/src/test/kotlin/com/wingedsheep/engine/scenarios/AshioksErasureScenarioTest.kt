package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ashiok's Erasure (THB #43): the enters trigger exiles a spell into the Erasure's linked pile,
 * opponents can't cast spells sharing a name with it (either half of a split card — ruling), and
 * the card returns to its owner's hand when the Erasure leaves.
 */
class AshioksErasureScenarioTest : ScenarioTestBase() {

    private fun TestGame.handCard(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getHand(playerId).first { state.getEntity(it)?.get<CardComponent>()?.name == name }
    }

    /** The opponent's spell [spellName] is on the stack; flash in the Erasure and exile it. */
    private fun TestGame.eraseTopSpell(spellName: String) {
        val spell = state.stack.first { state.getEntity(it)?.get<CardComponent>()?.name == spellName }
        passPriority() // the opponent passes; we get the instant-speed window
        castSpell(1, "Ashiok's Erasure").error shouldBe null
        resolveStack() // the Erasure resolves; its enters trigger asks for a target
        if (state.pendingDecision != null) selectTargets(listOf(spell)).error shouldBe null
        resolveStack()
        withClue("the spell is exiled, not resolved") {
            isInExile(2, spellName) shouldBe true
        }
    }

    init {
        test("exiles the spell, locks its name for opponents, and returns it when the Erasure leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ashiok's Erasure")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Disenchant")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.eraseTopSpell("Grizzly Bears")
            game.findPermanent("Grizzly Bears") shouldBe null

            withClue("an opponent can't cast a spell with the exiled card's name") {
                game.castSpell(2, "Grizzly Bears").error shouldNotBe null
            }

            val erasure = game.findPermanent("Ashiok's Erasure").shouldNotBeNull()
            game.castSpell(2, "Disenchant", erasure).error shouldBe null
            game.resolveStack()

            withClue("the exiled card returns to its owner's hand") {
                game.isInExile(2, "Grizzly Bears") shouldBe false
                game.findCardsInHand(2, "Grizzly Bears").size shouldBe 2
            }
            withClue("with the Erasure gone, the name is castable again") {
                game.castSpell(2, "Grizzly Bears").error shouldBe null
            }
        }

        test("an exiled split card locks both of its halves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Ashiok's Erasure")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(1, "Sol Ring")
                .withCardInHand(2, "Wear // Tear")
                .withCardInHand(2, "Wear // Tear")
                .withLandsOnBattlefield(2, "Mountain", 3)
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val solRing = game.findPermanent("Sol Ring")!!
            game.execute(
                CastSpell(
                    game.player2Id, game.handCard(2, "Wear // Tear"), faceIndex = 0,
                    targets = listOf(ChosenTarget.Permanent(solRing))
                )
            ).error shouldBe null
            game.eraseTopSpell("Wear // Tear")
            withClue("Wear never resolved") { game.findPermanent("Sol Ring") shouldBe solRing }

            val erasure = game.findPermanent("Ashiok's Erasure").shouldNotBeNull()
            withClue("Tear shares a name with the exiled Wear // Tear, so it can't be cast") {
                game.execute(
                    CastSpell(
                        game.player2Id, game.handCard(2, "Wear // Tear"), faceIndex = 1,
                        targets = listOf(ChosenTarget.Permanent(erasure))
                    )
                ).error shouldNotBe null
            }
        }
    }
}
