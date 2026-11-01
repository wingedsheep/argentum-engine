package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.evaluation.GrowthPotential
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [com.wingedsheep.ai.engine.evaluation.CreatureValuation.growthPotential]: a creature with a
 * recurring +1/+1-counter trigger is worth the counters its next three triggers put down, found by
 * resolving the trigger's own effect on a copy of the state ([GrowthPotential]).
 */
class GrowthPotentialAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val growth by lazy { intents.growth!! }

    private fun TestGame.expectedCounters(cardName: String): Double {
        val id = findPermanent(cardName)!!
        val trigger = growth.triggersOf(cardName).single()
        return trigger.likelihood * growth.expectedCounters(state, id, player1Id, trigger.effect)
    }

    /** The permanent [profile]'s AI aims its removal at, or what it did instead. */
    private fun TestGame.removalTarget(profile: AiProfile): String {
        val action = AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)
        val cast = action as? CastSpell ?: return "no cast: $action"
        val id = (cast.targets.single() as ChosenTarget.Permanent).entityId
        return state.getEntity(id)?.get<CardComponent>()?.name ?: "?"
    }

    init {
        test("only recurring, untargeted +1/+1-counter triggers are read as growth") {
            growth.triggersOf("Ouroboroid") shouldHaveSize 1
            growth.triggersOf("Ouroboroid").single().likelihood shouldBe 1.0
            growth.triggersOf("Victory's Envoy") shouldHaveSize 1
            // "Whenever this creature attacks" fires only on turns it attacks.
            growth.triggersOf("Herald of War").single().likelihood shouldBe GrowthPotential.ATTACK_LIKELIHOOD
            // A vanilla body, and a static-ability grower whose size the projection already reads.
            growth.triggersOf("Grizzly Bears").shouldBeEmpty()
            growth.triggersOf("Moon-Vigil Adherents").shouldBeEmpty()
        }

        // Ouroboroid's X is its own power, so it compounds: one counter on everything next combat,
        // two on everything the combat after, four the one after that.
        test("the simulation counts compounding counters across the team") {
            val alone = scenario().withPlayers()
                .withCardOnBattlefield(1, "Ouroboroid")
                .build()
            alone.expectedCounters("Ouroboroid") shouldBe
                (GrowthPotential.NEXT_TRIGGER_WEIGHT * 1 + GrowthPotential.SECOND_TRIGGER_WEIGHT * 2 +
                    GrowthPotential.THIRD_TRIGGER_WEIGHT * 4 plusOrMinus 1e-9)

            val team = scenario().withPlayers()
                .withCardOnBattlefield(1, "Ouroboroid")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
            team.expectedCounters("Ouroboroid") shouldBe
                (GrowthPotential.NEXT_TRIGGER_WEIGHT * 3 + GrowthPotential.SECOND_TRIGGER_WEIGHT * 6 +
                    GrowthPotential.THIRD_TRIGGER_WEIGHT * 12 plusOrMinus 1e-9)
        }

        test("a lord that grows only the others is worth nothing alone") {
            val alone = scenario().withPlayers()
                .withCardOnBattlefield(1, "Victory's Envoy")
                .build()
            alone.expectedCounters("Victory's Envoy") shouldBe 0.0

            val team = scenario().withPlayers()
                .withCardOnBattlefield(1, "Victory's Envoy")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
            team.expectedCounters("Victory's Envoy") shouldBe
                (GrowthPotential.NEXT_TRIGGER_WEIGHT * 2 + GrowthPotential.SECOND_TRIGGER_WEIGHT * 2 +
                    GrowthPotential.THIRD_TRIGGER_WEIGHT * 2 plusOrMinus 1e-9)
        }

        test("the board prices a fresh Ouroboroid above its 1/3 body, and only with the flag on") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Ouroboroid")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()
            fun score(profile: AiProfile) = BoardPresence.score(
                game.state, game.state.projectedState, game.player1Id, intents,
                creatureValuation = profile.creatureValuation,
            )
            score(AiProfile.LIVE) shouldBeGreaterThan score(AiProfile.LIVE_WITHOUT_GROWTH)
        }

        // Eviscerate against an Ouroboroid beside a Hill Giant. On bodies alone the 3/3 is the
        // bigger threat; priced by what it keeps adding, the 1/3 is the card that wins the game.
        // The removal-patience bar is off on both sides so this pins the target, not the timing.
        test("removal aims at the growth engine, not the bigger body beside it") {
            val game = scenario().withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInHand(1, "Eviscerate")
                .withCardOnBattlefield(2, "Ouroboroid")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val base = AiProfile.LIVE.copy(holdRemovalForBetterTargets = false)
            val without = game.removalTarget(base.copy(creatureValuation = AiProfile.LIVE_WITHOUT_GROWTH.creatureValuation))
            val with = game.removalTarget(base)
            withClue("with growth: $with; without: $without") {
                without shouldNotBe "Ouroboroid"
                with shouldBe "Ouroboroid"
            }
        }
    }
}
