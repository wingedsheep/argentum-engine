package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Expendable Lackey
 * {U}
 * Creature — Human Citizen
 * 1/1
 *
 * {1}{U}, Exile this card from your graveyard: Create a 1/1 blue Fish creature token with
 * "This token can't be blocked." Activate only as a sorcery.
 */
val ExpendableLackey = card("Expendable Lackey") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Citizen"
    power = 1
    toughness = 1
    oracleText = "{1}{U}, Exile this card from your graveyard: Create a 1/1 blue Fish creature token " +
        "with \"This token can't be blocked.\" Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.ExileSelf)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Fish"),
            staticAbilities = listOf(CantBeBlocked(GroupFilter.source())),
            imageUri = "https://cards.scryfall.io/normal/front/3/b/3b2e726b-f124-43f7-87e4-bb06bbe8e3d7.jpg?1783922964",
        )
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9515329a-0738-4b23-a326-ff417f47da77.jpg?1783923145"
    }
}
