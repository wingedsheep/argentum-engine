package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Jade-Cast Sentinel — Tarkir: Dragonstorm #243
 * {4} · Artifact Creature — Ape Snake · 1/5
 *
 * Reach
 * {2}, {T}: Put target card from a graveyard on the bottom of its owner's library.
 *
 * Reach is a keyword helper. The graveyard-to-library-bottom activated ability mirrors
 * Chrome Companion (EOE): a {2},{T} cost moving a targeted graveyard card to the bottom of
 * its owner's library via [MoveToZoneEffect] with [ZonePlacement.Bottom].
 */
val JadeCastSentinel = card("Jade-Cast Sentinel") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Ape Snake"
    power = 1
    toughness = 5
    oracleText = "Reach\n" +
        "{2}, {T}: Put target card from a graveyard on the bottom of its owner's library."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val cardInGraveyard = target(
            "target card from a graveyard",
            TargetObject(filter = TargetFilter.CardInGraveyard)
        )
        effect = Effects.Move(
            target = cardInGraveyard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
        description = "Put target card from a graveyard on the bottom of its owner's library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "David Astruga"
        flavorText = "Set in motion by artificers long forgotten, it remained loyal through the " +
            "ages to the occupants of Kheru City."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/516ce5fa-bd00-429b-ba22-b38c7dd9306c.jpg?1743204961"
    }
}
