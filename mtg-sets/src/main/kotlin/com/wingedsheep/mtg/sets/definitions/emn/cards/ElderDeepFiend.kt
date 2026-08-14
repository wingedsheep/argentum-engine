package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Elder Deep-Fiend
 * {8}
 * Creature — Eldrazi Octopus
 * 5/6
 *
 * Flash
 * Emerge {5}{U}{U}
 * When you cast this spell, tap up to four target permanents.
 *
 * Implementation notes:
 * - Emerge is the engine keyword (CR 702.119) via the `emerge(cost)` helper. Emerge grants no
 *   timing permission of its own, so the flash on this card is what makes the emerge cast legal at
 *   instant speed — the enumerator gates emerge on the spell's normal timing.
 * - "Tap up to four target permanents" is one requirement with `count = 4, optional = true`
 *   (a zero-target cast is legal), resolved by `TapEachTarget`.
 */
val ElderDeepFiend = card("Elder Deep-Fiend") {
    manaCost = "{8}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Octopus"
    power = 5
    toughness = 6
    oracleText = "Flash\nEmerge {5}{U}{U} (You may cast this spell by sacrificing a creature and " +
        "paying the emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, tap up to four target permanents."

    keywords(Keyword.FLASH)

    emerge("{5}{U}{U}")

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        target(
            "up to four target permanents",
            TargetPermanent(count = 4, optional = true, filter = TargetFilter.Permanent),
        )
        effect = Effects.TapEachTarget()
        description = "When you cast this spell, tap up to four target permanents."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c2789fb-a263-4207-8a56-4eeb015a024c.jpg?1783937526"
    }
}
