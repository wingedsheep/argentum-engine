package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Ruthless Negotiation
 * {B}
 * Sorcery
 *
 * Target opponent exiles a card from their hand. If this spell was cast
 * from a graveyard, draw a card.
 * Flashback {4}{B}
 */
val RuthlessNegotiation = card("Ruthless Negotiation") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent exiles a card from their hand. If this spell was cast from a graveyard, draw a card.\nFlashback {4}{B} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val opponent = target("opponent", Targets.Opponent)
        effect = HandPatterns.exileFromHand(1, opponent)
            .then(ConditionalEffect(
                condition = Conditions.WasCastFromZone(Zone.GRAVEYARD),
                effect = Effects.DrawCards(1)
            ))
    }

    keywordAbility(KeywordAbility.flashback("{4}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Rhonda Libbey"
        flavorText = "\"Next time you come this way, do us both a favor and have something worth stealing, yeah?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7f4360c-8d68-4058-b9ec-da9948cb060d.jpg?1721426488"
    }
}
