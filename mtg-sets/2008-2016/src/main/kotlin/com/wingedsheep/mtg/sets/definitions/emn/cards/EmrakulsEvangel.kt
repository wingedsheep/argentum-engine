package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Emrakul's Evangel (Eldritch Moon #156)
 * {2}{G}
 * Creature — Human Horror
 * 3/2
 *
 * {T}, Sacrifice this creature and any number of other non-Eldrazi creatures: Create a 3/2
 * colorless Eldrazi Horror creature token for each creature sacrificed this way.
 *
 * - The other creatures are a variable-count sacrifice cost (zero is legal, ruling 2016-07-13)
 *   that excludes the Evangel itself; the Evangel goes by its own `SacrificeSelf` cost, which
 *   isn't recorded among the "sacrificed this way" snapshots — hence the `+ 1` for the ruling's
 *   "counts itself".
 * - No Eldrazi Horror `imageUri` — the token art comes from Eldritch Moon's `temn` token printings.
 */
val EmrakulsEvangel = card("Emrakul's Evangel") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Horror"
    power = 3
    toughness = 2
    oracleText = "{T}, Sacrifice this creature and any number of other non-Eldrazi creatures: Create a 3/2 " +
        "colorless Eldrazi Horror creature token for each creature sacrificed this way."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf,
            Costs.SacrificePermanents(
                GameObjectFilter.Creature.notSubtype(Subtype.ELDRAZI),
                minCount = 0,
                excludeSelf = true
            )
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Horror"),
            count = DynamicAmounts.permanentsSacrificedThisWay() + 1
        )
        description = "{T}, Sacrifice this creature and any number of other non-Eldrazi creatures: Create a 3/2 " +
            "colorless Eldrazi Horror creature token for each creature sacrificed this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Jason Felix"
        flavorText = "\"In times like these, be particularly wary of those who claim they can save your soul.\"\n" +
            "—Alena, trapper of Kessig"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51c21822-972f-4c20-ac3b-80ba9af101c5.jpg?1783937449"
        ruling(
            "2016-07-13",
            "You may choose to sacrifice no other non-Eldrazi creatures this way. Emrakul's Evangel's ability " +
                "counts itself, so you'll get one Eldrazi Horror token."
        )
    }
}
