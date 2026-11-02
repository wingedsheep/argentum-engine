package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Salvager of Secrets
 * {3}{U}{U}
 * Creature — Merfolk Wizard
 * 2/2
 * When this creature enters, return target instant or sorcery card from your graveyard to your hand.
 */
val SalvagerOfSecrets = card("Salvager of Secrets") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, return target instant or sorcery card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.ReturnToHand(spellCard)
        description = "When this creature enters, return target instant or sorcery card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Lars Grant-West"
        flavorText = "The greatest treasure of the deep is an echo from above."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a55f484-5734-469c-8d41-95ce44473ec1.jpg?1783934583"
    }
}
