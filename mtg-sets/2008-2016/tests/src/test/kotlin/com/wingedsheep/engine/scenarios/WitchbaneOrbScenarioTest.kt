package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.CurseOfExhaustion
import com.wingedsheep.mtg.sets.definitions.isd.cards.WitchbaneOrb
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Witchbane Orb (ISD #236) — {4} Artifact.
 *
 * "When this artifact enters, destroy all Curses attached to you. You have hexproof."
 *
 * The enters trigger gathers the Auras attached to its controller (a player host) and destroys every
 * Curse among them, whoever controls it, while leaving Curses attached to other players alone.
 */
class WitchbaneOrbScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(WitchbaneOrb, CurseOfExhaustion))

        fun TestGame.attachToPlayer(aura: EntityId, playerId: EntityId) {
            state = state
                .updateEntity(aura) { it.with(AttachedToComponent(playerId)) }
                .updateEntity(playerId) { c ->
                    val existing = c.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
                    c.with(AttachmentsComponent(existing + aura))
                }
        }

        /** One Curse each side: player 2's enchants player 1, player 1's enchants player 2. */
        fun board(): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Witchbane Orb")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Curse of Exhaustion")
                .withCardOnBattlefield(2, "Curse of Exhaustion")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val curses = game.findPermanents("Curse of Exhaustion")
            val mine = curses.single { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id }
            val theirs = curses.single { it != mine }
            game.attachToPlayer(theirs, game.player1Id)
            game.attachToPlayer(mine, game.player2Id)
            return game
        }

        test("entering destroys the Curse attached to you, not the one attached to the opponent") {
            val game = board()
            game.castSpell(1, "Witchbane Orb").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("the opponent's Curse on you is destroyed") {
                game.isInGraveyard(2, "Curse of Exhaustion") shouldBe true
            }
            withClue("your Curse on the opponent stays") {
                game.isInGraveyard(1, "Curse of Exhaustion") shouldBe false
                game.findPermanents("Curse of Exhaustion").size shouldBe 1
            }
        }

        test("with nothing attached to you, the trigger does nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Witchbane Orb")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Curse of Exhaustion")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.attachToPlayer(game.findPermanent("Curse of Exhaustion")!!, game.player2Id)
            game.castSpell(1, "Witchbane Orb").error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            (game.findPermanent("Witchbane Orb") != null) shouldBe true
            game.findPermanents("Curse of Exhaustion").size shouldBe 1
        }

        test("you have hexproof: the opponent can't enchant you with a Curse") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Witchbane Orb")
                .withCardInHand(2, "Curse of Exhaustion")
                .withLandsOnBattlefield(2, "Plains", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            withClue("targeting you is refused") {
                (game.castSpellTargetingPlayer(2, "Curse of Exhaustion", 1).error != null) shouldBe true
            }
        }
    }
}
