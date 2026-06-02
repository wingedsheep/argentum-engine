package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Walking Desecration
 * {2}{B}
 * Creature — Zombie
 * 1/1
 * {B}, {T}: Creatures of the creature type of your choice attack this turn if able.
 */
val WalkingDesecration = card("Walking Desecration") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "{B}, {T}: Creatures of the creature type of your choice attack this turn if able."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        effect = Effects.Composite(
            listOf(
                ChooseOptionEffect(
                    optionType = OptionType.CREATURE_TYPE,
                    storeAs = "chosenCreatureType"
                ),
                Effects.ForEachInGroup(
                    filter = GroupFilter.ChosenSubtypeCreatures("chosenCreatureType"),
                    effect = MarkMustAttackThisTurnEffect(
                        target = EffectTarget.Self
                    )
                )
            )
        )
        description = "Creatures of the creature type of your choice attack this turn if able"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Daren Bader"
        flavorText = "\"Such sacrilege turns blinding grief into blinding rage.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c39f3e91-571a-4990-b1e8-db2a5bac34af.jpg?1562941229"
    }
}
