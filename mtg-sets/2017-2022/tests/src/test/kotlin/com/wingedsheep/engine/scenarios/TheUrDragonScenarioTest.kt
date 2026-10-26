package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Ur-Dragon (Commander 2017 #48).
 *
 *   Eminence — As long as The Ur-Dragon is in the command zone or on the battlefield, other Dragon
 *   spells you cast cost {1} less to cast.
 *   Flying
 *   Whenever one or more Dragons you control attack, draw that many cards, then you may put a
 *   permanent card from your hand onto the battlefield.
 *
 * The eminence static's zone rules are pinned down in `EminenceStaticAbilityTest`; this covers the
 * card itself — the discount from the command zone, and the attack trigger's "that many" and its
 * optional put.
 */
class TheUrDragonScenarioTest : ScenarioTestBase() {

    private fun TestGame.offeredCost(name: String): String? =
        getLegalActions(1)
            .firstOrNull { it.actionType == "CastSpell" && it.description == "Cast $name" }
            ?.manaCostString

    init {
        test("eminence: from the command zone, other Dragon spells cost {1} less") {
            val game = scenario()
                .withPlayers()
                .withCardInCommandZone(1, "The Ur-Dragon")
                .withCardInHand(1, "Shivan Dragon")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.offeredCost("Shivan Dragon") shouldBe "{3}{R}{R}"
            withClue("five Mountains pay for a six-mana Dragon") {
                game.castSpell(1, "Shivan Dragon").error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield("Shivan Dragon") shouldBe true
        }

        test("attacking with Dragons draws that many, then puts a chosen permanent card onto the battlefield") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "The Ur-Dragon")
                .withCardOnBattlefield(1, "Dragon Whelp")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("The Ur-Dragon" to 2, "Dragon Whelp" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is SelectCardsDecision && guard < 10) {
                game.resolveStack()
                guard++
            }
            withClue("two Dragons attacked (the Bears are not a Dragon), so two cards were drawn") {
                game.handSize(1) shouldBe 3
            }
            val giant = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
            }
            game.selectCards(listOf(giant)).error shouldBe null

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.handSize(1) shouldBe 2
        }

        test("ruling: an attacking Dragon that leaves before resolution still counts toward the draw") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "The Ur-Dragon")
                .withCardOnBattlefield(1, "Dragon Whelp")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("The Ur-Dragon" to 2, "Dragon Whelp" to 2)).error shouldBe null
            // The trigger is on the stack; Murder the attacking Whelp in response.
            game.castSpell(1, "Murder", game.findPermanent("Dragon Whelp")!!).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is SelectCardsDecision && guard < 10) {
                game.resolveStack()
                guard++
            }
            withClue("the Whelp is gone") { game.isOnBattlefield("Dragon Whelp") shouldBe false }
            withClue("both declared Dragons still count: two cards drawn") { game.handSize(1) shouldBe 2 }
            game.skipSelection().error shouldBe null
        }

        test("the put is optional") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "The Ur-Dragon")
                .withCardInHand(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("The Ur-Dragon" to 2)).error shouldBe null

            var guard = 0
            while (game.state.pendingDecision !is SelectCardsDecision && guard < 10) {
                game.resolveStack()
                guard++
            }
            game.handSize(1) shouldBe 2
            game.skipSelection().error shouldBe null

            game.isOnBattlefield("Hill Giant") shouldBe false
            game.handSize(1) shouldBe 2
        }

        test("attacking with only non-Dragons doesn't trigger") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "The Ur-Dragon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 0
            (game.state.pendingDecision is SelectCardsDecision) shouldBe false
        }
    }
}
