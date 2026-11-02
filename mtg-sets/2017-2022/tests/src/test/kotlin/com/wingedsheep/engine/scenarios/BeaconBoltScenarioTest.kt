package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Beacon Bolt (GRN #154).
 *
 * "Beacon Bolt deals damage to target creature equal to the total number of instant and sorcery
 *  cards you own in exile and in your graveyard. / Jump-start"
 *
 * Pinned: the amount adds up your graveyard and your exile. It ignores non-instant/sorcery cards,
 * the opponent's graveyard and exile, and Beacon Bolt itself while it is on the stack. A card
 * discarded for jump-start counts.
 */
class BeaconBoltScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("counts your instants and sorceries in graveyard and exile, and nobody else's") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Beacon Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Divination")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInExile(1, "Shock")
                .withCardInGraveyard(2, "Lava Spike")
                .withCardInExile(2, "Lightning Bolt")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Beacon Bolt", game.findPermanent("Craw Wurm")!!).error shouldBe null
            game.resolveStack()

            withClue("Lightning Bolt + Divination in the graveyard, Shock in exile") {
                game.damageOn("Craw Wurm") shouldBe 3
            }
            game.isInGraveyard(1, "Beacon Bolt") shouldBe true
        }

        test("jump-started, it counts the discarded card but not itself, then is exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInGraveyard(1, "Beacon Bolt")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bolt = game.findCardsInGraveyard(1, "Beacon Bolt").single()
            val shock = game.findCardsInHand(1, "Shock").single()
            val wurm = game.findPermanent("Craw Wurm")!!

            val cast = game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == bolt }.action as CastSpell
            game.execute(
                cast.copy(
                    targets = listOf(ChosenTarget.Permanent(wurm)),
                    additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(shock))
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("Lightning Bolt + the discarded Shock; Beacon Bolt is on the stack") {
                game.damageOn("Craw Wurm") shouldBe 2
            }
            game.isInExile(1, "Beacon Bolt") shouldBe true
            game.isInGraveyard(1, "Beacon Bolt") shouldBe false
        }
    }
}
