package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * Secrets of Strixhaven — the three buildable cards (Ark of Hunger, Professor Dellian Fel,
 * Great Hall of the Biblioplex). Each exercises the card's mechanics against the real
 * ActionProcessor.
 */
class SosBuildableCardsScenarioTest : ScenarioTestBase() {

    /** Seed a planeswalker's starting loyalty (withCardOnBattlefield doesn't stamp it). */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    init {
        // ---------------------------------------------------------------------
        // Ark of Hunger
        // ---------------------------------------------------------------------

        test("Ark of Hunger {T} mills a card and grants permission to play it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ark of Hunger")
                .withCardInLibrary(1, "Mountain")
                .build()

            val ark = game.findPermanent("Ark of Hunger")!!
            val millAbility = cardRegistry.requireCard("Ark of Hunger").script.activatedAbilities[0]
            val gyBefore = game.graveyardSize(1)

            game.execute(ActivateAbility(game.player1Id, ark, millAbility.id))
            game.resolveStack()

            // The milled card is now in the graveyard with a may-play permission.
            game.graveyardSize(1) shouldBe gyBefore + 1
            val milled = game.state.getGraveyard(game.player1Id).last()
            game.state.mayPlayPermissions.any { milled in it.cardIds }.shouldBeTrue()
        }

        test("Ark of Hunger triggers when a card leaves your graveyard (1 dmg to opp, gain 1)") {
            // Cast Raise Dead returning a creature from the graveyard to hand: the creature
            // leaves the graveyard, firing Ark's batched leave-graveyard trigger — 1 damage to
            // each opponent and 1 life to the controller.
            var builder = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ark of Hunger")
                .withCardInHand(1, "Raise Dead")
                .withCardInGraveyard(1, "Glory Seeker")
                .withLandsOnBattlefield(1, "Swamp", 1)
            repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
            repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()

            val lifeBefore = game.getLifeTotal(1)
            val oppLifeBefore = game.getLifeTotal(2)

            val glorySeeker = game.findCardsInGraveyard(1, "Glory Seeker").first()
            val castResult = game.execute(
                com.wingedsheep.engine.core.CastSpell(
                    game.player1Id,
                    game.findCardsInHand(1, "Raise Dead").first(),
                    listOf(ChosenTarget.Card(glorySeeker, game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD))
                )
            )
            castResult.error shouldBe null
            game.resolveStack()

            // Glory Seeker left the graveyard → Ark trigger fires.
            game.getLifeTotal(1) shouldBe lifeBefore + 1
            game.getLifeTotal(2) shouldBe oppLifeBefore - 1
        }

        // ---------------------------------------------------------------------
        // Professor Dellian Fel
        // ---------------------------------------------------------------------

        test("Professor Dellian Fel +2: gain 3 life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Professor Dellian Fel")
                .build()

            val pw = game.findPermanent("Professor Dellian Fel")!!
            seedLoyalty(game, pw, 5)
            val plusTwo = cardRegistry.requireCard("Professor Dellian Fel").script.activatedAbilities[0]
            val lifeBefore = game.getLifeTotal(1)

            game.execute(ActivateAbility(game.player1Id, pw, plusTwo.id))
            game.resolveStack()

            game.getLifeTotal(1) shouldBe lifeBefore + 3
        }

        test("Professor Dellian Fel -3: destroys target creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Professor Dellian Fel")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            val pw = game.findPermanent("Professor Dellian Fel")!!
            seedLoyalty(game, pw, 5)
            val bear = game.findPermanent("Grizzly Bears")!!
            val destroy = cardRegistry.requireCard("Professor Dellian Fel")
                .script.activatedAbilities.first { it.description.contains("Destroy", ignoreCase = true) }

            game.execute(
                ActivateAbility(
                    game.player1Id, pw, destroy.id,
                    targets = listOf(ChosenTarget.Permanent(bear))
                )
            )
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        // ---------------------------------------------------------------------
        // Great Hall of the Biblioplex
        // ---------------------------------------------------------------------

        test("Great Hall {5} animates the land into a 2/4 Wizard creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Great Hall of the Biblioplex")
                .build()

            val hall = game.findPermanent("Great Hall of the Biblioplex")!!
            // The {5} ability is the third activated ability (index 2).
            val animate = cardRegistry.requireCard("Great Hall of the Biblioplex")
                .script.activatedAbilities[2]

            // Pay {5}: give colorless mana then activate.
            game.state = game.state.updateEntity(game.player1Id) { c ->
                c.with(ManaPoolComponent(colorless = 5))
            }
            val result = game.execute(ActivateAbility(game.player1Id, hall, animate.id))
            result.error shouldBe null
            game.resolveStack()

            // The land is now a creature (verified via projected state).
            val projected = StateProjector().project(game.state)
            projected.isCreature(hall).shouldBeTrue()
        }
    }
}
