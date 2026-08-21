package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Conspiracy Unraveler (MKM #47) — {5}{U}{U} Creature — Sphinx Detective 6/6.
 *
 * "Flying
 *  You may collect evidence 10 rather than pay the mana cost for spells you cast."
 *
 * The first **battlefield-granted alternative cost that isn't mana**. Jodah's grant substitutes one
 * mana cost for another; this substitutes a non-mana cost, so the grant carries the same two halves
 * a card's own `SelfAlternativeCost` does — a `{0}` mana half and an `AdditionalCost` list.
 *
 * Four claims, each pinning a different seam:
 *  - the cast really is free of mana and really does exile the evidence (enumeration → payment);
 *  - CR 701.59b fails closed — a graveyard that can't reach total mana value 10 is never *offered*
 *    the option, rather than offered it and refused (enumeration);
 *  - an under-total selection is rejected server-side even so, because every `GameAction` field is
 *    client-supplied (validation);
 *  - it does **not** satisfy a spell's own linked "if evidence was collected" clause — the printed
 *    ruling, and the reason the grant deliberately stamps no `ChoiceSlot`.
 *
 * Centaur Courser is {2}{G}, mana value 3, so four of them total 12 and three total 9 — the two
 * sides of the floor.
 */
class ConspiracyUnravelerScenarioTest : ScenarioTestBase() {

    init {
        test("collecting evidence 10 casts a spell with no mana at all") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Conspiracy Unraveler")
                .withCardInHand(1, "Conspiracy Unraveler")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Deliberately no lands: {5}{U}{U} is unpayable, so a resolution here can only have
            // come through the alternative cost.
            game.castSpellWithGrantedAlternativeCost(
                1, "Conspiracy Unraveler",
                evidenceNames = listOf(
                    "Centaur Courser", "Centaur Courser", "Centaur Courser", "Centaur Courser"
                )
            ).error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Conspiracy Unraveler") shouldHaveSize 2
            game.isInExile(1, "Centaur Courser") shouldBe true
            game.isInGraveyard(1, "Centaur Courser") shouldBe false
        }

        test("the option carries the sum-gated picker payload the client needs") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Conspiracy Unraveler")
                .withCardInHand(1, "Conspiracy Unraveler")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val alt = game.getLegalActions(1)
                .single { it.actionType == "CastWithAlternativeCost" }

            // Without this the client would open no picker and the cast would arrive unpaid.
            val costInfo = alt.additionalCostInfo.shouldNotBeNull()
            costInfo.costType shouldBe "CollectEvidence"
            costInfo.exileMinTotalWeight shouldBe 10
            costInfo.validExileTargets shouldHaveSize 4
            costInfo.exileCardWeights.values.toSet() shouldBe setOf(3)

            // `manaCostString` is machine-facing — the client substitutes X into it, counts generic
            // pips and drives its mana-source phase off it — so the `{0}` mana half must stay a
            // parseable mana cost and must not be repurposed as the label.
            alt.manaCostString shouldBe "{0}"

            // The label is the human-facing half: a purely non-mana grant names what is actually
            // being paid rather than reading "({0})".
            alt.description shouldNotBe "Cast Conspiracy Unraveler ({0})"
            alt.description.contains("collect evidence 10", ignoreCase = true) shouldBe true
        }

        test("a graveyard that can't reach 10 is never offered the option (CR 701.59b)") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Conspiracy Unraveler")
                .withCardInHand(1, "Conspiracy Unraveler")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Three Coursers is 9 — one short. The option must be absent, not present-and-refused:
            // a player who can't reach N can't *choose* to collect evidence at all.
            game.getLegalActions(1).none { it.actionType == "CastWithAlternativeCost" } shouldBe true

            // And submitting it anyway is rejected rather than silently trimmed.
            game.castSpellWithGrantedAlternativeCost(
                1, "Conspiracy Unraveler",
                evidenceNames = listOf("Centaur Courser", "Centaur Courser", "Centaur Courser")
            ).error shouldNotBe null
            game.isInGraveyard(1, "Centaur Courser") shouldBe true
        }

        test("an under-total selection is rejected even when the graveyard could have paid") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Conspiracy Unraveler")
                .withCardInHand(1, "Conspiracy Unraveler")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // 9 of an available 12 — overpaying is the payer's right, underpaying is not.
            game.castSpellWithGrantedAlternativeCost(
                1, "Conspiracy Unraveler",
                evidenceNames = listOf("Centaur Courser", "Centaur Courser", "Centaur Courser")
            ).error shouldNotBe null
            game.findAllPermanents("Conspiracy Unraveler") shouldHaveSize 1
            game.isInExile(1, "Centaur Courser") shouldBe false
        }

        test("it does not satisfy a spell's own linked 'if evidence was collected' clause") {
            // Vitu-Ghazi Inspector: "As an additional cost you may collect evidence 6. When this
            // creature enters, if evidence was collected, put a +1/+1 counter on target creature
            // and you gain 2 life." Paying Conspiracy Unraveler's *alternative* cost is not paying
            // the spell's own additional cost, so the linkage stays false (printed ruling).
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Conspiracy Unraveler")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Vitu-Ghazi Inspector")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val life = game.getLifeTotal(1)
            game.castSpellWithGrantedAlternativeCost(
                1, "Vitu-Ghazi Inspector",
                evidenceNames = listOf(
                    "Centaur Courser", "Centaur Courser", "Centaur Courser", "Centaur Courser"
                )
            ).error shouldBe null
            game.resolveStack()

            game.findPermanent("Vitu-Ghazi Inspector").shouldNotBeNull()
            // CR 603.4 — the intervening-if is false, so the trigger never reached the stack and
            // nothing asked for a target.
            game.state.pendingDecision shouldBe null
            game.getLifeTotal(1) shouldBe life
            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 shouldBe 0
        }
    }
}
