package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Thunderous Velocipede
 * {1}{G}{G}
 * Artifact — Vehicle
 * 5/5
 * Trample
 * Each other Vehicle and creature you control enters with an additional +1/+1 counter on it if its
 * mana value is 4 or less. Otherwise, it enters with three additional +1/+1 counters on it.
 * Crew 3
 *
 * The "if … otherwise …" split is two [EntersWithCounters] replacements whose `appliesTo` filters
 * partition the affected set by mana value — `manaValueAtMost(4)` → 1 counter, `manaValueAtLeast(5)`
 * → 3 counters. `EntersWithCounters.condition` is a *global* condition (Llanowar Elite's kicked
 * check), not one that can read the entering permanent, so the mana-value test has to live in the
 * filter, which the engine evaluates against the entering entity in `matchesEnterFilter`.
 *
 * `otherOnly = true` on both is the "each **other**" clause (Gev, Scaled Scorch / Metallic Mimic):
 * the Velocipede's own entry path skips them, so it never counters itself.
 *
 * Tokens and other permanents with no mana cost have mana value 0, so they land in the
 * `manaValueAtMost(4)` bucket and enter with one additional counter — correct per CR 202.3b.
 */
val ThunderousVelocipede = card("Thunderous Velocipede") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Each other Vehicle and creature you control enters with an additional +1/+1 counter on it " +
        "if its mana value is 4 or less. Otherwise, it enters with three additional +1/+1 counters " +
        "on it.\n" +
        "Crew 3"

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 1,
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.CreatureOrVehicle.youControl().manaValueAtMost(4),
                to = Zone.BATTLEFIELD
            )
        )
    )

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 3,
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.CreatureOrVehicle.youControl().manaValueAtLeast(5),
                to = Zone.BATTLEFIELD
            )
        )
    )

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "183"
        artist = "Adrián Rodríguez Pérez"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98a79557-8ed6-4d9a-b4e1-cece05664984.jpg?1783907865"
    }
}
