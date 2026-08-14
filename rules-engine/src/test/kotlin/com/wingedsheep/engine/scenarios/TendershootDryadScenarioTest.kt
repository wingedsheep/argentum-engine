package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.citysblessing.CitysBlessingService
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tendershoot Dryad (RIX, printed here from BLC) — the other ascend card, and the one that pins the
 * *projection-mode* half of the city's blessing.
 *
 * Its "Saprolings you control get +2/+2 as long as you have the city's blessing" is a
 * `ConditionalStaticAbility`, so the layer system evaluates the `YouHaveCitysBlessing` gate **while
 * the projection is still being computed**. Ascend's ten-permanent count is itself a projected read
 * (granted keywords, Layer 2 control changes), so the gate has to be handed the in-flight projection
 * rather than reaching for `GameState.projectedState` — the lazy initializer would re-enter itself
 * and recurse until the stack overflows. A five-drop reaches ten permanents often enough that this
 * is the normal path through the card, not a corner.
 */
class TendershootDryadScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    fun saprolings(d: GameTestDriver, playerId: EntityId): List<EntityId> =
        d.getCreatures(playerId).filter { d.getCardName(it) == "Saproling Token" }

    test("ascend switches on the Saproling lord as soon as the tenth permanent arrives") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Nine permanents — one short. The Saproling the upkeep trigger is about to make is the tenth.
        d.putCreatureOnBattlefield(active, "Tendershoot Dryad")
        repeat(8) { d.putLandOnBattlefield(active, "Forest") }
        d.getPermanents(active).size shouldBe 9
        CitysBlessingService.has(d.state, active) shouldBe false

        // Walk into the next upkeep so "at the beginning of each upkeep" fires.
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.UPKEEP)
        repeat(10) {
            if (d.pendingDecision != null) d.autoResolveDecision()
            else if (d.getTopOfStack() != null) d.bothPass()
        }

        val saproling = saprolings(d, active).single()
        CitysBlessingService.has(d.state, active) shouldBe true

        // 1/1 base, +2/+2 from the now-live conditional static ability. Reading this at all is the
        // test: the gate is evaluated inside projection, on a board whose ascend count is projected.
        d.state.projectedState.getPower(saproling) shouldBe 3
        d.state.projectedState.getToughness(saproling) shouldBe 3
    }

    test("below ten permanents the Saprolings are plain 1/1s") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Tendershoot Dryad")
        repeat(3) { d.putLandOnBattlefield(active, "Forest") }

        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.UPKEEP)
        repeat(10) {
            if (d.pendingDecision != null) d.autoResolveDecision()
            else if (d.getTopOfStack() != null) d.bothPass()
        }

        val saproling = saprolings(d, active).single()
        CitysBlessingService.has(d.state, active) shouldBe false
        d.state.projectedState.getPower(saproling) shouldBe 1
        d.state.projectedState.getToughness(saproling) shouldBe 1
    }
})
