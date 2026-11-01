package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * House Guildmage
 * {U}{B}
 * Creature — Human Wizard
 * 2/2
 * {1}{U}, {T}: Target creature doesn't untap during its controller's next untap step.
 * {2}{B}, {T}: Surveil 2.
 */
val HouseGuildmage = card("House Guildmage") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Wizard"
    oracleText = "{1}{U}, {T}: Target creature doesn't untap during its controller's next untap step.\n" +
        "{2}{B}, {T}: Surveil 2. (Look at the top two cards of your library, then put any number of them " +
        "into your graveyard and the rest on top of your library in any order.)"
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        effect = Patterns.Library.surveil(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45ee7bc8-d5cf-47b1-93c2-7e1d5f536c85.jpg?1783934131"
    }
}
