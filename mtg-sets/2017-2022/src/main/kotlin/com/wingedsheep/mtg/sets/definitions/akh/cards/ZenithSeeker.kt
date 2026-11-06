package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zenith Seeker — Amonkhet #77
 * {3}{U} · Creature — Bird Wizard · Uncommon
 * 2/2
 *
 * Flying
 * Whenever you cycle or discard a card, target creature gains flying until end of turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card, so
 * `Triggers.you.discards()` fires once per cycled card, per the ruling.
 */
val ZenithSeeker = card("Zenith Seeker") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever you cycle or discard a card, target creature gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.discards()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
        description = "Whenever you cycle or discard a card, target creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Jason Kang"
        flavorText = "Only from the sky can one fully understand the horrors of the wastes and the wonder of the Hekma's protection."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47b711b5-aeec-47c4-a718-6bf43e561944.jpg?1783936512"
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
    }
}
