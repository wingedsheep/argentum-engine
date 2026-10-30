package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.thb.cards.CalixDestinysHand
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Calix, Destiny's Hand (THB #211): the −3 exiles a permanent you don't control until a *different*
 * target — an enchantment you control — leaves the battlefield. Calix leaving changes nothing; the
 * enchantment leaving returns the card at once; an enchantment already gone at resolution means
 * nothing is exiled (ruling).
 */
class CalixDestinysHandScenarioTest : ScenarioTestBase() {

    private val calix = "Calix, Destiny's Hand"
    private val minusThree = CalixDestinysHand.activatedAbilities[1].id

    private fun TestGame.setLoyalty(id: EntityId, loyalty: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) }
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, calix)
        .withCardOnBattlefield(1, "Glorious Anthem")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInHand(1, "Disenchant")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateMinusThree(): Pair<EntityId, EntityId> {
        val calixId = findPermanent(calix)!!
        setLoyalty(calixId, 4)
        val bears = findPermanent("Grizzly Bears")!!
        val anthem = findPermanent("Glorious Anthem")!!
        execute(
            ActivateAbility(
                player1Id, calixId, minusThree,
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(anthem))
            )
        ).error shouldBe null
        return bears to anthem
    }

    init {
        test("−3: the exile outlasts Calix and ends when the target enchantment leaves") {
            val game = board()
            val (bears, anthem) = game.activateMinusThree()
            game.resolveStack()

            game.state.getExile(game.player2Id) shouldContain bears

            game.castSpell(1, "Lightning Bolt", game.findPermanent(calix)).error shouldBe null
            game.resolveStack()
            withClue("Calix is gone, but it isn't the anchor") {
                game.findPermanent(calix) shouldBe null
                game.state.getExile(game.player2Id) shouldContain bears
            }

            game.castSpell(1, "Disenchant", anthem).error shouldBe null
            game.resolveStack()
            withClue("the enchantment left, so the card returned under its owner's control") {
                val returned = game.findPermanent("Grizzly Bears")
                returned shouldBe bears
                game.state.projectedState.getController(bears) shouldBe game.player2Id
                game.state.getExile(game.player2Id) shouldNotContain bears
            }
        }

        test("−3: nothing is exiled when the target enchantment is gone as the ability resolves") {
            val game = board()
            val (bears, anthem) = game.activateMinusThree()
            game.castSpell(1, "Disenchant", anthem).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Glorious Anthem") shouldBe true
            game.state.getBattlefield() shouldContain bears
            game.state.zoneReturns.size shouldBe 0
        }
    }
}
