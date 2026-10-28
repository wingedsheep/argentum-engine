package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tajuru Paragon (ZNR #209) — "Tajuru Paragon is also a Cleric, Rogue, Warrior, and Wizard.
 * Kicker {3}. When this creature enters, if it was kicked, reveal the top six cards of your library.
 * You may put a card that shares a creature type with it from among them into your hand. Put the
 * rest on the bottom of your library in a random order."
 *
 * The type clause is a characteristic-defining ability (CR 604.3), so the Paragon is every one of
 * those types in hand and in the graveyard as well as on the battlefield.
 */
class TajuruParagonScenarioTest : ScenarioTestBase() {

    private val partyTypes = listOf("Elf", "Cleric", "Rogue", "Warrior", "Wizard")

    private fun TestGame.printedSubtypes(id: EntityId): Set<String> =
        state.getEntity(id)!!.get<CardComponent>()!!.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }

    private fun board(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Tajuru Paragon")
        .withLandsOnBattlefield(1, "Forest", 5)
        // Top six, in order: only Llanowar Elves (Elf) and Prodigal Sorcerer (Wizard) share a type.
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Llanowar Elves")
        .withCardInLibrary(1, "Prodigal Sorcerer")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cast(kicked: Boolean) {
        val paragon = findCardsInHand(1, "Tajuru Paragon").single()
        execute(CastSpell(
            player1Id, paragon,
            declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
            paymentStrategy = PaymentStrategy.AutoPay
        )).error shouldBe null
        resolveStack().forEach { it.error shouldBe null }
    }

    init {
        test("is a Cleric, Rogue, Warrior and Wizard in hand, on the battlefield and in the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Tajuru Paragon")
                .withCardInGraveyard(1, "Tajuru Paragon")
                .withCardOnBattlefield(1, "Tajuru Paragon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val inHand = game.findCardsInHand(1, "Tajuru Paragon").single()
            val inGraveyard = game.findCardsInGraveyard(1, "Tajuru Paragon").single()
            val onBattlefield = game.findPermanent("Tajuru Paragon")!!

            withClue("hand") { game.printedSubtypes(inHand) shouldContainAll partyTypes }
            withClue("graveyard") { game.printedSubtypes(inGraveyard) shouldContainAll partyTypes }
            withClue("battlefield (projected)") {
                game.state.projectedState.getSubtypes(onBattlefield) shouldContainAll partyTypes
            }
        }

        test("fills a party slot: Archpriest of Iona with a Paragon counts a party of two") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Archpriest of Iona")
                .withCardOnBattlefield(1, "Tajuru Paragon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val archpriest = game.findPermanent("Archpriest of Iona")!!
            withClue("Cleric Archpriest + Paragon as a Rogue/Warrior/Wizard") {
                game.state.projectedState.getPower(archpriest) shouldBe 2
            }
        }

        test("unkicked: no trigger, the library is untouched") {
            val game = board()
            game.cast(kicked = false)

            game.state.pendingDecision shouldBe null
            (game.findPermanent("Tajuru Paragon") != null) shouldBe true
            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 7
        }

        test("kicked: put a card sharing a creature type into hand, the rest go to the bottom") {
            val game = board()
            val elves = game.findCardsInLibrary(1, "Llanowar Elves").single()
            val sorcerer = game.findCardsInLibrary(1, "Prodigal Sorcerer").single()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            game.cast(kicked = true)

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only cards sharing a type with the Paragon are selectable") {
                decision.options.toSet() shouldBe setOf(elves, sorcerer)
            }
            withClue("Grizzly Bears shares no type") {
                (game.selectCards(listOf(bears)).error != null) shouldBe true
            }
            game.selectCards(listOf(sorcerer)).error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }

            game.isInHand(1, "Prodigal Sorcerer") shouldBe true
            game.handSize(1) shouldBe 1
            withClue("the Plains below the six stays on top; the other five go under it") {
                game.librarySize(1) shouldBe 6
                game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Plains").single()
            }
        }

        test("kicked: may decline and put all six on the bottom") {
            val game = board()
            game.cast(kicked = true)

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack().forEach { it.error shouldBe null }

            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 7
            game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Plains").single()
        }
    }
}
