package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BudgetModalEffect
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Season of Gathering
 * {4}{G}{G}
 * Sorcery
 *
 * Choose up to five {P} worth of modes. You may choose the same mode more than once.
 * {P} — Put a +1/+1 counter on a creature you control. It gains vigilance and trample until end of turn.
 * {P}{P} — Choose artifact or enchantment. Destroy all permanents of the chosen type.
 * {P}{P}{P} — Draw cards equal to the greatest power among creatures you control.
 */
val SeasonOfGathering = card("Season of Gathering") {
    manaCost = "{4}{G}{G}"
    typeLine = "Sorcery"
    oracleText = "Choose up to five {P} worth of modes. You may choose the same mode more than once.\n" +
        "{P} — Put a +1/+1 counter on a creature you control. It gains vigilance and trample until end of turn.\n" +
        "{P}{P} — Choose artifact or enchantment. Destroy all permanents of the chosen type.\n" +
        "{P}{P}{P} — Draw cards equal to the greatest power among creatures you control."

    spell {
        effect = BudgetModalEffect(
            budget = 5,
            modes = listOf(
                // {P} — +1/+1 counter + vigilance + trample on a creature you control
                BudgetMode(
                    cost = 1,
                    effect = Effects.SelectTarget(Targets.CreatureYouControl, "chosenCreature")
                        .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.PipelineTarget("chosenCreature")))
                        .then(Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.PipelineTarget("chosenCreature")))
                        .then(Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.PipelineTarget("chosenCreature"))),
                    description = "Put a +1/+1 counter on a creature you control. It gains vigilance and trample until end of turn"
                ),
                // {P}{P} — Choose artifact or enchantment. Destroy all of that type.
                BudgetMode(
                    cost = 2,
                    effect = ModalEffect.chooseOne(
                        Mode.noTarget(
                            ForEachInGroupEffect(
                                filter = GroupFilter(GameObjectFilter.Artifact),
                                effect = Effects.Destroy(EffectTarget.Self),
                                simultaneous = true
                            ),
                            "Destroy all artifacts"
                        ),
                        Mode.noTarget(
                            ForEachInGroupEffect(
                                filter = GroupFilter(GameObjectFilter.Enchantment),
                                effect = Effects.Destroy(EffectTarget.Self),
                                simultaneous = true
                            ),
                            "Destroy all enchantments"
                        )
                    ),
                    description = "Choose artifact or enchantment. Destroy all permanents of the chosen type"
                ),
                // {P}{P}{P} — Draw cards equal to greatest power among creatures you control
                BudgetMode(
                    cost = 3,
                    effect = DrawCardsEffect(
                        count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
                        target = EffectTarget.Controller
                    ),
                    description = "Draw cards equal to the greatest power among creatures you control"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "A. M. Sartor"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71dd3c27-e0d5-434e-a0f3-4a95245e21c2.jpg?1721426924"
    }
}
