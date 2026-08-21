package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sanctum Weaver — Modern Horizons 2 #171
 * {1}{G} · Enchantment Creature — Dryad · 0 / 2
 *
 * {T}: Add X mana of any one color, where X is the number of enchantments you control.
 *
 * "Any *one* color" is [Effects.AddAnyColorMana]: a single color is chosen for the whole batch of
 * mana, not one color per mana — the same shape as Deathbloom Ritualist. X is counted on
 * resolution by [DynamicAmount.AggregateBattlefield] over the enchantments its controller has on
 * the battlefield (Serra's Sanctum's aggregate), which includes the Weaver itself: it is an
 * enchantment creature, so an unaccompanied Weaver still taps for one mana.
 *
 * `manaAbility = true` is the only switch needed — the builder derives both the `isManaAbility`
 * flag and the `ManaAbility` timing rule from it. This is a legal mana ability under CR 605.1a:
 * it has no target, it isn't a loyalty ability, and it could add mana to a pool.
 */
val SanctumWeaver = card("Sanctum Weaver") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Dryad"
    power = 0
    toughness = 2
    oracleText = "{T}: Add X mana of any one color, where X is the number of enchantments you control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Enchantment)
        )
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Kimonas Theodossiou"
        flavorText = "The arrival of spring's first flowers is a sacred occasion to Karametra's acolytes, who fill the Setessan woods with song in celebration."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d42e22d-f60e-40c5-b069-5e1708f3bebc.jpg?1783926826"
    }
}
