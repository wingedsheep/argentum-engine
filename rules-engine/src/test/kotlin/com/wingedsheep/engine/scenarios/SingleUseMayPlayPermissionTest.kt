package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
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
 * Mechanic-level tests for `singleUse` on [Effects.GrantMayPlayFromExile] / [MayPlayPermission] —
 * "you may cast **an** instant or sorcery spell from among those exiled cards" (Chandra, Hope's
 * Beacon): one play through the grant spends it for the whole group, where an ordinary multi-card
 * grant keeps authorising the rest. Two inline granters differ only in `singleUse`, so the contrast
 * test pins what the flag actually changes.
 */
class SingleUseMayPlayPermissionTest : ScenarioTestBase() {

    init {
        fun granter(name: String, singleUse: Boolean) = card(name) {
            manaCost = "{1}{G}"
            typeLine = "Creature — Shapeshifter"
            power = 1
            toughness = 1
            triggeredAbility {
                trigger = Triggers.self.enters()
                effect = GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2), player = Player.You),
                    storeAs = "exiled"
                ) then
                    MoveCollectionEffect(
                        from = "exiled",
                        destination = CardDestination.ToZone(Zone.EXILE, player = Player.You)
                    ) then
                    Effects.GrantMayPlayFromExile(from = "exiled", singleUse = singleUse)
            }
        }

        cardRegistry.register(listOf(
            granter("Test SingleUse Granter", singleUse = true),
            granter("Test Group Granter", singleUse = false)
        ))

        fun setup(granterName: String, vararg library: String): TestGame {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, granterName)
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            library.forEach { builder.withCardInLibrary(1, it) }
            val game = builder.build()
            game.castSpell(1, granterName).error shouldBe null
            game.resolveStack()
            return game
        }

        fun canCast(game: TestGame, cardId: EntityId): Boolean =
            LegalActionEnumerator.create(cardRegistry).enumerate(game.state, game.player1Id)
                .any { (it.action as? CastSpell)?.cardId == cardId }

        test("casting one card from a single-use group revokes the grant for the rest") {
            val game = setup("Test SingleUse Granter", "Grizzly Bears", "Grizzly Bears")
            val (first, second) = exiledCardsNamed(game, "Grizzly Bears")

            withClue("Before any cast, both exiled cards are offered") {
                canCast(game, first) shouldBe true
                canCast(game, second) shouldBe true
            }

            game.execute(CastSpell(playerId = game.player1Id, cardId = first)).error shouldBe null
            game.resolveStack()

            withClue("The other exiled card is no longer offered once one was cast") {
                canCast(game, second) shouldBe false
            }
            withClue("The authoritative handler rejects it too — the action is client-supplied") {
                game.execute(CastSpell(playerId = game.player1Id, cardId = second)).error shouldNotBe null
            }
            withClue("It stays in exile, merely unplayable") {
                game.state.getExile(game.player1Id).contains(second) shouldBe true
            }
        }

        test("the grant is spent as the spell is cast, not when it resolves") {
            val game = setup("Test SingleUse Granter", "Grizzly Bears", "Grizzly Bears")
            val (first, second) = exiledCardsNamed(game, "Grizzly Bears")

            game.execute(CastSpell(playerId = game.player1Id, cardId = first)).error shouldBe null

            withClue("With the first spell still on the stack, the permission is already gone") {
                game.state.mayPlayPermissions.none { second in it.cardIds } shouldBe true
            }
        }

        test("playing a land from a single-use group spends the grant too") {
            val game = setup("Test SingleUse Granter", "Mountain", "Grizzly Bears")
            val mountain = exiledCardsNamed(game, "Mountain").single()
            val bears = exiledCardsNamed(game, "Grizzly Bears").single()

            game.execute(PlayLand(playerId = game.player1Id, cardId = mountain)).error shouldBe null

            withClue("The Mountain was the one card played from among them") {
                game.isOnBattlefield("Mountain") shouldBe true
                canCast(game, bears) shouldBe false
            }
        }

        test("without singleUse, every card in the group stays castable after one is cast") {
            val game = setup("Test Group Granter", "Grizzly Bears", "Grizzly Bears")
            val (first, second) = exiledCardsNamed(game, "Grizzly Bears")

            game.execute(CastSpell(playerId = game.player1Id, cardId = first)).error shouldBe null
            game.resolveStack()

            withClue("An ordinary multi-card grant keeps authorising the remaining cards") {
                canCast(game, second) shouldBe true
            }
        }

        test("a card also covered by an ordinary grant is cast through that one, sparing the single-use grant") {
            val game = setup("Test SingleUse Granter", "Grizzly Bears", "Grizzly Bears")
            val (first, second) = exiledCardsNamed(game, "Grizzly Bears")
            game.state = game.state.addMayPlayPermission(
                MayPlayPermission(
                    id = EntityId.generate(),
                    cardIds = setOf(first),
                    controllerId = game.player1Id,
                    timestamp = game.state.timestamp
                )
            )

            game.execute(CastSpell(playerId = game.player1Id, cardId = first)).error shouldBe null
            game.resolveStack()

            withClue("The single-use grant still covers the other card") {
                canCast(game, second) shouldBe true
            }
        }
    }

    private fun exiledCardsNamed(game: TestGame, name: String): List<EntityId> =
        game.state.getExile(game.player1Id).filter { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }
}
