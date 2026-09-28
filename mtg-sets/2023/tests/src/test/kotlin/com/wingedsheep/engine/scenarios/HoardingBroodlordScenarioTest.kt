package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hoarding Broodlord (MOM #110) — {5}{B}{B}{B} 7/6 convoke, flying. "When this creature enters,
 * search your library for a card, exile it face down, then shuffle. For as long as that card
 * remains exiled, you may play it. Spells you cast from exile have convoke."
 *
 * The convoke grant is scoped to the zone a spell is cast *from* (CR 601.2a): the tutored card
 * cast from exile may convoke, the same card cast from hand may not, and the grant ends when
 * the Broodlord leaves.
 */
class HoardingBroodlordScenarioTest : ScenarioTestBase() {

    private fun nameOf(game: TestGame, id: EntityId): String? =
        game.state.getEntity(id)?.get<CardComponent>()?.name

    /** Cast the Broodlord off eight Swamps and tutor [target] into exile with its ETB. */
    private fun castAndTutor(game: TestGame, target: String): EntityId {
        game.castSpell(1, "Hoarding Broodlord").error shouldBe null
        game.resolveStack()
        val search = game.getPendingDecision()
        search.shouldBeInstanceOf<SelectCardsDecision>()
        val pick = search.options.first { nameOf(game, it) == target }
        game.selectCards(listOf(pick))
        game.resolveStack()
        withClue("the tutored card is exiled") { game.state.getExile(game.player1Id).contains(pick) shouldBe true }
        return pick
    }

    private fun convokeWith(vararg creatures: Pair<EntityId, com.wingedsheep.sdk.core.Color?>) =
        AlternativePaymentChoice(convokedCreatures = creatures.associate { (id, c) -> id to ConvokePayment(color = c) })

    init {
        context("Hoarding Broodlord") {

            test("the tutored card cast from exile can be paid entirely by convoke") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hoarding Broodlord")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = castAndTutor(game, "Grizzly Bears")
                val broodlord = game.findPermanent("Hoarding Broodlord")!!
                val elves = game.findPermanent("Llanowar Elves")!!

                val exileCast = game.getLegalActions(1).firstOrNull {
                    (it.action as? CastSpell)?.cardId == bears && it.sourceZone == "EXILE"
                }
                withClue("the exile cast is offered, affordable only through convoke, and carries convoke") {
                    exileCast shouldNotBe null
                    exileCast!!.isAffordable shouldBe true
                    exileCast.hasConvoke shouldBe true
                }

                // All eight Swamps are tapped: the Broodlord pays {1}, the Elves pay {G}.
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = bears,
                        alternativePayment = convokeWith(broodlord to null, elves to com.wingedsheep.sdk.core.Color.GREEN)
                    )
                )
                withClue("convoke pays for a spell cast from exile: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }

            test("a spell cast from hand gets no convoke from the grant") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hoarding Broodlord")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                val handCast = game.getLegalActions(1).firstOrNull { (it.action as? CastSpell)?.cardId == bears }
                withClue("no lands, no convoke: the hand cast is not affordable") {
                    (handCast == null || !handCast.isAffordable) shouldBe true
                }
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = bears,
                        alternativePayment = convokeWith(
                            game.findPermanent("Hoarding Broodlord")!! to null,
                            game.findPermanent("Llanowar Elves")!! to com.wingedsheep.sdk.core.Color.GREEN
                        )
                    )
                )
                withClue("convoke is rejected for a hand cast") { cast.error shouldNotBe null }
                game.state.getHand(game.player1Id).contains(bears) shouldBe true
            }

            test("the grant ends when the Broodlord leaves; the play permission does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hoarding Broodlord")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = castAndTutor(game, "Grizzly Bears")
                val broodlord = game.findPermanent("Hoarding Broodlord")!!
                game.state = game.zones.moveToZone(game.state, broodlord, Zone.GRAVEYARD).state

                val exileCast = game.getLegalActions(1).firstOrNull {
                    (it.action as? CastSpell)?.cardId == bears && it.sourceZone == "EXILE"
                }
                withClue("still castable from exile, but without convoke") {
                    exileCast shouldNotBe null
                    exileCast!!.hasConvoke shouldBe false
                    exileCast.isAffordable shouldBe false
                }
            }
        }
    }
}
