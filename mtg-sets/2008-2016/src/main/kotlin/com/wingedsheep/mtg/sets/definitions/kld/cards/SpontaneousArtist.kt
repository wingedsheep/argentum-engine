package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spontaneous Artist — Kaladesh #134
 * {3}{R} · Creature — Human Rogue · 3/3
 *
 * When this creature enters, you get {E} (an energy counter).
 * Pay {E}: Target creature gains haste until end of turn.
 */
val SpontaneousArtist = card("Spontaneous Artist") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you get {E} (an energy counter).\n" +
        "Pay {E}: Target creature gains haste until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 1)
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.HASTE, target = t)
        description = "Pay {E}: Target creature gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8bbeb22-5e50-4b90-b756-9d420f9cfe7f.jpg?1783937188"
    }
}
