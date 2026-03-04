package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Murderous Cut
 * {4}{B}
 * Instant
 * Delve
 * Destroy target creature.
 */
val MurderousCut = card("Murderous Cut") {
    manaCost = "{4}{B}"
    typeLine = "Instant"
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\nDestroy target creature."

    keywords(Keyword.DELVE)

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Yohann Schepacz"
        flavorText = "The blades of a Sultai assassin stab like the fangs of a dragon."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2dadff2-883f-4134-a881-be145cdcbd84.jpg?1562792142"
    }
}
