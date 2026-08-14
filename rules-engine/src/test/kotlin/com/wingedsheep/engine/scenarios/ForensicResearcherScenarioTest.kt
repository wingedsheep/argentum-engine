package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Forensic Researcher (MKM) — "{T}: Untap another target permanent you control. {T}, Collect
 * evidence 3: Tap target creature you don't control."
 *
 * The activated-ability-cost shape, and the card that shows collect evidence composing with `{T}`
 * through the ordinary cost machinery. Nothing here is linked (CR 701.59c), so the cost stamps no
 * choice slot; the rule that bites is CR 701.59b, which makes the second ability simply
 * unactivatable while the graveyard is under the threshold.
 */
class ForensicResearcherScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, name: String): Boolean =
        game.state.getEntity(game.findPermanent(name)!!)?.has<TappedComponent>() == true

    /** Activate the ability at [index] of [name], letting the engine auto-pay. */
    private fun TestGame.activateAbilityAt(
        name: String,
        index: Int,
        target: EntityId? = null,
    ) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent(name) ?: error("'$name' is not on the battlefield"),
            abilityId = cardRegistry.getCard(name)!!.activatedAbilities[index].id,
            targets = target?.let { listOf(ChosenTarget.Permanent(it)) } ?: emptyList(),
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    init {
        test("the collect-evidence ability taps an opposing creature and exiles the evidence") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Forensic Researcher")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.activateAbilityAt("Forensic Researcher", 1, target = bears).error shouldBe null
            game.resolveStack()

            // Centaur Courser is mana value 3 — exactly the threshold.
            game.isInExile(1, "Centaur Courser") shouldBe true
            isTapped(game, "Grizzly Bears") shouldBe true
            isTapped(game, "Forensic Researcher") shouldBe true
        }

        test("CR 701.59b — the ability is unaffordable with too little in the graveyard") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Forensic Researcher")
                .withCardOnBattlefield(2, "Grizzly Bears")
                // Mana value 1, short of 3.
                .withCardInGraveyard(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.getLegalActions(1)
                .filter { it.description.contains("Tap target creature you don't control") }
                .none { it.isAffordable } shouldBe true

            val bearsId = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.activateAbilityAt("Forensic Researcher", 1, target = bearsId)
                .error.shouldNotBeNull()
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            isTapped(game, "Forensic Researcher") shouldBe false
        }

        test("the untap ability carries no evidence cost and works with an empty graveyard") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Forensic Researcher")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            isTapped(game, "Grizzly Bears") shouldBe true

            game.activateAbilityAt("Forensic Researcher", 0, target = bears).error shouldBe null
            game.resolveStack()

            isTapped(game, "Grizzly Bears") shouldBe false
        }
    }
}
