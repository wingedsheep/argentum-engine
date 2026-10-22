package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flare of Faith
 * {1}{W}
 * Instant
 * Target creature gets +2/+2 until end of turn. If it's a Human, instead it gets +3/+3 and gains
 * indestructible until end of turn.
 *
 * "Instead" is an either/or checked on resolution, as on [com.wingedsheep.mtg.sets.definitions.one.cards.PorcelainZealot]:
 * `Effects.If(otherwise = …)` with the Human check reading the target's projected subtypes.
 */
val FlareOfFaith = card("Flare of Faith") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. If it's a Human, instead it gets +3/+3 " +
        "and gains indestructible until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
                creature,
            ),
            then = Effects.ModifyStats(3, 3, creature) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature),
            otherwise = Effects.ModifyStats(2, 2, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Justyna Dura"
        flavorText = "\"I fell to my knees. I called out to Sigarda. And then I saw my tormentors blaze " +
            "with blessed sunlight.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b4354c6-ea21-46c0-93c4-744dbacbfcf4.jpg?1783925659"
    }
}
