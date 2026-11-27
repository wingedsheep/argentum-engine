package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stonewright — Avacyn Restored #157 (canonical printing)
 * {R} · Creature — Human Shaman · 1/1
 *
 * Soulbond
 * As long as Stonewright is paired with another creature, each of those creatures has
 * "{R}: This creature gets +1/+0 until end of turn."
 *
 * [DeadeyeNavigator]'s shape with a firebreathing pump: [GrantActivatedAbility] over
 * `GroupFilter.soulbondPair()` — both halves while paired, nobody while unpaired. The granted
 * ability's [EffectTarget.Self] is the creature it was activated on, so each half pumps itself.
 */
val Stonewright = card("Stonewright") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as Stonewright is paired with another creature, each of those creatures has " +
        "\"{R}: This creature gets +1/+0 until end of turn.\""
    power = 1
    toughness = 1

    soulbond()

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{R}")
                effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
                description = "{R}: This creature gets +1/+0 until end of turn."
            },
            filter = GroupFilter.soulbondPair()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9564d79d-5f4d-4192-94ee-5e5998011266.jpg?1783940676"
    }
}
