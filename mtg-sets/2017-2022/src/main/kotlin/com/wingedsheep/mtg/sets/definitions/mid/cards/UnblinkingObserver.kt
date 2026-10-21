package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Unblinking Observer (Innistrad: Midnight Hunt #82)
 * {1}{U} · Creature — Homunculus 2/1
 * {T}: Add {U}. Spend this mana only to pay a disturb cost or cast an instant or sorcery spell.
 *
 * The restriction is `AnyOf(DisturbCostOnly, InstantOrSorceryOnly)`: the mana pays a spell cast
 * for its disturb cost (CR 702.146a) or any instant or sorcery spell.
 */
val UnblinkingObserver = card("Unblinking Observer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homunculus"
    power = 2
    toughness = 1
    oracleText = "{T}: Add {U}. Spend this mana only to pay a disturb cost or cast an instant or sorcery spell."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.BLUE,
            restriction = ManaRestriction.AnyOf(
                listOf(ManaRestriction.DisturbCostOnly, ManaRestriction.InstantOrSorceryOnly)
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8148063c-a509-43ac-a291-c9410c30c2f2.jpg?1783925626"
    }
}
