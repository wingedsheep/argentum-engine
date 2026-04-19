package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Impolite Entrance
 * {R}
 * Sorcery
 *
 * Target creature gains trample and haste until end of turn.
 * Draw a card.
 */
val ImpoliteEntrance = card("Impolite Entrance") {
    manaCost = "{R}"
    typeLine = "Sorcery"
    oracleText = "Target creature gains trample and haste until end of turn.\nDraw a card."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature)
            .then(Effects.GrantKeyword(Keyword.HASTE, creature))
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Scott Murphy"
        flavorText = "\"We smelled you kith-twits makin' porridge in here! We love porridge! Give us your porridge!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45be88d8-0be1-47a2-a1c1-d6e693fc706f.jpg?1767732753"
    }
}
