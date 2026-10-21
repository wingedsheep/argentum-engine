package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dryad's Revival
 * {2}{G}
 * Sorcery
 * Return target card from your graveyard to your hand.
 * Flashback {4}{G}
 *
 * Regrowth with flashback.
 */
val DryadsRevival = card("Dryad's Revival") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return target card from your graveyard to your hand.\n" +
        "Flashback {4}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.Move(t, Zone.HAND)
    }

    keywordAbility(KeywordAbility.flashback("{4}{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Mila Pesic"
        flavorText = "\"Pleased to meet you, Seven.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c0aa3eb-0aa8-42da-b437-a3f336df585d.jpg"
    }
}
