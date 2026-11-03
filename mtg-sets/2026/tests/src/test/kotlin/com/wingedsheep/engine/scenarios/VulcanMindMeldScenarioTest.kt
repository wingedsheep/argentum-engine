package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vulcan Mind-Meld (TRK #88) — {U} sorcery.
 *
 *   Target opponent looks at the top card of your library, then exiles it face down. You look at
 *   the top two cards of their library, then exile them face down. For each card exiled this way,
 *   the player who exiled it may play it for as long as it remains exiled. Mana of any type can be
 *   spent to cast spells this way.
 *
 * Pins the two permissions pointing in opposite directions: the caster's own top card is the
 * *opponent's* to play (and only they can see it), the opponent's top two are the caster's.
 */
class VulcanMindMeldScenarioTest : ScenarioTestBase() {

    init {
        test("each player may play the cards they exiled, with mana of any type") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Vulcan Mind-Meld")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()
            val bears = game.findCardsInLibrary(2, "Grizzly Bears").single()
            val island = game.findCardsInLibrary(2, "Island").single()

            game.castSpellTargetingPlayer(1, "Vulcan Mind-Meld", targetPlayerNumber = 2).error shouldBe null
            game.resolveStack()

            withClue("every exiled card is face down, each in its owner's exile") {
                game.state.getExile(game.player1Id) shouldContainExactlyInAnyOrder listOf(bolt)
                game.state.getExile(game.player2Id) shouldContainExactlyInAnyOrder listOf(bears, island)
                listOf(bolt, bears, island).forEach {
                    game.state.getEntity(it)?.get<FaceDownComponent>().shouldNotBeNull()
                }
            }

            val boltGrant = game.state.mayPlayPermissions.single { bolt in it.cardIds }
            val theirGrant = game.state.mayPlayPermissions.single { bears in it.cardIds }
            withClue("the opponent exiled the caster's top card, so the opponent may play it") {
                boltGrant.controllerId shouldBe game.player2Id
                boltGrant.permanent shouldBe true
                boltGrant.withAnyManaType shouldBe true
            }
            withClue("the caster exiled the opponent's top two, so the caster may play them") {
                theirGrant.controllerId shouldBe game.player1Id
                theirGrant.cardIds.toSet() shouldBe setOf(bears, island)
                theirGrant.permanent shouldBe true
                theirGrant.withAnyManaType shouldBe true
            }

            fun faceDownTo(viewer: EntityId, card: EntityId) =
                stateTransformer.transform(game.state, viewingPlayerId = viewer).cards[card].shouldNotBeNull().isFaceDown
            withClue("only the player who may play a face-down card sees it") {
                faceDownTo(game.player2Id, bolt) shouldBe false
                faceDownTo(game.player1Id, bolt) shouldBe true
                faceDownTo(game.player1Id, bears) shouldBe false
                faceDownTo(game.player2Id, bears) shouldBe true
            }

            withClue("the caster can't cast their own card — that permission is the opponent's") {
                game.execute(CastSpell(game.player1Id, bolt, emptyList())).error shouldNotBe null
            }
            withClue("the caster casts the opponent's {1}{G} Bears with Islands") {
                game.execute(CastSpell(game.player1Id, bears, emptyList())).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getController(game.findPermanent("Grizzly Bears")!!) shouldBe game.player1Id
            }
        }
    }
}
