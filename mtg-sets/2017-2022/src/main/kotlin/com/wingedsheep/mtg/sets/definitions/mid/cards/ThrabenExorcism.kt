package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thraben Exorcism (Innistrad: Midnight Hunt #39)
 * {1}{W} · Instant
 * Exile target Spirit, creature with disturb, or enchantment.
 *
 * "Creature with disturb" reads the DISTURB keyword the card builder derives from a front face's
 * `disturb(...)` ability. A disturb card on the battlefield back face up has only the back face's
 * characteristics, so it lacks disturb (per the 2021-09-24 ruling) — though every printed disturb
 * back face is a Spirit or an Aura and so is a legal target anyway.
 */
val ThrabenExorcism = card("Thraben Exorcism") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target Spirit, creature with disturb, or enchantment."

    spell {
        val t = target(
            TargetFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.SPIRIT)
                    .or(GameObjectFilter.Creature.withKeyword(Keyword.DISTURB))
                    .or(GameObjectFilter.Enchantment)
            )
        )
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Matt Stewart"
        flavorText = "Chandra, Teferi, and Arlinn sought clues to the Moonsilver Key's whereabouts " +
            "while Kaya dispatched the spirits obstructing their path."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0700298-30d2-45a6-bd64-4fd7fa4d7d30.jpg?1783925649"
        ruling(
            "2021-09-24",
            "A creature with disturb is one that has the disturb keyword ability in its text box. " +
                "Notably, permanents that were cast using the disturb ability rarely end up having " +
                "disturb on the battlefield. Barring some very weird circumstances with copy effects, " +
                "if a card's front face has disturb but its back face is on the battlefield, that " +
                "permanent doesn't have disturb."
        )
    }
}
