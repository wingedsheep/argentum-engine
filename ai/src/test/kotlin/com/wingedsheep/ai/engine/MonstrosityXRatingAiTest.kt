package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Monstrosity X with a becomes-monstrous payoff (Hydra Broodmaster's shape, CR 701.37c): the AI
 * reads the monstrous designation as needing no intent, prices the X/X tokens the trigger makes
 * (their printed 0/0 is a placeholder for the dynamic P/T) and the X +1/+1 counters the
 * monstrosity puts on the source, and so drafts it over the same body without the abilities.
 */
class MonstrosityXRatingAiTest : FunSpec({
    fun hydra(name: String, withAbilities: Boolean) = card(name) {
        manaCost = "{4}{G}{G}"
        typeLine = "Creature — Hydra"
        power = 7
        toughness = 7
        if (withAbilities) {
            activatedAbility {
                cost = Costs.Mana("{X}{X}{G}")
                effect = Effects.Monstrosity(DynamicAmounts.xValue())
            }
            triggeredAbility {
                trigger = Triggers.self.becomesMonstrous()
                effect = Effects.CreateToken(
                    count = DynamicAmounts.xValue(),
                    power = 0,
                    toughness = 0,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Hydra"),
                    dynamicPower = DynamicAmounts.xValue(),
                    dynamicToughness = DynamicAmounts.xValue(),
                )
            }
        }
    }
    fun CardDefinition.summary() = CardSummary(
        name = name, manaCost = manaCost.toString(), typeLine = typeLine.toString(),
        rarity = metadata.rarity.name, oracleText = oracleText, setCode = "ZZZ",
    )
    val broodmaster = hydra("Monstrosity X broodmaster probe", withAbilities = true)
    val vanilla = hydra("Vanilla seven-seven probe", withAbilities = false)

    test("monstrosity X and its becomes-monstrous trigger read fully") {
        AiReadingAudit.read(broodmaster).readFully shouldBe true
    }

    test("the X/X tokens and the X counters each add to the heuristic rating") {
        val terms = LimitedCardRater.explain(broodmaster).heuristic.associate { it.part to it.value }
        terms.getValue("Triggered ability 1") shouldBeGreaterThan 0.0
        terms.getValue("Activated ability 1") shouldBeGreaterThan 0.0
    }

    test("drafts the monstrosity hydra over the same vanilla body") {
        LimitedCardRater.explain(broodmaster).heuristicRating shouldBeGreaterThan
            LimitedCardRater.explain(vanilla).heuristicRating
        val registry = CardRegistry().apply { register(listOf(vanilla, broodmaster)) }
        val bot = EngineAiPlayerController(registry, EntityId.of("bot"), { null })
        bot.chooseDraftPick(listOf(vanilla.summary(), broodmaster.summary()), emptyList(), 1, 1)
            .shouldContainExactly(listOf(broodmaster.name))
    }
})
