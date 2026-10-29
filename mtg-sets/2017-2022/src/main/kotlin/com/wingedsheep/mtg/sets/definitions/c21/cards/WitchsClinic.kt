package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Witch's Clinic — Commander 2021 #81
 * Land
 * {T}: Add {C}.
 * {2}, {T}: Target commander gains lifelink until end of turn.
 *
 * "Target commander" is any player's commander on the battlefield (CR 903.3) — a permanent
 * carrying `StatePredicate.IsCommander` via `.commander()`.
 */
val WitchsClinic = card("Witch's Clinic") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{2}, {T}: Target commander gains lifelink until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target(TargetFilter(GameObjectFilter.Permanent.commander()))
        effect = Effects.GrantKeyword(Keyword.LIFELINK, t)
        description = "{2}, {T}: Target commander gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Piotr Dura"
        flavorText = "Students from other colleges are invited to visit Witherbloom's medical facilities. " +
            "Not many do."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe17b08f-4790-4885-8b2b-b22f00b52d4e.jpg?1783927582"
    }
}
