package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.WizardsRockets
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wizard's Rockets (LTR) — "{X}, {T}, Sacrifice this artifact: Add X mana in any combination of
 * colors." When the ability is activated from the legal-actions/UI path (a bare ActivateAbility
 * with no xValue), the engine must pause to let the player **choose X** — otherwise X defaults to
 * 0, the cost pays nothing, and the ability produces no mana.
 */
class WizardsRocketsXChoiceScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(WizardsRockets)
        )
        return d
    }

    test("activating with no xValue pauses to choose X, then pays it and produces X mana") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val rockets = d.putPermanentOnBattlefield(active, "Wizard's Rockets")
        d.untapPermanent(rockets)
        repeat(2) { d.putLandOnBattlefield(active, "Mountain") } // untapped mana for {X} = 2
        val abilityId = WizardsRockets.activatedAbilities.first().id
        val handBefore = d.getHandSize(active)

        // Bare activation (no xValue) — the legal-actions/UI path.
        val res = d.submit(ActivateAbility(playerId = active, sourceId = rockets, abilityId = abilityId))
        res.isPaused shouldBe true
        // It must pause asking for X (not silently resolve at X=0).
        d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()

        // Choose X = 2.
        d.submitDecision(active, NumberChosenResponse(d.pendingDecision!!.id, 2))
        // Resolve the remaining flow: "add 2 mana in any combination of colors" pauses per mana for
        // a color choice; then the sacrifice's draw trigger goes on the stack.
        repeat(12) {
            val dec = d.pendingDecision
            when {
                dec is ChooseColorDecision -> d.submitDecision(active, ColorChosenResponse(dec.id, Color.RED))
                dec != null -> d.autoResolveDecision()
                else -> d.bothPass()
            }
        }

        // The artifact was sacrificed to pay the cost, and its dies-trigger drew a card.
        d.getGraveyard(active).any { d.getCardName(it) == "Wizard's Rockets" } shouldBe true
        d.getHandSize(active) shouldBe handBefore + 1
        // X = 2 was actually chosen and paid: two lands were tapped for the {2} cost (if X had
        // silently defaulted to 0, no lands would be tapped).
        d.getLands(active).count { d.isTapped(it) } shouldBe 2
    }
})
