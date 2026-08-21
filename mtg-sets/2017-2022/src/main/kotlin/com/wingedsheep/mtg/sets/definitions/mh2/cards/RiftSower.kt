package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rift Sower — Modern Horizons 2 #170
 * {2}{G} · Creature — Elf Druid · 1 / 3
 *
 * {T}: Add one mana of any color.
 * Suspend 2—{G} (Rather than cast this card from your hand, you may pay {G} and exile it with two time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, you may cast it without paying its mana cost. It has haste.)
 *
 * The mana ability is the stock Birds of Paradise shape: [Effects.AddManaOfChoice] with every
 * default taken — all five colors, one mana, no restriction — which is precisely what "add one mana
 * of any color" means. `manaAbility = true` is the only flag set; the builder derives
 * `TimingRule.ManaAbility` from it (CR 605.1a: no target, produces mana, not a loyalty ability), so
 * writing the timing again would be the same fact twice.
 *
 * Suspend is the parameterized [KeywordAbility.Suspend] (CR 702.62) — cost first, time counters
 * second. The bare `Keyword.SUSPEND` is display-only and is derived from this by
 * `CardBuilder.build()`; the engine's `SuspendEnumerator` reads the keyword *ability*.
 */
val RiftSower = card("Rift Sower") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 3
    oracleText = "{T}: Add one mana of any color.\n" +
        "Suspend 2—{G} (Rather than cast this card from your hand, you may pay {G} and exile it with two time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, you may cast it without paying its mana cost. It has haste.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
    }

    keywordAbility(KeywordAbility.suspend("{G}", 2))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97dbd212-f1e8-429a-bf00-b2ea966d880e.jpg?1783926827"
    }
}
