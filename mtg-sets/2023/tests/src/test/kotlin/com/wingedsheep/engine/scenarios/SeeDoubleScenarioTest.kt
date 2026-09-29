package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * See Double — {2}{U}{U} Instant
 *
 * "This spell can't be copied.
 *  Choose one. If an opponent has eight or more cards in their graveyard, you may choose both instead.
 *  • Copy target spell. You may choose new targets for the copy.
 *  • Create a token that's a copy of target creature."
 *
 * Modes: 0 = copy target spell, 1 = token copy of target creature.
 */
class SeeDoubleScenarioTest : ScenarioTestBase() {

    init {
        fun builder(opponentGraveyard: Int = 0, ownGraveyard: Int = 0): TestGame {
            var b = scenario()
                .withPlayers()
                .withCardInHand(1, "See Double")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(opponentGraveyard) { b = b.withCardInGraveyard(2, "Grizzly Bears") }
            repeat(ownGraveyard) { b = b.withCardInGraveyard(1, "Grizzly Bears") }
            return b.build()
        }

        fun TestGame.stackSpell(name: String): EntityId = state.stack.first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

        fun TestGame.castBothModes(bolt: EntityId, creature: EntityId) = execute(
            CastSpell(
                player1Id,
                findCardsInHand(1, "See Double").first(),
                listOf(ChosenTarget.Spell(bolt), ChosenTarget.Permanent(creature)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Spell(bolt)),
                    listOf(ChosenTarget.Permanent(creature))
                )
            )
        )

        test("mode 0 copies target spell and the copy may take a new target") {
            val game = builder()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            val bolt = game.stackSpell("Lightning Bolt")
            game.execute(
                CastSpell(
                    game.player1Id,
                    game.findCardsInHand(1, "See Double").first(),
                    listOf(ChosenTarget.Spell(bolt)),
                    chosenModes = listOf(0),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(bolt)))
                )
            ).error shouldBe null

            game.resolveStack()
            game.hasPendingDecision().shouldBeTrue()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.findPermanent("Hill Giant") shouldBe null
        }

        test("mode 1 creates a token copy of target creature") {
            val game = builder()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpellWithMode(1, "See Double", 1, giant).error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Hill Giant").size shouldBe 2
            game.isInGraveyard(1, "See Double").shouldBeTrue()
        }

        test("choosing both modes is illegal when no opponent has eight graveyard cards") {
            // Seven in the opponent's graveyard, eight in the caster's own: neither qualifies.
            val game = builder(opponentGraveyard = 7, ownGraveyard = 8)
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            val bolt = game.stackSpell("Lightning Bolt")
            game.castBothModes(bolt, giant).error.shouldNotBeNull()
        }

        test("with an opponent at eight graveyard cards, both modes may be chosen") {
            val game = builder(opponentGraveyard = 8)
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            val bolt = game.stackSpell("Lightning Bolt")
            game.castBothModes(bolt, giant).error shouldBe null

            // See Double is uncopiable (CR 707.10).
            game.state.getEntity(game.stackSpell("See Double"))!!
                .has<CantBeCopiedComponent>().shouldBeTrue()

            var guard = 0
            while (game.state.stack.isNotEmpty() && guard++ < 10) {
                game.resolveStack()
                // Keep the Bolt copy on the original target (the Bears).
                if (game.hasPendingDecision()) game.selectTargets(listOf(bears)).error shouldBe null
            }

            game.findPermanent("Grizzly Bears") shouldBe null
            game.findAllPermanents("Hill Giant").size shouldBe 2
        }
    }
}
