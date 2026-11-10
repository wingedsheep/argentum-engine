package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Containment Membrane (OGW #52, {2}{U}, Enchantment — Aura).
 *
 *   Surge {U}
 *   Enchant creature
 *   Enchanted creature doesn't untap during its controller's untap step.
 *
 * The first Aura cast for its surge cost: the keyword-alternative cast must still choose and bind
 * the Aura's enchant target.
 */
class ContainmentMembraneScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Containment Membrane")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
        .withCardOnBattlefield(2, "Glory Seeker", tapped = true)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.surgeOnto(target: EntityId) = execute(
        CastSpell(
            player1Id, findCardsInHand(1, "Containment Membrane").first(),
            targets = listOf(ChosenTarget.Permanent(target)),
            useAlternativeCost = true, alternativeCostType = AlternativeCostType.SURGE,
        )
    )

    private fun ScenarioTestBase.TestGame.isTapped(id: EntityId) =
        state.getEntity(id)?.get<TappedComponent>() != null

    init {
        test("surged for {U} after Lightning Bolt: it enchants the target and the creature stays tapped") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            game.surgeOnto(bears).error shouldBe null
            game.resolveStack()

            val membrane = game.findPermanent("Containment Membrane")!!
            game.state.getEntity(membrane)?.get<AttachedToComponent>()?.targetId shouldBe bears

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("the enchanted creature skips its controller's untap step") {
                game.isTapped(bears) shouldBe true
            }
            withClue("the opponent's other creature untaps normally") {
                game.isTapped(seeker) shouldBe false
            }
        }

        test("surge can't be paid before another spell was cast this turn") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.surgeOnto(bears).error shouldNotBe null
            game.isInHand(1, "Containment Membrane") shouldBe true
        }
    }
}
