package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Warden of the Beyond
 * {2}{W}
 * Creature — Human Wizard
 * 2/2
 * Vigilance
 * This creature gets +2/+2 as long as an opponent owns a card in exile.
 *
 * A [ConditionalStaticAbility] self-buff gated by `Exists(Player.EachOpponent, Zone.EXILE, Any)`.
 * Exile is keyed per player and a card always goes to its *owner's* exile zone, so scoping the zone
 * to the opponents reads ownership — not who exiled the card. Being a boolean gate, the bonus is a
 * single +2/+2 however many opponents own however many exiled cards (the 2014-07-18 ruling).
 */
val WardenOfTheBeyond = card("Warden of the Beyond") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "This creature gets +2/+2 as long as an opponent owns a card in exile."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Exists(Player.EachOpponent, Zone.EXILE, GameObjectFilter.Any),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Raymond Swanland"
        flavorText = "He draws strength from a vast source few mortals can fathom.\nDesigned by Isaiah Cartwright"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0a7406c-8c49-4ef6-a349-902833ea8ec5.jpg?1783939196"
        ruling("2014-07-18", "Warden of the Beyond can't get more than +2/+2 from its last ability, no matter how many opponents own a card in exile or how many exiled cards they own.")
    }
}
