package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lazav, Wearer of Faces (MKM) — {U}{B} 2/3 Legendary Creature — Shapeshifter Detective.
 *
 * "Whenever Lazav attacks, exile target card from a graveyard, then investigate."
 * "Whenever you sacrifice a Clue, you may have Lazav become a copy of a creature card exiled with
 * it until end of turn."
 *
 * The pair is linked (CR 607): the attack trigger's exile is the *only* thing that stocks the pool
 * the sacrifice trigger reads, so these tests run them in sequence rather than seeding a linked
 * exile by hand. What they pin down is that the exile really lands on Lazav's
 * [LinkedExileComponent], that a Clue sacrificed for any reason is the trigger, that only creature
 * cards from that pile are offered, and that declining leaves Lazav alone.
 */
class LazavWearerOfFacesScenarioTest : ScenarioTestBase() {

    private val clueAbilityId = PredefinedTokens.Clue.activatedAbilities.first().id

    private fun clue(game: TestGame): EntityId? = game.state.getBattlefield()
        .firstOrNull { game.state.getEntity(it)?.get<CardComponent>()?.name == "Clue" }

    private fun linkedExile(game: TestGame): List<EntityId> =
        game.state.getEntity(game.findPermanent("Lazav, Wearer of Faces")!!)
            ?.get<LinkedExileComponent>()?.exiledIds.orEmpty()

    /** Attack with Lazav, exiling [graveyardCard]; leaves the Clue on the battlefield. */
    private fun TestGame.attackExiling(graveyardCard: EntityId) {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Lazav, Wearer of Faces" to 2)).error shouldBe null

        val decision = getPendingDecision()
        (decision is ChooseTargetsDecision) shouldBe true
        submitDecision(
            TargetsResponse((decision as ChooseTargetsDecision).id, mapOf(0 to listOf(graveyardCard)))
        ).error shouldBe null
        resolveStack()
    }

    init {
        test("attacking exiles the targeted graveyard card onto Lazav's linked exile and investigates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lazav, Wearer of Faces")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            game.attackExiling(bears)

            game.isInExile(2, "Grizzly Bears") shouldBe true
            linkedExile(game) shouldBe listOf(bears)
            clue(game).shouldNotBeNull()
        }

        test("sacrificing the Clue lets Lazav become a copy of the exiled creature card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lazav, Wearer of Faces")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            game.attackExiling(bears)

            val lazav = game.findPermanent("Lazav, Wearer of Faces").shouldNotBeNull()
            val clueId = clue(game).shouldNotBeNull()

            // Crack the Clue for its own draw — "for any reason" includes its own ability.
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = clueId, abilityId = clueAbilityId)
            ).error shouldBe null
            game.resolveStack()

            // The copy trigger offers exactly the creature cards in Lazav's linked exile.
            val selection = game.getPendingDecision()
            (selection is SelectCardsDecision) shouldBe true
            (selection as SelectCardsDecision).options shouldBe listOf(bears)

            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(lazav)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
            game.state.projectedState.getPower(lazav) shouldBe 2
            game.state.projectedState.getToughness(lazav) shouldBe 2
        }

        test("declining the choice leaves Lazav as himself") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lazav, Wearer of Faces")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            game.attackExiling(bears)

            val lazav = game.findPermanent("Lazav, Wearer of Faces").shouldNotBeNull()
            val clueId = clue(game).shouldNotBeNull()

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = clueId, abilityId = clueAbilityId)
            ).error shouldBe null
            game.resolveStack()

            // Selecting nothing is the printed "you may" declining.
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.state.getEntity(lazav)?.get<CardComponent>()?.name shouldBe "Lazav, Wearer of Faces"
            game.state.projectedState.getPower(lazav) shouldBe 2
            game.state.projectedState.getToughness(lazav) shouldBe 3
        }

        test("a noncreature card in the linked exile is never offered as a copy source") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lazav, Wearer of Faces")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInGraveyard(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
            game.attackExiling(bolt)

            linkedExile(game) shouldBe listOf(bolt)

            val lazav = game.findPermanent("Lazav, Wearer of Faces").shouldNotBeNull()
            val clueId = clue(game).shouldNotBeNull()

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = clueId, abilityId = clueAbilityId)
            ).error shouldBe null
            game.resolveStack()

            // The creature-card filter empties the pool, so there is nothing to prompt for and
            // Lazav is untouched — "a creature card exiled with it" had no candidate.
            val decision = game.getPendingDecision()
            if (decision is SelectCardsDecision) {
                decision.options shouldNotBe listOf(bolt)
                game.skipSelection()
            }
            game.resolveStack()

            game.state.getEntity(lazav)?.get<CardComponent>()?.name shouldBe "Lazav, Wearer of Faces"
        }
    }
}
