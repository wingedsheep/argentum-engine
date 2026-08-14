package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mechanic-level tests for `nonLandOnly` on [Effects.GrantMayPlayFromExile] /
 * [com.wingedsheep.engine.state.permissions.MayPlayPermission] — the restriction added alongside
 * Ragavan, Nimble Pilferer to model "you may **cast** that card" wording, where a land among the
 * granted cards can never be played (CR 305.1 — playing a land is a special action, not a cast).
 * Ragavan's own scenario test exercises this too, but only through Ragavan's exact shape (a combat
 * damage trigger). This file proves the primitive itself, generically, with two inline test cards
 * that differ only in `nonLandOnly` — so the contrast test (baseline, no restriction) makes the
 * parameter's actual effect unambiguous.
 */
class NonLandOnlyMayPlayPermissionTest : ScenarioTestBase() {

    init {
        val nonLandOnlyGranter = card("Test NonLandOnly Granter") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Shapeshifter"
            power = 1
            toughness = 1
            triggeredAbility {
                trigger = Triggers.EntersBattlefield
                effect = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), player = Player.You),
                            storeAs = "exiled"
                        ),
                        MoveCollectionEffect(
                            from = "exiled",
                            destination = CardDestination.ToZone(Zone.EXILE, player = Player.You)
                        ),
                        Effects.GrantMayPlayFromExile(from = "exiled", nonLandOnly = true)
                    )
                )
            }
        }

        // Contrast baseline: the same shape without nonLandOnly, so a land it exiles CAN be played.
        val plainGranter = card("Test MayPlay Granter") {
            manaCost = "{1}{G}"
            typeLine = "Creature — Shapeshifter"
            power = 1
            toughness = 1
            triggeredAbility {
                trigger = Triggers.EntersBattlefield
                effect = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), player = Player.You),
                            storeAs = "exiled"
                        ),
                        MoveCollectionEffect(
                            from = "exiled",
                            destination = CardDestination.ToZone(Zone.EXILE, player = Player.You)
                        ),
                        Effects.GrantMayPlayFromExile(from = "exiled")
                    )
                )
            }
        }

        cardRegistry.register(listOf(nonLandOnlyGranter, plainGranter))

        test("a land exiled via a nonLandOnly grant can't be played") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test NonLandOnly Granter")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test NonLandOnly Granter").error shouldBe null
            game.resolveStack()

            val exiledLandId = exiledCardNamed(game, 1, "Mountain")

            withClue("'You may cast' never authorizes playing a land — the legal actions omit it") {
                val enumerator = LegalActionEnumerator.create(cardRegistry)
                val legalActions = enumerator.enumerate(game.state, game.player1Id)
                legalActions.none { action ->
                    (action.action as? PlayLand)?.cardId == exiledLandId
                } shouldBe true
            }

            withClue("The authoritative handler rejects the play too, not just the enumerator") {
                game.execute(PlayLand(playerId = game.player1Id, cardId = exiledLandId)).error shouldNotBe null
            }
        }

        test("a nonland card exiled via a nonLandOnly grant can still be cast normally") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test NonLandOnly Granter")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test NonLandOnly Granter").error shouldBe null
            game.resolveStack()

            val exiledCardId = exiledCardNamed(game, 1, "Grizzly Bears")
            game.execute(CastSpell(playerId = game.player1Id, cardId = exiledCardId)).error shouldBe null
            game.resolveStack()

            withClue("The exiled Grizzly Bears resolved onto the battlefield under Player1's control") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("without nonLandOnly, a land exiled via the plain grant can be played") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test MayPlay Granter")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test MayPlay Granter").error shouldBe null
            game.resolveStack()

            val exiledLandId = exiledCardNamed(game, 1, "Mountain")
            game.execute(PlayLand(playerId = game.player1Id, cardId = exiledLandId)).error shouldBe null

            withClue("Without nonLandOnly, the granted permission covers playing a land too") {
                game.isOnBattlefield("Mountain") shouldBe true
            }
        }

        test("a cast-only permission cannot impose land riders when another permission authorizes the play") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test MayPlay Granter")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test MayPlay Granter").error shouldBe null
            game.resolveStack()

            val exiledLandId = exiledCardNamed(game, 1, "Mountain")
            game.state = game.state.addMayPlayPermission(
                MayPlayPermission(
                    id = EntityId.generate(),
                    cardIds = setOf(exiledLandId),
                    controllerId = game.player1Id,
                    landEntersTapped = true,
                    nonLandOnly = true,
                    timestamp = game.state.timestamp
                )
            )

            game.execute(PlayLand(playerId = game.player1Id, cardId = exiledLandId)).error shouldBe null

            withClue("The land was played through the plain permission, so the cast-only rider is inapplicable") {
                game.state.getEntity(exiledLandId)?.has<TappedComponent>() shouldBe false
            }
        }
    }

    private fun exiledCardNamed(game: TestGame, playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }
}
