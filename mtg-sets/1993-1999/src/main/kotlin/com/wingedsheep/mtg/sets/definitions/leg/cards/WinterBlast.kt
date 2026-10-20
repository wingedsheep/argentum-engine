package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Winter Blast — Legends #213
 * {X}{G} · Sorcery
 *
 * Tap X target creatures. Winter Blast deals 2 damage to each of those creatures with flying.
 *
 * "X target creatures" is exactly X. Flying is checked per target at resolution, not as a
 * targeting restriction, so the damage half is a per-target `If` over the same targets
 * (the Lost in the Maze shape). Already-tapped creatures are legal targets and still take
 * the damage if they fly.
 */
val WinterBlast = card("Winter Blast") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Tap X target creatures. Winter Blast deals 2 damage to each of those creatures with flying."

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
        effect = Effects.TapEachTarget() then
            Effects.ForEachTarget(
                Effects.If(
                    condition = Conditions.TargetMatchesFilter(
                        GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
                    ),
                    then = Effects.DealDamage(2, EffectTarget.ContextTarget(0))
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Kaja Foglio"
        flavorText = "\"Blow, winds, and crack your cheeks! rage! blow!\"\n—William Shakespeare, King Lear"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb846366-2105-4999-8af1-a11687f42e17.jpg?1783948043"
        ruling(
            "2007-09-16",
            "Winter Blast can target tapped creatures. If they have flying, the second part of the effect will still affect them."
        )
        ruling(
            "2007-09-16",
            "Checks if the creatures are Flying on resolution and not on announcement. It's not a targeting restriction."
        )
    }
}
