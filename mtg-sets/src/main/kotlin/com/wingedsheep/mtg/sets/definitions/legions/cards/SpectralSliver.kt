package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spectral Sliver
 * {2}{B}
 * Creature — Sliver Spirit
 * 2/2
 * All Sliver creatures have "{2}: This creature gets +1/+1 until end of turn."
 */
val SpectralSliver = card("Spectral Sliver") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Sliver Spirit"
    power = 2
    toughness = 2
    oracleText = "All Sliver creatures have \"{2}: This creature gets +1/+1 until end of turn.\""

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    staticAbility {
        ability = GrantActivatedAbilityToCreatureGroup(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Mana("{2}"),
                effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Pete Venters"
        flavorText = "\"Sure, I've seen agents of the Cabal here and there. Well, not here, and certainly not in any of the sliver labs. Oh dear, I've said too much.\" —Apprentice researcher"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bec97e3c-7b75-4abb-a50e-86bc8cc3bf06.jpg?1562933371"
    }
}
