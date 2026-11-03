package com.wingedsheep.engine.state.permissions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [MayPlayCardsFromExile.zones]: the filter-defined may-play static reaching past exile into the
 * granted player's own graveyard and sideboard (The Holodeck), with its once-per-turn allowance
 * charged only when no other graveyard route already covered the play.
 */
class MayPlayCardsFromZonesTest : ScenarioTestBase() {
    init {
        cardRegistry.register(card("Test Grave Deck") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility {
                ability = MayPlayCardsFromExile(
                    filter = GameObjectFilter.Permanent.ownedByYou(),
                    oncePerTurn = true,
                    zones = setOf(Zone.GRAVEYARD, Zone.SIDEBOARD),
                )
            }
        })
        cardRegistry.register(card("Test Exile Deck") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility { ability = MayPlayCardsFromExile(filter = GameObjectFilter.Permanent) }
        })
        cardRegistry.register(card("Test Gravetide") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility { ability = MayPlayPermanentsFromGraveyard }
        })
        cardRegistry.register(card("Test Crucible") {
            manaCost = "{0}"; typeLine = "Artifact"
            staticAbility { ability = MayPlayLandsFromGraveyard }
        })
        cardRegistry.register(card("Test Zone Bear") {
            manaCost = "{0}"; typeLine = "Creature — Bear"; power = 2; toughness = 2
        })

        fun TestGame.offered(cardId: EntityId): Boolean =
            LegalActionEnumerator.create(cardRegistry).enumerate(state, player1Id).any {
                (it.action as? CastSpell)?.cardId == cardId || (it.action as? PlayLand)?.cardId == cardId
            }

        fun TestGame.spent(granter: String): Boolean =
            state.getEntity(findPermanent(granter)!!)?.has<MayCastFromLinkedExileUsedThisTurnComponent>() == true

        fun game(vararg permanents: String) = scenario()
            .withPlayers("Player1", "Player2")
            .apply { permanents.forEach { withCardOnBattlefield(1, it) } }
            .withCardInGraveyard(1, "Test Zone Bear")
            .withCardInGraveyard(1, "Forest")
            .withCardInGraveyard(2, "Test Zone Bear")
            .withCardInExile(1, "Test Zone Bear")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("a graveyard grant reaches only the player's own graveyard, not exile or an opponent's graveyard") {
            val g = game("Test Grave Deck")
            g.offered(g.findCardsInGraveyard(1, "Test Zone Bear").single()) shouldBe true
            g.offered(g.findCardsInGraveyard(1, "Forest").single()) shouldBe true
            g.offered(g.findCardsInGraveyard(2, "Test Zone Bear").single()) shouldBe false
            g.offered(g.state.getExile(g.player1Id).single()) shouldBe false
        }

        test("the default exile-only grant does not reach the graveyard") {
            val g = game("Test Exile Deck")
            g.offered(g.findCardsInGraveyard(1, "Test Zone Bear").single()) shouldBe false
            g.offered(g.state.getExile(g.player1Id).single()) shouldBe true
        }

        test("a cast from the graveyard spends the allowance, closing the land play too") {
            val g = game("Test Grave Deck")
            g.execute(CastSpell(g.player1Id, g.findCardsInGraveyard(1, "Test Zone Bear").single())).error shouldBe null
            g.spent("Test Grave Deck") shouldBe true
            g.offered(g.findCardsInGraveyard(1, "Forest").single()) shouldBe false
        }

        test("a Muldrotha-covered cast is charged to Muldrotha, not to the once-per-turn grant") {
            val g = game("Test Grave Deck", "Test Gravetide")
            g.execute(CastSpell(g.player1Id, g.findCardsInGraveyard(1, "Test Zone Bear").single())).error shouldBe null
            g.spent("Test Grave Deck") shouldBe false
            g.state.getEntity(g.findPermanent("Test Gravetide")!!)?.get<GraveyardPlayPermissionUsedComponent>() shouldNotBe null
        }

        test("a Crucible-covered land play leaves the once-per-turn grant unspent") {
            val g = game("Test Grave Deck", "Test Crucible")
            g.execute(PlayLand(g.player1Id, g.findCardsInGraveyard(1, "Forest").single())).error shouldBe null
            g.spent("Test Grave Deck") shouldBe false
        }

        test("a land played from the graveyard through the grant spends it") {
            val g = game("Test Grave Deck")
            g.execute(PlayLand(g.player1Id, g.findCardsInGraveyard(1, "Forest").single())).error shouldBe null
            g.spent("Test Grave Deck") shouldBe true
            g.offered(g.findCardsInGraveyard(1, "Test Zone Bear").single()) shouldBe false
        }

        test("zones outside exile, graveyard and sideboard are rejected") {
            shouldThrow<IllegalArgumentException> {
                MayPlayCardsFromExile(filter = GameObjectFilter.Any, zones = setOf(Zone.HAND))
            }
            shouldThrow<IllegalArgumentException> {
                MayPlayCardsFromExile(filter = GameObjectFilter.Any, zones = emptySet())
            }
        }
    }
}
