package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.ValkiGodOfLies
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Valki, God of Lies // Tibalt, Cosmic Impostor (KHM #114).
 *
 * Valki: the enter trigger exiles one creature card from each opponent's revealed hand until Valki
 * leaves — a duration that survives Valki becoming a copy and losing its printed abilities — and
 * `{X}` turns Valki into a copy of the exiled creature card with mana value X, or does nothing.
 *
 * Tibalt: the emblem he gives as he enters lets you play the cards *that* Tibalt exiled, spending
 * mana as though it were any colour, and keeps doing so after he leaves the battlefield.
 */
class ValkiGodOfLiesScenarioTest : ScenarioTestBase() {

    private val valki = "Valki, God of Lies"
    private val tibalt = "Tibalt, Cosmic Impostor"
    private val copyAbility = ValkiGodOfLies.activatedAbilities[0].id
    private val plusTwo = ValkiGodOfLies.backFace!!.activatedAbilities[0].id
    private val minusThree = ValkiGodOfLies.backFace!!.activatedAbilities[1].id
    private val minusEight = ValkiGodOfLies.backFace!!.activatedAbilities[2].id

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.playableFromExile(cardId: EntityId): Boolean =
        getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId }

    private fun TestGame.emblems(): List<Pair<EntityId, EmblemSourceComponent>> =
        state.entities.mapNotNull { (id, c) -> c.get<EmblemSourceComponent>()?.let { id to it } }

    /** Cast Valki from hand and exile the opponent's Grizzly Bears with its enter trigger. */
    private fun valkiExilingBears(): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, valki)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInHand(2, "Grizzly Bears")
            .withCardInHand(2, "Hill Giant")
            .withCardInHand(2, "Shock")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val bears = game.findCardsInHand(2, "Grizzly Bears").first()

        game.castSpell(1, valki).error shouldBe null
        game.resolveStack()
        val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        withClue("Valki's controller makes the choice") { decision.playerId shouldBe game.player1Id }
        withClue("only the creature cards of the revealed hand are choosable") {
            decision.options.toSet() shouldBe game.findCardsInHand(2, "Grizzly Bears").toSet() +
                game.findCardsInHand(2, "Hill Giant")
        }
        game.selectCards(listOf(bears)).error shouldBe null
        game.resolveStack()
        game.isInExile(2, "Grizzly Bears") shouldBe true
        return game to bears
    }

    init {
        test("Valki's enter trigger exiles a creature card from the opponent's hand until Valki leaves") {
            val (game, _) = valkiExilingBears()
            val valkiId = game.findPermanent(valki).shouldNotBeNull()

            game.castSpell(1, "Lightning Bolt", valkiId).error shouldBe null
            game.resolveStack()

            withClue("Valki left, so the Bears return to their owner's hand") {
                game.isInExile(2, "Grizzly Bears") shouldBe false
                game.isInHand(2, "Grizzly Bears") shouldBe true
            }
        }

        test("{X}: Valki becomes a copy of the exiled creature card with mana value X; the exile still ends when it leaves") {
            val (game, bears) = valkiExilingBears()
            val valkiId = game.findPermanent(valki).shouldNotBeNull()

            game.execute(ActivateAbility(game.player1Id, valkiId, copyAbility, xValue = 2)).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Valki is now a Grizzly Bears") {
                game.nameOf(valkiId) shouldBe "Grizzly Bears"
                game.state.projectedState.getPower(valkiId) shouldBe 2
                game.state.projectedState.getToughness(valkiId) shouldBe 2
            }

            game.castSpell(1, "Lightning Bolt", valkiId).error shouldBe null
            game.resolveStack()

            withClue("the copy lost Valki's text, but the exile's duration still ends: the Bears return") {
                game.isInHand(2, "Grizzly Bears") shouldBe true
            }
        }

        test("{X} with no exiled creature card of mana value X does nothing") {
            val (game, _) = valkiExilingBears()
            val valkiId = game.findPermanent(valki).shouldNotBeNull()

            game.execute(ActivateAbility(game.player1Id, valkiId, copyAbility, xValue = 3)).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.nameOf(valkiId) shouldBe valki
        }

        test("Tibalt's emblem: play cards that Tibalt exiled, with mana of any colour, even after he dies") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, valki)
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Swamp", 8)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cardId = game.findCardsInHand(1, valki).first()

            game.execute(
                CastSpell(
                    game.player1Id,
                    cardId,
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.MODAL_BACK_FACE,
                )
            ).error shouldBe null
            game.resolveStack()
            val tibaltId = game.findPermanent(tibalt).shouldNotBeNull()

            val (_, emblem) = game.emblems().single()
            withClue("the emblem is linked to the Tibalt that created it") {
                emblem.creatorId shouldBe tibaltId
                emblem.creatorEntryTimestamp shouldNotBe null
            }

            // −3: exile the opponent's Grizzly Bears into Tibalt's pile.
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.execute(
                ActivateAbility(game.player1Id, tibaltId, minusThree, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Grizzly Bears") shouldBe true

            // Tibalt (2 loyalty) dies to Lightning Bolt.
            game.castSpell(1, "Lightning Bolt", tibaltId).error shouldBe null
            game.resolveStack()
            game.findPermanent(tibalt).shouldBeNull()

            withClue("the opponent's Grizzly Bears is castable from exile, {G} paid with Swamps") {
                game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
                game.resolveStack()
                game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.state.projectedState.getController(game.findPermanent("Grizzly Bears")!!) shouldBe game.player1Id
            }
        }

        test("Tibalt +2 exiles the top card of each library into his pile; a card exiled otherwise is not playable") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, valki)
                .withLandsOnBattlefield(1, "Swamp", 10)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInExile(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cardId = game.findCardsInHand(1, valki).first()
            game.execute(
                CastSpell(game.player1Id, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MODAL_BACK_FACE)
            ).error shouldBe null
            game.resolveStack()
            val tibaltId = game.findPermanent(tibalt).shouldNotBeNull()

            game.execute(ActivateAbility(game.player1Id, tibaltId, plusTwo)).error shouldBe null
            game.resolveStack()

            val giant = game.state.getExile(game.player2Id).first { game.nameOf(it) == "Hill Giant" }
            val mountain = game.state.getExile(game.player1Id).first { game.nameOf(it) == "Mountain" }
            val otherBears = game.state.getExile(game.player2Id).first { game.nameOf(it) == "Grizzly Bears" }
            game.state.getEntity(tibaltId)!!.get<LinkedExileComponent>()!!.exiledIds.toSet() shouldBe setOf(giant, mountain)

            withClue("Bears exiled by something else is not covered by the emblem") {
                game.execute(CastSpell(game.player1Id, otherBears)).error shouldNotBe null
            }
            withClue("the opponent's Hill Giant ({3}{R}) is castable from exile with Swamps") {
                game.execute(CastSpell(game.player1Id, giant)).error shouldBe null
                game.resolveStack()
                game.findPermanent("Hill Giant").shouldNotBeNull()
            }
        }

        test("Tibalt −8 exiles all graveyards into his pile and adds {R}{R}{R}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, valki)
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cardId = game.findCardsInHand(1, valki).first()
            game.execute(
                CastSpell(game.player1Id, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MODAL_BACK_FACE)
            ).error shouldBe null
            game.resolveStack()
            val tibaltId = game.findPermanent(tibalt).shouldNotBeNull()
            game.state = game.state.updateEntity(tibaltId) { c ->
                c.with(CountersComponent(mapOf(CounterType.LOYALTY to 8)))
            }

            game.execute(ActivateAbility(game.player1Id, tibaltId, minusEight)).error shouldBe null
            game.resolveStack()

            game.graveyardSize(1) shouldBe 0
            game.graveyardSize(2) shouldBe 0
            withClue("the −8 cost Tibalt his last loyalty, so he died before it resolved") {
                game.findPermanent(tibalt).shouldBeNull()
            }
            withClue("{R}{R}{R} added") {
                game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 3
            }
            game.state.getExile(game.player2Id).single { game.nameOf(it) == "Hill Giant" }
            withClue("the cards it exiled are still 'exiled with Tibalt': Grizzly Bears ({1}{G}) is cast with the {R}{R}{R}") {
                val bears = game.state.getExile(game.player1Id).first { game.nameOf(it) == "Grizzly Bears" }
                game.playableFromExile(bears) shouldBe true
                game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
                game.resolveStack()
                game.findPermanent("Grizzly Bears").shouldNotBeNull()
            }
        }
    }
}
