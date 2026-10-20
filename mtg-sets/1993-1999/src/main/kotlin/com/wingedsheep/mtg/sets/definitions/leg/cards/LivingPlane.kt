package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Living Plane — Legends #193
 * {2}{G}{G} · World Enchantment
 *
 * All lands are 1/1 creatures that are still lands.
 *
 * Living Lands' group animation widened to every land on the battlefield (either player's):
 * each gains the creature type and base P/T 1/1, keeping its land types and colourlessness.
 */
val LivingPlane = card("Living Plane") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "World Enchantment"
    oracleText = "All lands are 1/1 creatures that are still lands."

    staticAbility {
        ability = AnimateLandGroup(
            filter = GroupFilter.AllLands,
            power = 1,
            toughness = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Bryon Wackwitz"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/0341da27-3d77-4959-b7fa-5929b2cc7141.jpg?1783948046"
        ruling(
            "2009-10-01",
            "A noncreature permanent that turns into a creature is subject to the \"summoning sickness\" rule: " +
                "It can only attack, and its {T} abilities can only be activated, if its controller has " +
                "continuously controlled that permanent since the beginning of their most recent turn."
        )
        ruling(
            "2008-10-01",
            "This has the supertype world. When a world permanent enters, any world permanents that were " +
                "already on the battlefield are put into their owners' graveyards. This is a state-based " +
                "action called the \"world rule.\" The new world permanent stays on the battlefield. If two " +
                "world permanents enter at the same time, they're both put into their owners' graveyards."
        )
    }
}
