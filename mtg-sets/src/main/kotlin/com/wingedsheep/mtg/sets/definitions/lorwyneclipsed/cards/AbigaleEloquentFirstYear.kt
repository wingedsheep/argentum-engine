package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Abigale, Eloquent First-Year
 * {W/B}{W/B}
 * Legendary Creature — Bird Bard
 * 1/1
 *
 * Flying, first strike, lifelink
 * When Abigale enters, up to one other target creature loses all abilities.
 * Put a flying counter, a first strike counter, and a lifelink counter on that creature.
 *
 * Per Wizards rulings (2025-11-17): the "loses all abilities" effect lasts indefinitely; it
 * doesn't expire during cleanup or when Abigale leaves the battlefield. The keyword counters
 * grant their respective abilities back via Rule 122.1b, so the targeted creature ends up with
 * exactly flying + first strike + lifelink. Any abilities gained after Abigale's trigger
 * resolves are kept.
 */
val AbigaleEloquentFirstYear = card("Abigale, Eloquent First-Year") {
    manaCost = "{W/B}{W/B}"
    typeLine = "Legendary Creature — Bird Bard"
    power = 1
    toughness = 1
    oracleText = "Flying, first strike, lifelink\n" +
        "When Abigale enters, up to one other target creature loses all abilities. " +
        "Put a flying counter, a first strike counter, and a lifelink counter on that creature."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one other target creature",
            TargetCreature(
                optional = true,
                filter = TargetFilter.Creature.copy(excludeSelf = true)
            )
        )
        effect = Effects.Composite(
            RemoveAllAbilitiesEffect(creature, Duration.Permanent),
            Effects.AddCounters(Counters.FLYING, 1, creature),
            Effects.AddCounters(Counters.FIRST_STRIKE, 1, creature),
            Effects.AddCounters(Counters.LIFELINK, 1, creature),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Mark Zug"
        flavorText = "The right poem lifts more than spirits."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf708169-a307-494b-b8d8-baae53b2e2f2.jpg?1767658678"
        ruling("2025-11-17", "If the affected creature gains an ability after Abigale's last ability resolves, it will keep that ability.")
        ruling("2025-11-17", "The effect of Abigale's last ability that causes the target creature to lose all abilities lasts indefinitely. It doesn't expire during the cleanup step or when Abigale leaves the battlefield.")
    }
}
