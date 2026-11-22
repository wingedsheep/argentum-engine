package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gaze of Granite
 * {X}{B}{B}{G}
 * Sorcery
 * Destroy each nonland permanent with mana value X or less.
 *
 * Meltdown's shape widened to every nonland permanent. Mana value is read off each permanent on
 * resolution, so an {X} in a permanent's own cost counts as 0 and an uncopied token is 0.
 */
val GazeOfGranite = card("Gaze of Granite") {
    manaCost = "{X}{B}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Destroy each nonland permanent with mana value X or less."

    spell {
        effect = Effects.DestroyAll(
            GameObjectFilter.NonlandPermanent.manaValueAtMostDynamic(DynamicAmounts.xValue())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Nils Hamm"
        flavorText = "Golgari reclaimers resent the gorgons, begrudging the loss of so many fine nutrients locked away in stone."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96c9ac10-d114-4aa5-87ac-f1069cde8e40.jpg?1783940029"
        ruling("2013-04-15", "If a permanent has {X} in its mana cost, the value of that X is 0.")
        ruling(
            "2013-04-15",
            "The mana value of a creature token is 0 unless that token is a copy of another creature, in which case it copies that creature's mana cost."
        )
    }
}
