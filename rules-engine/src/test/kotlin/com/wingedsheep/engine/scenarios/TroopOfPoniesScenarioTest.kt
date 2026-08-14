package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Troop of Ponies (HOB #3) — {2} Creature — Horse 2/1
 * "{2}, {T}, Sacrifice this creature: Search your library for up to two basic land cards, reveal
 * them, put one onto the battlefield tapped and the other into your hand, then shuffle."
 *
 * The Cultivate split: a *second* selection decides which found card enters tapped and which goes
 * to hand. The regression this guards against is both cards landing in the same zone — the shape a
 * single-destination `searchLibrary(destination = HAND, entersTapped = true)` produces.
 */
class TroopOfPoniesScenarioTest : ScenarioTestBase() {

    private val searchAbilityId by lazy {
        cardRegistry.requireCard("Troop of Ponies").activatedAbilities[0].id
    }

    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Troop of Ponies", tapped = false, summoningSickness = false)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Ancestral Recall")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("one basic enters tapped, the other goes to hand, and the Ponies are sacrificed") {
            val g = game()
            val ponies = g.findPermanent("Troop of Ponies")!!

            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = ponies, abilityId = searchAbilityId))
                .error shouldBe null
            g.resolveStack()

            // First selection: which basics to find.
            val forest = g.findCardsInLibrary(1, "Forest").first()
            val mountain = g.findCardsInLibrary(1, "Mountain").first()
            (g.getPendingDecision() is SelectCardsDecision) shouldBe true
            withClue("only basic lands are searchable") {
                (g.getPendingDecision() as SelectCardsDecision).options
                    .contains(g.findCardsInLibrary(1, "Ancestral Recall").first()) shouldBe false
            }
            g.selectCards(listOf(forest, mountain)).error shouldBe null

            // Second selection: which of the two enters the battlefield tapped.
            (g.getPendingDecision() is SelectCardsDecision) shouldBe true
            g.selectCards(listOf(forest)).error shouldBe null
            g.resolveStack()

            withClue("the Forest entered the battlefield tapped") {
                g.isOnBattlefield("Forest") shouldBe true
                (g.state.getEntity(g.findPermanent("Forest")!!)?.has<TappedComponent>() ?: false) shouldBe true
            }
            withClue("the Mountain went to hand, not the battlefield") {
                g.isInHand(1, "Mountain") shouldBe true
                g.isOnBattlefield("Mountain") shouldBe false
            }
            withClue("the sacrifice cost was paid") {
                g.isOnBattlefield("Troop of Ponies") shouldBe false
                g.isInGraveyard(1, "Troop of Ponies") shouldBe true
            }
        }

        test("finding a single basic still lets it go to the battlefield tapped") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Troop of Ponies", tapped = false, summoningSickness = false)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ponies = g.findPermanent("Troop of Ponies")!!

            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = ponies, abilityId = searchAbilityId))
                .error shouldBe null
            g.resolveStack()

            var guard = 0
            while (g.getPendingDecision() is SelectCardsDecision && guard++ < 4) {
                val forest = g.findCardsInLibrary(1, "Forest").firstOrNull()
                if (forest != null) g.selectCards(listOf(forest)) else g.skipSelection()
                g.resolveStack()
            }

            withClue("the only basic found enters tapped; nothing is left over for the hand") {
                g.isOnBattlefield("Forest") shouldBe true
                (g.state.getEntity(g.findPermanent("Forest")!!)?.has<TappedComponent>() ?: false) shouldBe true
                g.isInHand(1, "Forest") shouldBe false
            }
        }
    }
}
