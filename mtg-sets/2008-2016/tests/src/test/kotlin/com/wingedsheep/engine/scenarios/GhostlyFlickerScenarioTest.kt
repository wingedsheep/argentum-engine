package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ghostly Flicker (AVR #57) — {2}{U} Instant.
 *
 * "Exile two target artifacts, creatures, and/or lands you control, then return those cards to
 * the battlefield under your control."
 *
 * The card composes a linked-exile pipeline from an instant: both targets leave together and the
 * same batch returns as new objects under the *caster's* control (not the owner's).
 */
class GhostlyFlickerScenarioTest : ScenarioTestBase() {

    init {
        context("Ghostly Flicker") {

            fun ScenarioTestBase.TestGame.cast(first: EntityId, second: EntityId) {
                val cardId = state.getHand(player1Id).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == "Ghostly Flicker"
                }
                execute(CastSpell(player1Id, cardId, listOf(ChosenTarget.Permanent(first), ChosenTarget.Permanent(second))))
                    .error shouldBe null
                resolveStack()
            }

            test("an artifact and a land of different types both return untapped as new objects") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ghostly Flicker")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(1, "Ornithopter", tapped = true)
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val thopter = game.findPermanent("Ornithopter")!!
                val forest = game.findPermanent("Forest")!!
                game.cast(thopter, forest)

                val newThopter = game.findPermanent("Ornithopter")
                val newForest = game.findPermanent("Forest")
                withClue("both cards came back") {
                    newThopter shouldNotBe null
                    newForest shouldNotBe null
                }
                withClue("they re-entered the battlefield untapped") {
                    game.state.getEntity(newThopter!!)!!.has<TappedComponent>() shouldBe false
                    game.state.getEntity(newForest!!)!!.has<TappedComponent>() shouldBe false
                }
                game.isInGraveyard(1, "Ghostly Flicker") shouldBe true
            }

            test("a creature you control but don't own returns under your control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ghostly Flicker")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 owns the Bears; player 1 controls them.
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { container ->
                    val card = container.get<CardComponent>()!!
                    container
                        .with(card.copy(ownerId = game.player2Id))
                        .with(OwnerComponent(game.player2Id))
                }
                val thopter = game.findPermanent("Ornithopter")!!
                game.cast(bears, thopter)

                val newBears = game.findPermanent("Grizzly Bears")
                withClue("the Bears returned") { newBears shouldNotBe null }
                withClue("under the caster's control, not the owner's") {
                    game.state.getEntity(newBears!!)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
                    game.state.getEntity(newBears)!!.get<OwnerComponent>()!!.playerId shouldBe game.player2Id
                }
                game.state.getBattlefield() shouldContain game.findPermanent("Ornithopter")!!
            }
        }
    }
}
