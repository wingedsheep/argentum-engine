package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quarum Trench Gnomes
 * {3}{R}
 * Creature — Gnome
 * 1/1
 * {T}: If target Plains is tapped for mana, it produces colorless mana instead of white mana.
 * (This effect lasts indefinitely.)
 *
 * The rule is granted to the targeted Plains itself, forever ([Duration.Permanent]), so it outlives
 * the Gnomes and ends only when that Plains leaves the battlefield. Its filter is the land itself
 * rather than "a Plains": the effect is bound to that object, even if it later stops being a Plains.
 * `replaces = WHITE` leaves any other mana the land can make alone.
 */
val QuarumTrenchGnomes = card("Quarum Trench Gnomes") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Gnome"
    power = 1
    toughness = 1
    oracleText = "{T}: If target Plains is tapped for mana, it produces colorless mana instead of " +
        "white mana. (This effect lasts indefinitely.)"

    activatedAbility {
        cost = Costs.Tap
        val plains = target(TargetFilter(GameObjectFilter.Land.withSubtype(Subtype.PLAINS)))
        effect = Effects.GrantStaticAbility(
            ability = ReplaceLandManaColor(
                filter = GameObjectFilter.Land.sourceItself(),
                produces = ManaSymbol.Colorless,
                replaces = Color.WHITE,
            ),
            target = plains,
            duration = Duration.Permanent,
        )
        description = "{T}: If target Plains is tapped for mana, it produces colorless mana instead " +
            "of white mana."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Dan Frazier"
        flavorText = "\"O Great Captain, all is lost. They tunneled, they burrowed, they trenched. " +
            "They sapped the strength of our defenses.\"\n—Sorgus, *Chronicles of the Quarum Plains*"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c3b33bf-3074-406e-86f3-2a9843cf4862.jpg?1783948053"
    }
}
