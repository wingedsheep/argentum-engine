package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.ConduitOfWorlds
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Conduit of Worlds (ONE #163) — {2}{G}{G} Artifact.
 *
 *   You may play lands from your graveyard.
 *   {T}: Choose target nonland permanent card in your graveyard. If you haven't cast a spell this
 *   turn, you may cast that card. If you do, you can't cast additional spells this turn. Activate
 *   only as a sorcery.
 *
 * Pins the paid cast during resolution and the follow-up spell lock, the "haven't cast a spell
 * this turn" gate read at resolution, and the graveyard land play.
 */
class ConduitOfWorldsScenarioTest : ScenarioTestBase() {

    private val abilityId = ConduitOfWorlds.activatedAbilities.single().id

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Conduit of Worlds")
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Forest")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInHand(1, "Lightning Bolt")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateOn(cardName: String) {
        val conduit = findPermanent("Conduit of Worlds")!!
        val card = findCardsInGraveyard(1, cardName).first()
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = conduit,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(card, player1Id, Zone.GRAVEYARD)),
            )
        ).error shouldBe null
    }

    init {
        test("casts the target card paying its cost, then locks out further spells this turn") {
            val game = board()
            game.activateOn("Grizzly Bears")
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears was cast from the graveyard and resolved") {
                game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            }
            withClue("Its mana cost was paid") {
                game.state.getBattlefield().count { game.state.getEntity(it)?.get<TappedComponent>() != null } shouldBe 3
            }
            withClue("You can't cast additional spells this turn") {
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error.shouldNotBeNull()
            }
        }

        test("declining leaves the card in the graveyard and spells castable") {
            val game = board()
            game.activateOn("Grizzly Bears")
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error.shouldBeNull()
        }

        test("does nothing if you already cast a spell this turn") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error.shouldBeNull()
            game.resolveStack()

            game.activateOn("Grizzly Bears")
            game.resolveStack()

            withClue("No cast prompt is offered") {
                game.hasPendingDecision() shouldBe false
            }
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("you may play lands from your graveyard") {
            val game = board()
            val forest = game.findCardsInGraveyard(1, "Forest").first()
            game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
            withClue("The Forest left the graveyard for the battlefield") {
                game.isInGraveyard(1, "Forest") shouldBe false
                game.state.getBattlefield().count {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                } shouldBe 3
            }
        }
    }
}
