package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kaya, Spirits' Justice (MKM #211, {2}{W}{B} planeswalker, loyalty 3).
 *
 *   Whenever one or more creatures you control and/or creature cards in your graveyard are put
 *   into exile, you may choose a creature card from among them. Until end of turn, target token
 *   you control becomes a copy of it, except it has flying.
 *   +2: Surveil 2, then exile a card from a graveyard.
 *   +1: Create a 1/1 white and black Spirit creature token with flying.
 *   −2: Exile target creature you control. For each other player, exile up to one target creature
 *   that player controls.
 *
 * The two engine capabilities this card needed both show up here: the exile batch trigger scoped
 * to one player's creatures and graveyard (and capturing that batch as "them"), and the
 * one-target-per-other-player shape on the −2.
 *
 * Kaya is also the first card that is a batch trigger *and* targets, so the copy tests below are
 * the regression for `TriggeredAbilityContinuation.capturedEntityIds`: the captured batch used to
 * be dropped while the ability paused to choose its token target, and "them" resolved empty.
 */
class KayaSpiritsJusticeScenarioTest : ScenarioTestBase() {

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Seed a planeswalker's starting loyalty — withCardOnBattlefield doesn't stamp it. */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun abilityId(index: Int) =
        cardRegistry.getCard("Kaya, Spirits' Justice")!!.script.activatedAbilities[index].id

    init {
        test("+1: create a 1/1 white and black Spirit token with flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kaya, Spirits' Justice")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kaya = game.findPermanent("Kaya, Spirits' Justice")!!
            seedLoyalty(game, kaya, 3)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = kaya, abilityId = abilityId(1))
            ).error shouldBe null
            game.resolveStack()

            withClue("+1 leaves Kaya at 4 loyalty") { loyalty(game, kaya) shouldBe 4 }

            val spirit = game.findPermanents("Spirit").single {
                game.state.getEntity(it)?.has<TokenComponent>() == true
            }
            game.state.projectedState.getPower(spirit) shouldBe 1
            game.state.projectedState.getToughness(spirit) shouldBe 1
            withClue("the Spirit flies") {
                game.state.projectedState.getKeywords(spirit) shouldContain Keyword.FLYING.name
            }
        }

        test("-2 exiles your creature and one the opponent controls, and the trigger copies it onto your token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kaya, Spirits' Justice")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Clue", isToken = true)
                .withCardOnBattlefield(2, "Runeclaw Bear")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kaya = game.findPermanent("Kaya, Spirits' Justice")!!
            seedLoyalty(game, kaya, 3)
            val myBears = game.findPermanent("Grizzly Bears")!!
            val theirBear = game.findPermanent("Runeclaw Bear")!!
            val token = game.findPermanents("Clue").single {
                game.state.getEntity(it)?.has<TokenComponent>() == true
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = kaya,
                    abilityId = abilityId(2),
                    targets = listOf(ChosenTarget.Permanent(myBears), ChosenTarget.Permanent(theirBear)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("-2 leaves Kaya at 1 loyalty") { loyalty(game, kaya) shouldBe 1 }
            withClue("both creatures were exiled") {
                game.state.getZone(game.player1Id, Zone.EXILE) shouldContain myBears
                game.state.getZone(game.player2Id, Zone.EXILE) shouldContain theirBear
            }

            // The exile of a creature you control fires Kaya's trigger. Its token target is chosen
            // as it goes on the stack (CR 603.3d); the "you may choose a creature card from among
            // them" pick follows on resolution.
            game.selectTargets(listOf(token))
            game.resolveStack()
            game.selectCards(listOf(myBears))
            game.resolveStack()

            withClue("the token became a copy of the exiled creature you controlled") {
                game.state.getEntity(token)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
                game.state.projectedState.getPower(token) shouldBe 2
                game.state.projectedState.getToughness(token) shouldBe 2
            }
            withClue("\"except it has flying\" — Grizzly Bears has no flying of its own") {
                game.state.projectedState.getKeywords(token) shouldContain Keyword.FLYING.name
            }
        }

        test("the opponent's exiled creature is not choosable — \"creatures you control\" scopes the batch") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kaya, Spirits' Justice")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Clue", isToken = true)
                .withCardOnBattlefield(2, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kaya = game.findPermanent("Kaya, Spirits' Justice")!!
            seedLoyalty(game, kaya, 3)
            val myBears = game.findPermanent("Grizzly Bears")!!
            val theirAngel = game.findPermanent("Serra Angel")!!
            val token = game.findPermanents("Clue").single {
                game.state.getEntity(it)?.has<TokenComponent>() == true
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = kaya,
                    abilityId = abilityId(2),
                    targets = listOf(ChosenTarget.Permanent(myBears), ChosenTarget.Permanent(theirAngel)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(token))
            game.resolveStack()

            // Only Grizzly Bears is on offer; picking it is the only legal non-empty choice.
            game.selectCards(listOf(myBears))
            game.resolveStack()

            withClue("the 4/4 flier the opponent controlled was never part of \"them\"") {
                game.state.getEntity(token)?.get<CardComponent>()?.name shouldNotBe "Serra Angel"
                game.state.projectedState.getPower(token) shouldBe 2
            }
        }

        test("declining the optional choice leaves the token alone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kaya, Spirits' Justice")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Clue", isToken = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kaya = game.findPermanent("Kaya, Spirits' Justice")!!
            seedLoyalty(game, kaya, 3)
            val myBears = game.findPermanent("Grizzly Bears")!!
            val token = game.findPermanents("Clue").single {
                game.state.getEntity(it)?.has<TokenComponent>() == true
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = kaya,
                    abilityId = abilityId(2),
                    targets = listOf(ChosenTarget.Permanent(myBears)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(token))
            game.resolveStack()
            game.selectCards(emptyList())
            game.resolveStack()

            withClue("\"you may choose\" — declining is a no-op, the Clue stays a Clue") {
                game.state.getEntity(token)?.get<CardComponent>()?.name shouldBe "Clue"
            }
        }

        test("+2: surveil 2, then exile a card from a graveyard — a creature card from YOUR yard fires the trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Kaya, Spirits' Justice")
                .withCardOnBattlefield(1, "Clue", isToken = true)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kaya = game.findPermanent("Kaya, Spirits' Justice")!!
            seedLoyalty(game, kaya, 3)
            val token = game.findPermanents("Clue").single {
                game.state.getEntity(it)?.has<TokenComponent>() == true
            }
            val bearsInYard = game.findCardsInGraveyard(1, "Grizzly Bears").first()

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = kaya, abilityId = abilityId(0))
            ).error shouldBe null
            game.resolveStack()

            withClue("+2 leaves Kaya at 5 loyalty") { loyalty(game, kaya) shouldBe 5 }

            // Surveil 2: bin nothing, keep the two cards in order. Then exile the creature card
            // from your graveyard.
            game.selectCards(emptyList())
            game.keepLibraryOrder()
            game.selectCards(listOf(bearsInYard))
            game.resolveStack()

            withClue("the graveyard card is now in exile") {
                game.state.getZone(game.player1Id, Zone.EXILE) shouldContain bearsInYard
            }

            // Exiling a creature card from your graveyard is the other arm of the and/or.
            game.selectTargets(listOf(token))
            game.resolveStack()
            game.selectCards(listOf(bearsInYard))
            game.resolveStack()

            withClue("the token copied the creature card exiled out of your own graveyard") {
                game.state.getEntity(token)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
                game.state.projectedState.getKeywords(token) shouldContain Keyword.FLYING.name
            }
        }
    }
}
