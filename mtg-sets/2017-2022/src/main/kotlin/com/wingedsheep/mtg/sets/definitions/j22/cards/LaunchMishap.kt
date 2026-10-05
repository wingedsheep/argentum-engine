package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Launch Mishap
 * {2}{U}
 * Instant
 *
 * Counter target creature or planeswalker spell. Create a 1/1 colorless Thopter artifact creature
 * token with flying.
 *
 * Syphon Essence's shape with a Thopter in place of the Blood token: the token goes to the caster,
 * and — the spell having a single target — nothing happens if that target is gone on resolution.
 */
val LaunchMishap = card("Launch Mishap") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or planeswalker spell. Create a 1/1 colorless Thopter " +
        "artifact creature token with flying."

    spell {
        target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.STACK))
        effect = Effects.CounterSpell() then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/3/5/3597cf76-f0db-4d2c-b01c-d518d7c7a764.jpg?1789754755",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Kim Sokol"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abc561d3-9e52-41fd-8311-60cc9dfffdd1.jpg?1783919193"
    }
}
