package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Coveted Prize (ZNR #95, {4}{B}, Sorcery).
 *
 *   This spell costs {1} less to cast for each creature in your party.
 *   Search your library for a card, put it into your hand, then shuffle. If you have a full party,
 *   you may cast a spell with mana value 4 or less from your hand without paying its mana cost.
 *
 * Pins the resolution order: the full-party check comes after the search, so the tutored card can be
 * the one cast for free; only nonland cards of mana value 4 or less are offered; and without a full
 * party there is no free cast at all.
 */
class CovetedPrizeScenarioTest : ScenarioTestBase() {

    private fun member(name: String, type: String) = card(name) {
        manaCost = "{1}"
        typeLine = "Creature — Human $type"
        power = 1
        toughness = 1
    }

    private fun board(members: List<String>, swamps: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Coveted Prize")
        .withCardInHand(1, "Force of Nature")
        .withCardInHand(1, "Swamp")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Mountain")
        .withLandsOnBattlefield(1, "Swamp", swamps)
        .also { b -> members.forEach { b.withCardOnBattlefield(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private val fullParty = listOf("Prize Test Cleric", "Prize Test Rogue", "Prize Test Warrior", "Prize Test Wizard")

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    init {
        cardRegistry.register(member("Prize Test Cleric", "Cleric"))
        cardRegistry.register(member("Prize Test Rogue", "Rogue"))
        cardRegistry.register(member("Prize Test Warrior", "Warrior"))
        cardRegistry.register(member("Prize Test Wizard", "Wizard"))

        test("with a full party it costs {B} and can free-cast the card it just found") {
            val game = board(fullParty, swamps = 1)
            withClue("four party members take {4} off — one Swamp pays for it") {
                game.castSpell(1, "Coveted Prize").error shouldBe null
            }
            game.resolveStack()

            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(search.options.filter { game.nameOf(it) == "Grizzly Bears" })

            val cast = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only nonland cards with mana value 4 or less — the tutored Bears, not Force of Nature or Swamp") {
                cast.options.map { game.nameOf(it) } shouldBe listOf("Grizzly Bears")
            }
            game.selectCards(cast.options)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
            game.isInGraveyard(1, "Coveted Prize").shouldBeTrue()
        }

        test("declining the free cast keeps the card in hand") {
            val game = board(fullParty, swamps = 1)
            game.castSpell(1, "Coveted Prize").error shouldBe null
            game.resolveStack()

            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(search.options.filter { game.nameOf(it) == "Grizzly Bears" })
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList())
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears").shouldBeFalse()
            game.isInHand(1, "Grizzly Bears").shouldBeTrue()
        }

        test("without a full party it only tutors") {
            // Three party members: {1} off, so {1}{B} needs two Swamps.
            val game = board(fullParty.take(3), swamps = 2)
            game.castSpell(1, "Coveted Prize").error shouldBe null
            game.resolveStack()

            val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(search.options.filter { game.nameOf(it) == "Grizzly Bears" })

            game.hasPendingDecision().shouldBeFalse()
            game.isInHand(1, "Grizzly Bears").shouldBeTrue()
            game.isInGraveyard(1, "Coveted Prize").shouldBeTrue()
        }
    }
}
