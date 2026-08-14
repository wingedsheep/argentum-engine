package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Abyssal Harvester (FDN #54).
 *
 * "{1}{B}{B} Creature — Demon Warlock 3/2
 *  {T}: Exile target creature card from a graveyard that was put there this turn. Create a
 *  token that's a copy of it, except it's a Nightmare in addition to its other types. Then
 *  exile all other Nightmare tokens you control."
 *
 * The "put there this turn" half is `StatePredicate.PutIntoGraveyardThisTurn` — see
 * [PutIntoGraveyardThisTurnTest] for the predicate's own rules coverage.
 */
class AbyssalHarvesterScenarioTest : ScenarioTestBase() {

    init {
        val harvesterAbilityId by lazy {
            cardRegistry.getCard("Abyssal Harvester")!!.script.activatedAbilities.single().id
        }

        context("Abyssal Harvester") {

            /**
             * Move a card the builder planted in a graveyard out and back in through
             * [ZoneTransitionService], so it carries a genuine "put into a graveyard this turn"
             * stamp. The builder's `withCardInGraveyard` writes the zone directly and therefore
             * never stamps — which is exactly the distinction the ability cares about.
             */
            fun ScenarioTestBase.TestGame.landInGraveyardThisTurn(cardId: EntityId) {
                state = ZoneTransitionService.moveToZone(state, cardId, Zone.HAND).state
                state = ZoneTransitionService.moveToZone(state, cardId, Zone.GRAVEYARD).state
            }

            test("exiles the targeted card and leaves a Nightmare token copy behind") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Abyssal Harvester")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearCard = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                game.landInGraveyardThisTurn(bearCard)

                val harvester = game.findPermanent("Abyssal Harvester")!!
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = harvester,
                        abilityId = harvesterAbilityId,
                        targets = listOf(
                            ChosenTarget.Card(bearCard, game.player2Id, Zone.GRAVEYARD)
                        )
                    )
                )
                withClue("Activating Abyssal Harvester should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("the targeted card is exiled") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }

                val token = game.findPermanent("Grizzly Bears")
                token shouldNotBe null
                val tokenContainer = game.state.getEntity(token!!)!!
                withClue("the copy is a token") {
                    tokenContainer.has<TokenComponent>() shouldBe true
                }
                val typeLine = tokenContainer.get<CardComponent>()!!.typeLine
                withClue("Nightmare is added in addition to the copied types: $typeLine") {
                    typeLine.subtypes.contains(Subtype.NIGHTMARE) shouldBe true
                    typeLine.subtypes.contains(Subtype("Bear")) shouldBe true
                }
                withClue("the token copies the card's printed P/T (Grizzly Bears is 2/2)") {
                    game.state.projectedState.getPower(token) shouldBe 2
                    game.state.projectedState.getToughness(token) shouldBe 2
                }
                withClue("the token is controlled by the activating player, not the card's owner") {
                    game.state.projectedState.getController(token) shouldBe game.player1Id
                }
            }

            test("a second activation exiles the Nightmare token the first one made") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Abyssal Harvester")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val harvester = game.findPermanent("Abyssal Harvester")!!
                val abilityId = harvesterAbilityId

                val bearCard = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                game.landInGraveyardThisTurn(bearCard)
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = harvester,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bearCard, game.player2Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true

                // Untap the Harvester so it can be activated again this turn.
                game.state = game.state.updateEntity(harvester) { it.without<TappedComponent>() }

                val giantCard = game.findCardsInGraveyard(2, "Hill Giant").single()
                game.landInGraveyardThisTurn(giantCard)
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = harvester,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(giantCard, game.player2Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the new Nightmare token stays") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("the previous Nightmare token is exiled by 'all other Nightmare tokens'") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("a card already sitting in a graveyard is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Abyssal Harvester")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // No landInGraveyardThisTurn call — the card was never *put there* this turn.
                val bearCard = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                val harvester = game.findPermanent("Abyssal Harvester")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = harvester,
                        abilityId = harvesterAbilityId,
                        targets = listOf(ChosenTarget.Card(bearCard, game.player2Id, Zone.GRAVEYARD))
                    )
                )
                withClue("the ability should be rejected for an illegal target") {
                    result.error shouldNotBe null
                }
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("a noncreature card put into a graveyard this turn is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Abyssal Harvester")
                    .withCardInGraveyard(2, "Shock")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shockCard = game.findCardsInGraveyard(2, "Shock").single()
                game.landInGraveyardThisTurn(shockCard)

                val harvester = game.findPermanent("Abyssal Harvester")!!
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = harvester,
                        abilityId = harvesterAbilityId,
                        targets = listOf(ChosenTarget.Card(shockCard, game.player2Id, Zone.GRAVEYARD))
                    )
                )
                withClue("only creature cards qualify") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
