package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hanweir Battlements
 * Land
 * {T}: Add {C}.
 * {R}, {T}: Target creature gains haste until end of turn.
 * {3}{R}{R}, {T}: If you both own and control this land and a creature named Hanweir Garrison,
 * exile them, then meld them into Hanweir, the Writhing Township.
 *
 * The meld ability has no timing restriction; with no Garrison (or one you don't own) it
 * resolves and does nothing (CR 701.42).
 */
val HanweirBattlements = card("Hanweir Battlements") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{R}, {T}: Target creature gains haste until end of turn.\n" +
        "{3}{R}{R}, {T}: If you both own and control this land and a creature named Hanweir " +
        "Garrison, exile them, then meld them into Hanweir, the Writhing Township."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.HASTE, creature)
        description = "{R}, {T}: Target creature gains haste until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}{R}"), Costs.Tap)
        effect = Effects.Meld(Filters.Creature.named("Hanweir Garrison"), into = "Hanweir, the Writhing Township")
        description = "{3}{R}{R}, {T}: If you both own and control this land and a creature named Hanweir " +
            "Garrison, exile them, then meld them into Hanweir, the Writhing Township."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d743ad6-6ca2-409a-9773-581cc195dbf2.jpg?1783937421"
    }
}
