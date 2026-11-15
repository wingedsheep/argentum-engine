package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mistfire Adept
 * {3}{U}
 * Creature — Human Monk
 * 3/3
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * Whenever you cast a noncreature spell, target creature gains flying until end of turn.
 *
 * Prowess is `prowess()` (keyword plus its +1/+1 trigger), not `keywords(Keyword.PROWESS)`, which
 * would only display the keyword. Each noncreature spell triggers both abilities separately.
 */
val MistfireAdept = card("Mistfire Adept") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Monk"
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Whenever you cast a noncreature spell, target creature gains flying until end of turn."
    power = 3
    toughness = 3

    prowess()

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Clint Cearley"
        flavorText = "\"The quieter the skies, the calmer the mind.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acdb2388-9ec4-496a-a63e-6b3f33a608a7.jpg"
    }
}
