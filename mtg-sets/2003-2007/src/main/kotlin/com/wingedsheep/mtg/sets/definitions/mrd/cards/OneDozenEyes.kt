package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity

/**
 * One Dozen Eyes
 * {5}{G}
 * Sorcery
 * Choose one —
 * • Create a 5/5 green Beast creature token.
 * • Create five 1/1 green Insect creature tokens.
 * Entwine {G}{G}{G} (Choose both if you pay the entwine cost.)
 */
val OneDozenEyes = card("One Dozen Eyes") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Create a 5/5 green Beast creature token.\n" +
        "• Create five 1/1 green Insect creature tokens.\n" +
        "Entwine {G}{G}{G} (Choose both if you pay the entwine cost.)"

    entwine("{G}{G}{G}")

    spell {
        entwineModal {
            mode("Create a 5/5 green Beast creature token") {
                effect = Effects.CreateToken(
                    power = 5,
                    toughness = 5,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Beast"),
                    imageUri = "https://cards.scryfall.io/normal/front/4/4/44675b6a-ea5a-44f1-9f2c-3725cdfcc814.jpg?1783934353"
                )
            }
            mode("Create five 1/1 green Insect creature tokens") {
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Insect"),
                    count = 5,
                    imageUri = "https://cards.scryfall.io/normal/front/e/5/e5aa36ec-5f3a-405d-9a65-5a56a44dcee3.jpg?1783902801"
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3216148-f64e-434b-80b8-772f6eb831ca.jpg?1783944533"
    }
}
