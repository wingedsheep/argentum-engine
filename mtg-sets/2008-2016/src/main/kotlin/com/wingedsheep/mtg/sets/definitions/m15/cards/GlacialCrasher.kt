package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Glacial Crasher
 * {4}{U}{U}
 * Creature — Elemental
 * 5/5
 * Trample
 * This creature can't attack unless there is a Mountain on the battlefield.
 *
 * "There is a Mountain on the battlefield" is battlefield-wide, not defender-relative: the
 * restriction is `Exists(Player.Each, BATTLEFIELD, Land.withSubtype(Mountain))`, which sums
 * every player's permanents, so a Mountain any player controls (including your own) lets it
 * attack. The check is made only at declaration (CR 508.1c), so it stays attacking if the
 * Mountains leave afterwards.
 */
val GlacialCrasher = card("Glacial Crasher") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "This creature can't attack unless there is a Mountain on the battlefield."
    power = 5
    toughness = 5

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = CantAttackUnless(
            Exists(Player.Each, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8bf55bf-ee38-4e68-81ca-0fdb8af779ef.jpg?1783939193"
        ruling("2014-07-18", "It doesn't matter who controls the Mountain.")
        ruling("2014-07-18", "If the only Mountains on the battlefield leave the battlefield after Glacial Crasher attacks, it stays attacking.")
    }
}
