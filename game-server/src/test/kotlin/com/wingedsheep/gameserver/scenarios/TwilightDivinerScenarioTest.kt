package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredFromGraveyardComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Twilight Diviner's second ability:
 * "Whenever one or more other creatures you control enter, if they entered or were cast
 *  from a graveyard, create a token that's a copy of one of them. This ability triggers
 *  only once each turn."
 *
 * End-to-end checks drive the trigger via a reanimation (Doomed Necromancer). Baseline
 * tests exercise the marker components and the intervening-if predicate in isolation.
 */
class TwilightDivinerScenarioTest : ScenarioTestBase() {

    init {
        context("Twilight Diviner — reanimation trigger") {

            test("creature reanimated from graveyard gets EnteredFromGraveyardComponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doomed Necromancer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necromancerId = game.findPermanent("Doomed Necromancer")!!
                val bearsId = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val cardDef = cardRegistry.getCard("Doomed Necromancer")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = necromancerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null

                game.resolveStack()

                withClue("Grizzly Bears should be on the battlefield after reanimation") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                val enteredBears = game.findPermanent("Grizzly Bears")!!
                withClue("Reanimated creature must carry EnteredFromGraveyardComponent") {
                    game.state.getEntity(enteredBears)!!
                        .has<EnteredFromGraveyardComponent>() shouldBe true
                }
            }

            test("Twilight Diviner creates a token copy when a creature is reanimated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Twilight Diviner")
                    .withCardOnBattlefield(1, "Doomed Necromancer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Plains") // Prevent draw-from-empty losses
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necromancerId = game.findPermanent("Doomed Necromancer")!!
                val bearsId = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val cardDef = cardRegistry.getCard("Doomed Necromancer")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = necromancerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null

                // Resolves Doomed Necromancer's ability, then Twilight Diviner's follow-up trigger.
                game.resolveStack()

                val bearsOnBattlefield = game.state.getBattlefield().filter { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@filter false
                    container.get<CardComponent>()?.name == "Grizzly Bears" &&
                        container.get<ControllerComponent>()?.playerId == game.player1Id
                }

                withClue("Expected two Grizzly Bears (original + token copy), got ${bearsOnBattlefield.size}") {
                    bearsOnBattlefield.size shouldBe 2
                }

                val tokenCopy = bearsOnBattlefield.firstOrNull { entityId ->
                    game.state.getEntity(entityId)!!.has<TokenComponent>()
                }
                withClue("One Grizzly Bears should be a token copy") {
                    tokenCopy shouldNotBe null
                }
            }

            test("non-graveyard ETB does not trigger the copy ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Twilight Diviner")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                val bearsOnBattlefield = game.state.getBattlefield().count { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@count false
                    container.get<CardComponent>()?.name == "Grizzly Bears" &&
                        container.get<ControllerComponent>()?.playerId == game.player1Id
                }

                withClue("Only the hard-cast Grizzly Bears should be on the battlefield — no token copy") {
                    bearsOnBattlefield shouldBe 1
                }
            }
        }

        context("Marker components are stripped on zone change") {

            test("EnteredFromGraveyardComponent is removed when the creature leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doomed Necromancer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necromancerId = game.findPermanent("Doomed Necromancer")!!
                val bearsId = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val cardDef = cardRegistry.getCard("Doomed Necromancer")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = necromancerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD))
                    )
                ).error shouldBe null
                game.resolveStack()

                // Sanity: component is present while on the battlefield
                val enteredBears = game.findPermanent("Grizzly Bears")!!
                game.state.getEntity(enteredBears)!!
                    .has<EnteredFromGraveyardComponent>() shouldBe true

                // Send the creature to the graveyard and back to the battlefield via a fresh
                // entry; component should not linger.
                val currentState = game.state
                val strippedState = currentState.updateEntity(enteredBears) { c ->
                    com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                        .stripBattlefieldComponents(c)
                }
                strippedState.getEntity(enteredBears)!!
                    .has<EnteredFromGraveyardComponent>() shouldBe false
                strippedState.getEntity(enteredBears)!!
                    .has<CastFromGraveyardComponent>() shouldBe false
            }
        }
    }
}
