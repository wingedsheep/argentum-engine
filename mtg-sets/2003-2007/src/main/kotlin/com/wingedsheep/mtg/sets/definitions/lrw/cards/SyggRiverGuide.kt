package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sygg, River Guide
 * {W}{U}
 * Legendary Creature — Merfolk Wizard
 * 2/2
 * Islandwalk
 * {1}{W}: Target Merfolk you control gains protection from the color of your choice until end of turn.
 *
 * The colour is chosen on resolution (Thornscape Master's shape): [Effects.ChooseColorThen] wraps
 * [Effects.GrantProtectionFromChosenColor], so the choice is made after targeting rather than at
 * activation. Sygg is a Merfolk itself, so it can target itself. "Target Merfolk" is any Merfolk
 * permanent, not only a creature — a kindred Merfolk is one too.
 */
val SyggRiverGuide = card("Sygg, River Guide") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "Islandwalk (This creature can't be blocked as long as defending player controls " +
        "an Island.)\n" +
        "{1}{W}: Target Merfolk you control gains protection from the color of your choice until " +
        "end of turn."

    keywords(Keyword.ISLANDWALK)

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val t = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK).youControl()))
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(t))
        description = "Target Merfolk you control gains protection from the color of your choice until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "251"
        artist = "Larry MacDougall"
        flavorText = "\"If there's a place worth going, the Merrow Lanes already do. And if there's a route worth taking, yours truly already has.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb1aefaf-1b96-4c08-a73e-98e401655965.jpg?1783942852"
    }
}
