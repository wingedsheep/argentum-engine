package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Secluded Starforge (EOE #257, Land).
 *
 * Oracle:
 *   {T}: Add {C}.
 *   {2}, {T}, Tap X untapped artifacts you control: Target creature gets +X/+0 until end
 *   of turn. Activate only as a sorcery.
 *   {5}, {T}: Create a 2/2 colorless Robot artifact creature token.
 *
 * BUG: activating the pump ability with X = 2 (tapping two Chrome Companions) does not
 * actually tap the chosen artifacts and/or the +X/+0 buff does not propagate X to the
 * ModifyStats effect. The two withClue blocks below split those two failure modes so the
 * caller can tell whether the bug is in cost payment or in X-amount propagation.
 */
class SecludedStarforgeTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Secluded Starforge") {

            test("pump ability taps the chosen artifacts and grants +X/+0") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    // The Starforge itself, untapped so it can pay its own {T}.
                    .withCardOnBattlefield(1, "Secluded Starforge", tapped = false, summoningSickness = false)
                    // Two artifact creatures Alice can tap to pay the X cost. Chrome
                    // Companion is a 2/1 Artifact Creature — Dog, so it matches the
                    // GameObjectFilter.Artifact filter on the cost.
                    .withCardOnBattlefield(1, "Chrome Companion", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Chrome Companion", tapped = false, summoningSickness = false)
                    // Mountains supply the {2} generic mana the activation needs.
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    // Pump target.
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    // Bob has a harmless permanent so he isn't "empty board".
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val starforgeId = game.findPermanent("Secluded Starforge")!!
                val chromes = game.findPermanents("Chrome Companion")
                withClue("Setup sanity: Alice should control exactly two Chrome Companions") {
                    chromes.size shouldBe 2
                }
                val grizzly = game.findPermanents("Grizzly Bears")
                    .first { id ->
                        game.state.getEntity(id)
                            ?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
                            ?.playerId == game.player1Id
                    }

                // The pump is the SECOND activated ability on Secluded Starforge —
                // index 0 is the {T}: Add {C} mana ability, index 1 is the pump, index
                // 2 is the {5}, {T}: Create-Robot ability.
                val starforgeDef = cardRegistry.getCard("Secluded Starforge")!!
                val pumpAbility = starforgeDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = starforgeId,
                        abilityId = pumpAbility.id,
                        targets = listOf(ChosenTarget.Permanent(grizzly)),
                        costPayment = AdditionalCostPayment(
                            tappedPermanents = chromes
                        ),
                        xValue = 2
                    )
                )
                withClue("Activating Secluded Starforge's pump with X=2 should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // (1) Cost payment: both Chrome Companions must be tapped after the
                // ability is announced. The cost is paid on announcement, before
                // resolution, so this should already be true here. If this assertion
                // fails the bug is in cost-payment plumbing for TapXPermanents.
                withClue(
                    "Cost payment: paying 'Tap X untapped artifacts you control' with both " +
                        "Chrome Companions should leave both artifacts tapped after activation. " +
                        "If this fails, the TapXPermanents cost is not actually tapping the " +
                        "chosen permanents."
                ) {
                    chromes.forEach { id ->
                        (game.state.getEntity(id)?.has<TappedComponent>() == true) shouldBe true
                    }
                }

                // Resolve the pump ability on the stack.
                game.resolveStack()

                // (2) Effect: Grizzly Bears (2/2) should be 4/2 after +X/+0 with X = 2.
                // If (1) passed but this fails, the bug is in X-amount propagation
                // from the cost to ModifyStatsEffect (DynamicAmount.XValue).
                withClue(
                    "Effect: Grizzly Bears should be 4/2 (+2/+0) after the pump resolves " +
                        "with X = 2. If cost-tapping succeeded but power stays at 2, the X " +
                        "amount isn't propagating from the TapXPermanents cost into the " +
                        "ModifyStatsEffect's DynamicAmount.XValue."
                ) {
                    val projected = stateProjector.project(game.state)
                    projected.getPower(grizzly) shouldBe 4
                    projected.getToughness(grizzly) shouldBe 2
                }
            }

            // ---------------------------------------------------------------
            // Bug reproducer driven through legal-actions + decisions instead
            // of pre-filling xValue / tappedPermanents on ActivateAbility.
            //
            // User-reported symptom: "After choosing X=3 in the frontend I get
            // 'Select permanents to tap 0/0' but all artifacts have a blue border."
            //
            // In engine terms the contract is a two-step decision flow:
            //   1. Submit the legal-action's ActivateAbility (xValue=null,
            //      costPayment=null) — frontend hasn't yet collected those.
            //   2. Engine emits a ChooseNumberDecision for X. Player answers 3.
            //   3. Engine emits a follow-up SelectCardsDecision asking the player
            //      to tap exactly 3 untapped artifacts, with minSelections=3.
            //   4. Player submits the 3 chromes; ability resolves; Grizzly is 5/2.
            //
            // The bug is in step 3: the engine never raises that follow-up
            // decision (or raises it with minSelections=0 / a "Select 0/0"
            // prompt), because ActivatedAbilityEnumerator emits tapCount=0 as
            // a sentinel for X-variable costs and no continuation bridges the
            // chosen X back into a tap-selection decision with the right count.
            // ---------------------------------------------------------------
            test("UI flow: choosing X=3 produces a 3-permanent tap-selection decision (not 0/0)") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Secluded Starforge", tapped = false, summoningSickness = false)
                    // Three Chrome Companions so we can prove the follow-up
                    // decision asks for *exactly X*, not "0..all artifacts".
                    .withCardOnBattlefield(1, "Chrome Companion", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Chrome Companion", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Chrome Companion", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val starforgeId = game.findPermanent("Secluded Starforge")!!
                val chromes = game.findPermanents("Chrome Companion")
                withClue("Setup sanity: Alice should control exactly three Chrome Companions") {
                    chromes.size shouldBe 3
                }
                val grizzly = game.findPermanents("Grizzly Bears")
                    .first { id ->
                        game.state.getEntity(id)
                            ?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
                            ?.playerId == game.player1Id
                    }

                // ------------------------------------------------------------
                // Step 1: enumerate legal actions for Alice and find the pump.
                // ------------------------------------------------------------
                val legalActions = game.getLegalActions(1)
                val pumpAction = legalActions
                    .filter { it.actionType == "ActivateAbility" }
                    .mapNotNull { info ->
                        val activate = info.action as? ActivateAbility ?: return@mapNotNull null
                        if (activate.sourceId != starforgeId) return@mapNotNull null
                        info to activate
                    }
                    .single { (info, _) -> info.hasXCost }
                val (pumpInfo, pumpActivate) = pumpAction

                withClue(
                    "Enumeration sanity: the pump ability must surface as hasXCost=true " +
                        "with TapPermanents additional-cost info exposing the three " +
                        "candidate chromes as validTapTargets."
                ) {
                    pumpInfo.hasXCost shouldBe true
                    val costInfo = pumpInfo.additionalCostInfo.shouldNotBeNull()
                    costInfo.costType shouldBe "TapPermanents"
                    costInfo.validTapTargets shouldContainAll chromes
                }

                // ------------------------------------------------------------
                // Step 2: submit the legal-action's ActivateAbility as-is —
                // xValue/costPayment unfilled, since the frontend hasn't yet
                // asked the player. The engine is expected to PAUSE here and
                // raise a ChooseNumberDecision for X.
                //
                // We still have to attach the spell target (the legal-action
                // describes it via requiresTargets/validTargets and the
                // frontend wires it onto the action before submitting).
                // ------------------------------------------------------------
                val submission = pumpActivate.copy(
                    targets = listOf(ChosenTarget.Permanent(grizzly))
                )
                val activateResult = game.execute(submission)
                withClue(
                    "Submitting ActivateAbility from getLegalActions without pre-filling " +
                        "xValue/tappedPermanents must not error — the engine should pause " +
                        "for the X-selection decision instead. Got error: ${activateResult.error}"
                ) {
                    activateResult.error shouldBe null
                }

                // ------------------------------------------------------------
                // Step 3: answer the ChooseNumberDecision for X with 3.
                // ------------------------------------------------------------
                val xDecision = game.getPendingDecision().shouldNotBeNull()
                xDecision.shouldBeInstanceOf<ChooseNumberDecision>()
                game.chooseNumber(3).error shouldBe null

                // ------------------------------------------------------------
                // Step 4: THE BUG. After X=3 is chosen the engine should raise
                // a SelectCardsDecision asking for EXACTLY 3 untapped artifacts
                // (minSelections=3, maxSelections=3), with the three chromes as
                // options. Today the engine either:
                //   - never raises this decision (state.pendingDecision is null
                //     and the ability has already resolved with no tapping), OR
                //   - raises it with minSelections=0 / maxSelections=0 ("Select
                //     0/0") — which is the literal user-visible bug.
                // ------------------------------------------------------------
                val tapDecision: PendingDecision = game.getPendingDecision().shouldNotBeNull()
                withClue(
                    "After choosing X=3 the engine must raise a SelectCardsDecision for " +
                        "the tap-target selection (was: ${tapDecision::class.simpleName})."
                ) {
                    tapDecision.shouldBeInstanceOf<SelectCardsDecision>()
                }
                val tapSelect = tapDecision as SelectCardsDecision
                withClue(
                    "BUG: the tap-target selection's required count must equal the chosen " +
                        "X (3). Today the SelectCardsDecision surfaces with minSelections=" +
                        "${tapSelect.minSelections} and maxSelections=${tapSelect.maxSelections}, " +
                        "so the player sees 'Select permanents to tap 0/0' with the chromes " +
                        "highlighted but no way to actually tap any of them."
                ) {
                    tapSelect.minSelections shouldBe 3
                    tapSelect.maxSelections shouldBe 3
                    tapSelect.options shouldContainAll chromes
                }

                // ------------------------------------------------------------
                // Step 5: with X bound and the three chromes selected, the
                // ability should resolve and Grizzly Bears should be 5/2.
                // (We only get here if the failing assertion above is fixed.)
                // ------------------------------------------------------------
                game.selectCards(chromes).error shouldBe null
                game.resolveStack()

                chromes.forEach { id ->
                    (game.state.getEntity(id)?.has<TappedComponent>() == true) shouldBe true
                }
                val projected = stateProjector.project(game.state)
                projected.getPower(grizzly) shouldBe 5
                projected.getToughness(grizzly) shouldBe 2
            }
        }
    }
}
