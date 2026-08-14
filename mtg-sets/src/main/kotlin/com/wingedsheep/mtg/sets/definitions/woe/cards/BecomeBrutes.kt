package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Become Brutes
 * {1}{R}
 * Sorcery
 *
 * One or two target creatures each gain haste until end of turn. For each of those creatures,
 * create a Monster Role token attached to it.
 *
 * A single multi-target requirement enforces the printed one-or-two range and distinct targets.
 * Each still-legal target receives both parts in order; an illegal target is omitted while the
 * spell continues to resolve for any other legal target.
 */
val BecomeBrutes = card("Become Brutes") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "One or two target creatures each gain haste until end of turn. For each of those " +
        "creatures, create a Monster Role token attached to it. (If you control another Role on it, " +
        "put that one into the graveyard. Enchanted creature gets +1/+1 and has trample.)"

    spell {
        target = TargetCreature(
            count = 2,
            minCount = 1,
            filter = TargetFilter(GameObjectFilter.Creature),
        )
        effect = ForEachTargetEffect(
            effects = listOf(
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
                Effects.CreateRoleToken("Monster Role", EffectTarget.ContextTarget(0)),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "317"
        artist = "Julia Griffin"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a154dadc-be5b-4ad6-9946-bdc54c251bff.jpg?1783915039"
    }
}
