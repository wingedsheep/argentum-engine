package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Desert Nomads
 * {2}{R}
 * Creature — Human Nomad
 * 2/2
 *
 * Desertwalk (CR 702.13 landwalk variant — engine-supported via the new [Keyword.DESERTWALK]
 * wired into [com.wingedsheep.engine.mechanics.combat.rules.LandwalkRule], keyed off
 * [com.wingedsheep.sdk.core.Subtype.DESERT]).
 *
 * The Desert damage-prevention clause is a continuous [PreventDamage] replacement (CR 615):
 *  - recipient filter = [RecipientFilter.Self] (only Desert Nomads itself),
 *  - source filter = any Desert (a permanent with the Desert land subtype).
 */
val DesertNomads = card("Desert Nomads") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Nomad"
    power = 2
    toughness = 2
    oracleText = "Desertwalk (This creature can't be blocked as long as defending player " +
        "controls a Desert.)\n" +
        "Prevent all damage that would be dealt to this creature by Deserts."

    keywords(Keyword.DESERTWALK)

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Self,
                source = SourceFilter.Matching(GameObjectFilter.Land.withSubtype("Desert"))
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e46d0c10-ec09-48ba-9e93-1392dca8111a.jpg?1562937684"
    }
}
