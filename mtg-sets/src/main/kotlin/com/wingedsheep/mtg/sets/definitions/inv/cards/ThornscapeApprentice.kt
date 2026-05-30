package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect

/**
 * Thornscape Apprentice
 * {G}
 * Creature — Human Wizard
 * 1/1
 * {R}, {T}: Target creature gains first strike until end of turn.
 * {W}, {T}: Tap target creature.
 */
val ThornscapeApprentice = card("Thornscape Apprentice") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{R}, {T}: Target creature gains first strike until end of turn.\n" +
        "{W}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = TapUntapEffect(target = t, tap = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/505da522-73a8-4232-ae1a-d3365f3e598f.jpg?1562911289"
    }
}
