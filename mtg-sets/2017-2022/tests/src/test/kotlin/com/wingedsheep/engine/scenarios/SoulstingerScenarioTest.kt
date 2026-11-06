package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Soulstinger (AKH #108) — {3}{B} Creature — Scorpion Demon 4/5.
 *
 *   When this creature enters, put two -1/-1 counters on target creature you control.
 *   When this creature dies, you may put a -1/-1 counter on target creature for each -1/-1 counter
 *   on this creature.
 *
 * Soulstinger puts its own two counters on itself, then a test instant adds three more and kills it.
 * The dies trigger resolves with Soulstinger already in the graveyard, so the count must come from
 * last-known information: all five -1/-1 counters, including the ones that killed it. Declining the
 * "you may" puts none.
 */
class SoulstingerScenarioTest : ScenarioTestBase() {

    private val giant = card("Soulstinger Test Giant") {
        manaCost = "{5}{G}"
        typeLine = "Creature — Giant"
        power = 7
        toughness = 7
    }

    private val shot = card("Soulstinger Test Shot") {
        manaCost = "{G}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 3, creature)
        }
    }

    init {
        cardRegistry.register(giant)
        cardRegistry.register(shot)

        fun TestGame.minusCounters(id: EntityId) =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

        /** Casts Soulstinger onto itself, kills it with a three-counter test instant, and leaves the dies trigger's target pending. */
        fun TestGame.castAndKillSoulstinger() {
            castSpell(1, "Soulstinger").error shouldBe null
            resolveStack()
            val stinger = findPermanent("Soulstinger")!!
            if (state.pendingDecision is ChooseTargetsDecision) {
                selectTargets(listOf(stinger)).error shouldBe null
                resolveStack()
            }
            withClue("the enters trigger put two -1/-1 counters on Soulstinger itself") {
                minusCounters(stinger) shouldBe 2
            }

            castSpell(1, "Soulstinger Test Shot", stinger).error shouldBe null
            resolveStack()
            isInGraveyard(1, "Soulstinger") shouldBe true
            state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            selectTargets(listOf(findPermanent("Soulstinger Test Giant")!!)).error shouldBe null
            resolveStack()
        }

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Soulstinger")
            .withCardInHand(1, "Soulstinger Test Shot")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withLandsOnBattlefield(1, "Forest", 4) // any autopay split of {3}{B} leaves a Forest for the test instant
            .withCardOnBattlefield(2, "Soulstinger Test Giant")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("dying with five -1/-1 counters puts five on the target, read as last-known information") {
            val game = builder().build()
            game.castAndKillSoulstinger()

            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val target = game.findPermanent("Soulstinger Test Giant")!!
            game.minusCounters(target) shouldBe 5
            game.state.projectedState.getPower(target) shouldBe 2
        }

        test("declining the may puts no counters") {
            val game = builder().build()
            game.castAndKillSoulstinger()

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.minusCounters(game.findPermanent("Soulstinger Test Giant")!!) shouldBe 0
        }
    }
}
