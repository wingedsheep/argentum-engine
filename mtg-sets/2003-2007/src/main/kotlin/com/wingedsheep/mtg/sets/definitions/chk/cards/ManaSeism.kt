package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mana Seism {1}{R}
 * Sorcery
 *
 * Sacrifice any number of lands, then add that much {C}.
 *
 * Gather the lands you control, choose any number (zero is legal) on the battlefield, sacrifice
 * them, then add {C} equal to the number actually sacrificed.
 */
val ManaSeism = card("Mana Seism") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Sacrifice any number of lands, then add that much {C}."

    spell {
        effect = Effects.Pipeline {
            val lands = gather(GameObjectFilter.Land, player = Player.You)
            val sacrificed = chooseAnyNumber(
                from = lands,
                useTargetingUI = true,
                prompt = "Choose any number of lands to sacrifice"
            )
            sacrifice(sacrificed)
            run(Effects.AddColorlessMana(DynamicAmounts.distinctEntitiesIn(sacrificed)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"It is the nature of humanity not to worry about tomorrow, especially when " +
            "there's a good chance they won't live to see the end of today.\"\n—Kiku, Night's Flower"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bde4cba-57b2-485f-9724-0f79d156d664.jpg?1783944298"
    }
}
