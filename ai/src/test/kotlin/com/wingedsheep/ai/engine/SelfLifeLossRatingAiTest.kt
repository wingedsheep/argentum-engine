package com.wingedsheep.ai.engine

import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeLessThan

/**
 * An upkeep "you lose 1 life" (Herald of Torment) is the card's price: the rater must not score it
 * like a drain on an opponent, so the same body with the self-loss rates below the one without it.
 */
class SelfLifeLossRatingAiTest : FunSpec({
    fun upkeepLoser(name: String, target: EffectTarget) = card(name) {
        manaCost = "{1}{B}{B}"
        typeLine = "Creature — Demon"
        power = 3
        toughness = 3
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.LoseLife(1, target)
        }
    }
    fun CardDefinition.summary() = CardSummary(
        name = name, manaCost = manaCost.toString(), typeLine = typeLine.toString(),
        rarity = metadata.rarity.name, oracleText = oracleText, setCode = "ZZZ",
    )
    val paysLife = upkeepLoser("Self life loss rating probe", EffectTarget.Controller)
    val drains = upkeepLoser("Opponent life loss rating probe", EffectTarget.PlayerRef(Player.EachOpponent))

    test("drafts the upkeep drain over the same body that costs its controller life") {
        LimitedCardRater.explain(paysLife).heuristicRating shouldBeLessThan
            LimitedCardRater.explain(drains).heuristicRating
        val registry = CardRegistry().apply { register(listOf(paysLife, drains)) }
        val bot = EngineAiPlayerController(registry, EntityId.of("bot"), { null })
        bot.chooseDraftPick(listOf(paysLife.summary(), drains.summary()), emptyList(), 1, 1)
            .shouldContainExactly(listOf(drains.name))
    }
})
