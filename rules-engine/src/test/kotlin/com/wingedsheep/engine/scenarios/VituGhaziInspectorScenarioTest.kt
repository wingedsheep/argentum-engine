package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Vitu-Ghazi Inspector (MKM) — "As an additional cost to cast this spell, you may collect evidence
 * 6. Reach. When this creature enters, if evidence was collected, put a +1/+1 counter on target
 * creature and you gain 2 life."
 *
 * The enters-trigger shape of the CR 701.59c linkage. What this card specifically pins, beyond the
 * mechanic-level coverage in [CollectEvidenceScenarioTest], is that the linkage survives the *stack
 * transition*: the choice is declared while the card is a spell, and the fact has to still be
 * readable once it is a permanent with an enters trigger.
 */
class VituGhaziInspectorScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)
            ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("collecting evidence 6 grows a creature and gains 2 life") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Vitu-Ghazi Inspector")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

            // Centaur Courser is {2}{G} — mana value 3, so two of them total exactly 6.
            game.castSpellCollectingEvidence(
                1, "Vitu-Ghazi Inspector", "Centaur Courser", "Centaur Courser"
            ).error shouldBe null
            game.resolveStack()
            // The enters trigger picks its target after the creature has resolved.
            game.selectTargets(listOf(bears))
            game.resolveStack()

            game.isInExile(1, "Centaur Courser") shouldBe true
            counters(game, "Grizzly Bears") shouldBe 1
            game.getLifeTotal(1) shouldBe life + 2
        }

        test("casting it without collecting evidence leaves the trigger off the stack entirely") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Vitu-Ghazi Inspector")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.castSpell(1, "Vitu-Ghazi Inspector").error shouldBe null
            game.resolveStack()

            // CR 603.4 — an intervening-if trigger whose condition is false never goes on the
            // stack, so nothing asked for a target.
            game.state.pendingDecision shouldBe null
            counters(game, "Grizzly Bears") shouldBe 0
            game.getLifeTotal(1) shouldBe life
            game.isInGraveyard(1, "Centaur Courser") shouldBe true
            game.findPermanent("Vitu-Ghazi Inspector").shouldNotBeNull()
        }

        test("it may target any creature, not only one you control") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Vitu-Ghazi Inspector")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.castSpellCollectingEvidence(
                1, "Vitu-Ghazi Inspector", "Centaur Courser", "Centaur Courser"
            ).error shouldBe null
            game.resolveStack()
            // The enters trigger picks its target after the creature has resolved.
            game.selectTargets(listOf(bears))
            game.resolveStack()

            // "target creature" is unrestricted — the printed card really does let you grow an
            // opponent's creature.
            counters(game, "Grizzly Bears") shouldBe 1
        }
    }
}
