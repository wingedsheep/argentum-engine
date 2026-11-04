package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.ZacamaPrimalCalamity
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zacama, Primal Calamity (RIX #174).
 *
 * Pins the "if you cast it" intervening-if: casting Zacama untaps every land you control —
 * including the ones tapped to pay for it, which then pay for its abilities — while putting it
 * onto the battlefield without casting it (Reanimate) untaps nothing.
 */
class ZacamaPrimalCalamityScenarioTest : ScenarioTestBase() {

    private val damageAbilityId get() = ZacamaPrimalCalamity.activatedAbilities[0].id

    private fun TestGame.tapped(id: EntityId): Boolean = state.getEntity(id)!!.has<TappedComponent>()

    private fun TestGame.lands(): List<EntityId> =
        listOf("Mountain", "Forest", "Plains", "Swamp").flatMap { findPermanents(it) }

    init {
        test("casting Zacama untaps all lands you control, and that mana pays for its abilities") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Zacama, Primal Calamity")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Swamp", tapped = true)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Zacama, Primal Calamity").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            val zacama = game.findPermanent("Zacama, Primal Calamity")
            zacama shouldNotBe null
            withClue("every land, including the Swamp tapped beforehand, is untapped") {
                game.lands().count { game.tapped(it) } shouldBe 0
            }

            val giant = game.findPermanent("Hill Giant")!!
            val result = game.execute(
                ActivateAbility(game.player1Id, zacama!!, damageAbilityId, targets = listOf(ChosenTarget.Permanent(giant)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.findPermanent("Hill Giant") shouldBe null
        }

        test("putting Zacama onto the battlefield without casting it untaps nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Reanimate")
                .withCardInGraveyard(1, "Zacama, Primal Calamity")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Plains", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingGraveyardCard(1, "Reanimate", 1, "Zacama, Primal Calamity").error shouldBe null
            game.resolveStack()

            game.findPermanent("Zacama, Primal Calamity") shouldNotBe null
            withClue("no cast, no untap: the Swamp paid for Reanimate and the Plains stays tapped") {
                game.lands().count { game.tapped(it) } shouldBe 2
            }
        }
    }
}
