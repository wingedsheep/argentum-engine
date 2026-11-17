package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Marang River Prowler
 * {2}{U}
 * Creature — Human Rogue
 * 2/1
 * This creature can't block and can't be blocked.
 * You may cast this card from your graveyard as long as you control a black or green permanent.
 *
 * The graveyard permission is gated on the projected colours of the permanents you control and is
 * checked as you begin casting; normal timing and the {2}{U} cost still apply.
 */
val MarangRiverProwler = card("Marang River Prowler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "This creature can't block and can't be blocked.\n" +
        "You may cast this card from your graveyard as long as you control a black or green permanent."

    staticAbility {
        ability = CantBlock()
    }

    staticAbility {
        ability = CantBeBlocked()
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.BLACK, Color.GREEN)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Yefim Kligerman"
        flavorText = "The currents of the Marang wash away both tracks and blood."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bc8174c-da93-414e-b89c-c588a2c4ed0c.jpg?1783938707"
        ruling(
            "2018-12-07",
            "You must follow the normal timing permissions and restrictions and pay its cost to cast " +
                "Marang River Prowler from your graveyard."
        )
        ruling(
            "2018-12-07",
            "Marang River Prowler checks if you control a black or green permanent only as you begin to cast it. " +
                "Once you begin to cast Marang River Prowler from your graveyard, it doesn't matter what happens " +
                "to the black or green permanents you control."
        )
    }
}
