package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eoe.cards.AtomicMicrosizer
import com.wingedsheep.mtg.sets.definitions.eoe.cards.AuxiliaryBoosters
import com.wingedsheep.mtg.sets.definitions.eoe.cards.ChromeCompanion
import com.wingedsheep.mtg.sets.definitions.eoe.cards.RustHarvester
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reproduces the bug: activating Rust Harvester's
 *   "{2}, {T}, Exile an artifact card from your graveyard: ..."
 * ability auto-picks which artifact to exile.
 *
 * The exile is a COST (left of the colon, no "target" keyword), so the player must
 * be prompted to choose WHICH artifact card from their graveyard to exile. With
 * three different artifact cards in the graveyard, the engine must hand back a
 * SelectCardsDecision listing all three. It instead silently picks the first card
 * (see CostHandler.kt:exileCardsFromGraveyard — `validCards.take(count)`).
 */
class RustHarvesterExileCostChoiceTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                RustHarvester,
                AtomicMicrosizer,
                AuxiliaryBoosters,
                ChromeCompanion,
            )
        )
        return driver
    }

    val abilityId = RustHarvester.activatedAbilities.first().id

    test("Activating Rust Harvester must prompt the player to choose which artifact to exile from graveyard (not auto-pick)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Forest" to 30))
        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Alice: Rust Harvester (no summoning sickness) and two Mountains for {2}.
        val rustHarvester = driver.putCreatureOnBattlefield(alice, "Rust Harvester")
        driver.replaceState(
            driver.state.updateEntity(rustHarvester) { it.without<SummoningSicknessComponent>() }
        )
        repeat(2) { driver.putLandOnBattlefield(alice, "Mountain") }

        // Alice has THREE distinct artifact cards in her graveyard — a real choice.
        val atomicMicrosizer = driver.putCardInGraveyard(alice, "Atomic Microsizer")
        val auxiliaryBoosters = driver.putCardInGraveyard(alice, "Auxiliary Boosters")
        val chromeCompanion = driver.putCardInGraveyard(alice, "Chrome Companion")
        val artifactGraveyardCards = listOf(atomicMicrosizer, auxiliaryBoosters, chromeCompanion)

        // Bob: a Centaur Courser to damage with the pump-then-zap effect.
        val centaurCourser = driver.putCreatureOnBattlefield(bob, "Centaur Courser")
        driver.replaceState(
            driver.state.updateEntity(centaurCourser) { it.without<SummoningSicknessComponent>() }
        )

        // Sanity: legal-action enumerator advertises the activation with all
        // three graveyard cards as valid exile candidates (this part is correct).
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, alice, EnumerationMode.FULL)
        val activate = actions.single {
            val a = it.action
            a is ActivateAbility && a.sourceId == rustHarvester && a.abilityId == abilityId
        }
        activate.affordable shouldBe true
        val costInfo = activate.additionalCostInfo
        costInfo.shouldNotBeNull()
        costInfo.costType shouldBe "ExileFromGraveyard"
        costInfo.exileMinCount shouldBe 1
        costInfo.exileMaxCount shouldBe 1
        costInfo.validExileTargets shouldContainAll artifactGraveyardCards

        // Drive the activation through the engine WITHOUT pre-filling exiledCards.
        // The damage target is chosen; the exile victim is a cost-choice that
        // must surface as a pending decision.
        val result = driver.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = rustHarvester,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(centaurCourser)),
                costPayment = null,
            )
        )
        // The activation should not error — it pauses for the cost-choice decision.
        // (Mirrors SecludedStarforgeTest's UI-flow pattern: paused-for-decision is not
        // an error, and `ExecutionResult.isSuccess` is false while paused.)
        result.error shouldBe null

        // BUG: engine auto-picks the first artifact instead of prompting. The
        // assertions below describe the correct behaviour; the test is expected
        // to FAIL at the pending-decision check, with `pendingDecision == null`.
        val pending = driver.pendingDecision
        pending.shouldNotBeNull()

        val cardSelection = pending.shouldBeInstanceOf<SelectCardsDecision>()
        cardSelection.playerId shouldBe alice
        cardSelection.minSelections shouldBe 1
        cardSelection.maxSelections shouldBe 1
        cardSelection.options shouldContainAll artifactGraveyardCards
    }
})
