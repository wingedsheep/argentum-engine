package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hama, the Bloodbender ({2}{U/B}{U/B}{U/B}, Legendary Creature — Human Warlock,
 * 3/3):
 *
 * "When Hama enters, target opponent mills three cards. Exile up to one noncreature, nonland card
 * from that player's graveyard. For as long as you control Hama, you may cast the exiled card during
 * your turn by waterbending {X} rather than paying its mana cost, where X is its mana value."
 *
 * Pins:
 *  1. The ETB mills the target opponent three cards, then exiles a chosen noncreature/nonland card
 *     from *their* graveyard — creatures and lands are not eligible.
 *  2. On your turn the exiled card can be cast by waterbending {its mana value}: the whole generic
 *     is paid by tapping artifacts/creatures (each {1}); the spell resolves and the taps are tapped.
 *  3. The grant is gated to your turn and to controlling Hama — the card is not castable on an
 *     opponent's turn, nor after Hama has left the battlefield.
 */
class HamaTheBloodbenderScenarioTest : ScenarioTestBase() {

    init {
        // A noncreature, nonland, no-target spell of mana value 2 — the card Hama steals and lets
        // you recast by waterbending {2}.
        val bait = card("Bloodbend Bait") {
            manaCost = "{1}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "You gain 3 life."
            spell {
                effect = Effects.GainLife(3)
            }
        }
        cardRegistry.register(bait)

        context("Hama, the Bloodbender") {

            test("ETB mills three and exiles a chosen noncreature, nonland card (creatures/lands ineligible)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hama, the Bloodbender")
                    .withLandsOnBattlefield(1, "Island", 5)
                    // Opponent's graveyard: one eligible spell + an ineligible creature and land.
                    .withCardInGraveyard(2, "Bloodbend Bait")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Island")
                    // Three creatures to mill (they land in the graveyard as creatures — ineligible).
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.execute(CastSpell(game.player1Id, hamaId(game)))
                withClue("Casting Hama should succeed: ${cast.error}") { cast.error shouldBe null }

                val decision = driveToExileChoice(game, game.player2Id)

                withClue("target opponent mills three cards (3 seeded + 3 milled = 6 in the graveyard)") {
                    game.graveyardSize(2) shouldBe 6
                }
                withClue("only the noncreature, nonland card is eligible to exile") {
                    optionNames(game, decision) shouldBe setOf("Bloodbend Bait")
                }

                val baitId = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Bloodbend Bait"
                }
                game.selectCards(listOf(baitId))
                game.resolveStack()

                withClue("the chosen card is exiled (into its owner's exile)") {
                    game.state.getExile(game.player2Id).contains(baitId) shouldBe true
                }
                withClue("the ineligible creature and land stay in the graveyard") {
                    namesInGraveyard(game, game.player2Id).contains("Grizzly Bears") shouldBe true
                    namesInGraveyard(game, game.player2Id).contains("Island") shouldBe true
                }
            }

            test("the exiled card can be cast on your turn by waterbending its mana value") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hama, the Bloodbender")
                    .withLandsOnBattlefield(1, "Island", 5)
                    // Two untapped creatures to tap for the waterbend {2} (mana value of Bloodbend Bait).
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Bloodbend Bait")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(CastSpell(game.player1Id, hamaId(game)))
                val decision = driveToExileChoice(game, game.player2Id)
                val baitId = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Bloodbend Bait"
                }
                game.selectCards(listOf(baitId))
                game.resolveStack()

                val exiledBait = game.state.getExile(game.player2Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Bloodbend Bait"
                }
                val tappers = game.findAllPermanents("Grizzly Bears")
                withClue("Player 1 controls two creatures to tap for waterbend {2}") {
                    tappers.size shouldBe 2
                }

                val lifeBefore = game.getLifeTotal(1)
                val castExiled = game.execute(
                    CastSpell(
                        game.player1Id,
                        exiledBait,
                        alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = tappers.toSet()),
                    )
                )
                withClue("waterbending {2} by tapping the two creatures pays the whole cost: ${castExiled.error}") {
                    castExiled.error shouldBe null
                }
                game.resolveStack()

                withClue("the exiled Bloodbend Bait resolves — Player 1 gains 3 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 3
                }
                withClue("the two tapped creatures are tapped") {
                    tappers.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                }
            }

            test("the exiled card is not castable on an opponent's turn or after Hama leaves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hama, the Bloodbender")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInGraveyard(2, "Bloodbend Bait")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(CastSpell(game.player1Id, hamaId(game)))
                val decision = driveToExileChoice(game, game.player2Id)
                val baitId = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Bloodbend Bait"
                }
                game.selectCards(listOf(baitId))
                game.resolveStack()

                withClue("control: on your turn while you control Hama the exiled card is offered") {
                    canCastExiled(game, baitId) shouldBe true
                }

                // On an opponent's turn (IsYourTurn fails) the grant is closed.
                game.state = game.state.copy(activePlayerId = game.player2Id)
                withClue("on an opponent's turn the exiled card is not castable") {
                    canCastExiled(game, baitId) shouldBe false
                }

                // Back on your turn, but Hama has left the battlefield (YouControlSource fails).
                game.state = game.state.copy(activePlayerId = game.player1Id)
                val hama = game.findPermanent("Hama, the Bloodbender")!!
                game.state = game.state
                    .removeFromZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD), hama)
                    .addToZone(ZoneKey(game.player1Id, Zone.GRAVEYARD), hama)
                    .updateEntity(hama) { it.without<ControllerComponent>() }
                withClue("after Hama leaves the battlefield the exiled card is not castable") {
                    canCastExiled(game, baitId) shouldBe false
                }
            }
        }
    }

    private fun hamaId(game: TestGame): EntityId =
        game.state.getHand(game.player1Id).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Hama, the Bloodbender"
        }

    /**
     * Resolve the cast of Hama and its ETB — answering the target choice and any mana payment — until
     * the "exile up to one card" selection decision surfaces, then return it.
     */
    private fun driveToExileChoice(game: TestGame, opponentId: EntityId): SelectCardsDecision {
        var guard = 0
        while (guard++ < 40) {
            when (val decision = game.getPendingDecision()) {
                is SelectCardsDecision -> return decision
                is ChooseTargetsDecision -> game.selectTargets(listOf(opponentId))
                is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                null -> {
                    check(game.state.stack.isNotEmpty()) { "No exile-selection decision surfaced" }
                    game.resolveStack()
                }
                else -> error("Unexpected decision while resolving Hama's ETB: $decision")
            }
        }
        error("Hama ETB resolution did not reach the exile-selection decision")
    }

    private fun canCastExiled(game: TestGame, cardId: EntityId): Boolean =
        game.getLegalActions(1).any {
            it.actionType == "CastSpell" && (it.action as? CastSpell)?.cardId == cardId
        }

    private fun optionNames(game: TestGame, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    private fun namesInGraveyard(game: TestGame, playerId: EntityId): Set<String> =
        game.state.getGraveyard(playerId).mapNotNull {
            game.state.getEntity(it)?.get<CardComponent>()?.name
        }.toSet()
}
