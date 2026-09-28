package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Oculus Whelp — March of the Machine #69.
 * {3}{U} · Creature — Phyrexian Dragon · 3/2
 *
 * Flying
 * As long as you control a transformed permanent, this creature has "When this creature dies,
 * draw a card."
 *
 * A [ConditionalStaticAbility] around a self-scoped [GrantTriggeredAbility]. The granted ability is
 * a leaves-the-battlefield trigger, so whether the Whelp had it is decided by looking back to the
 * moment before it died (CR 603.10a): the engine freezes the condition onto the exit snapshot
 * against the pre-event state, so a transformed permanent dying alongside it still counts (the
 * printed ruling).
 */
val OculusWhelp = card("Oculus Whelp") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Dragon"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "As long as you control a transformed permanent, this creature has \"When this creature dies, draw a card.\""

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.dies()
                    effect = Effects.DrawCards(1)
                    description = "When this creature dies, draw a card."
                },
                filter = GroupFilter.source(),
            ),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.transformed()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Donato Giancola"
        flavorText = "Even before Phyrexia's forces arrived on Tarkir, Silumgar's draconic brood had a " +
            "discerning eye for valuables."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58b35399-79af-49fa-989e-867a85320cc2.jpg?1783917031"
        ruling("2023-04-14", "A \"transformed permanent\" is a double-faced permanent with its back face up. Notably, modal double-faced permanents and melded permanents are never transformed permanents, no matter which faces are up.")
        ruling("2023-04-14", "If Oculus Whelp dies at the same time as your transformed permanents, its ability will trigger, and you'll draw a card. (This wouldn't be true if the ability were worded differently, in case you were wondering.)")
    }
}
