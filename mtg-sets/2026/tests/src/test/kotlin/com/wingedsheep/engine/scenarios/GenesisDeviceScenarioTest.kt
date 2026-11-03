package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.trk.cards.GenesisDevice
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Genesis Device — {2}{G} Legendary Artifact (TRK #200)
 * "{3}{G}{G}, {T}, Sacrifice Genesis Device: Sacrifice any number of lands you control, then search
 * your library for up to that many land cards, put them onto the battlefield tapped, then shuffle.
 * Activate only as a sorcery."
 *
 * "Up to that many" reads the lands sacrificed on resolution. The Device itself is sacrificed as a
 * cost and must not inflate that count, which is what this test pins.
 */
class GenesisDeviceScenarioTest : ScenarioTestBase() {

    private val abilityId = GenesisDevice.activatedAbilities.single().id

    private fun game(activePlayer: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Genesis Device")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Grizzly Bears")
        .withActivePlayer(activePlayer)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("sacrificing two lands finds up to two lands, not three — the Device is not counted") {
            val g = game()
            val device = g.findPermanent("Genesis Device")!!

            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = device, abilityId = abilityId))
                .error shouldBe null
            if (g.hasPendingDecision()) g.submitManaSourcesAutoPay()
            withClue("the Device is sacrificed as a cost") {
                g.isOnBattlefield("Genesis Device") shouldBe false
                g.findCardsInGraveyard(1, "Genesis Device").size shouldBe 1
            }
            g.resolveStack()

            val sacrifice = g.getPendingDecision() as SelectCardsDecision
            withClue("every land you control is offered, tapped or not") {
                sacrifice.options.size shouldBe 6
            }
            g.selectCards(g.findPermanents("Forest").take(2)).error shouldBe null

            val search = g.getPendingDecision() as SelectCardsDecision
            withClue("the search allows exactly as many lands as were sacrificed") {
                search.maxSelections shouldBe 2
            }
            withClue("only land cards are searchable") {
                search.options.contains(g.findCardsInLibrary(1, "Grizzly Bears").first()) shouldBe false
            }
            val mountain = g.findCardsInLibrary(1, "Mountain").first()
            val island = g.findCardsInLibrary(1, "Island").first()
            g.selectCards(listOf(mountain, island)).error shouldBe null
            g.resolveStack()

            withClue("two Forests were sacrificed") {
                g.findCardsInGraveyard(1, "Forest").size shouldBe 2
                g.findPermanents("Forest").size shouldBe 4
            }
            withClue("the fetched lands entered tapped") {
                listOf("Mountain", "Island").forEach { name ->
                    (g.state.getEntity(g.findPermanent(name)!!)?.has<TappedComponent>() ?: false) shouldBe true
                }
                g.isOnBattlefield("Plains") shouldBe false
            }
        }

        test("activate only as a sorcery") {
            val g = game(activePlayer = 2)
            val device = g.findPermanent("Genesis Device")!!

            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = device, abilityId = abilityId))
                .error shouldNotBe null
            g.isOnBattlefield("Genesis Device") shouldBe true
        }
    }
}
