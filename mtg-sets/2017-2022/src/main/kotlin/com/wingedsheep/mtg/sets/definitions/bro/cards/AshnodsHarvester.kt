package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ashnod's Harvester — The Brothers' War #117
 * {2} · Artifact Creature — Construct · Uncommon
 * 3/1
 *
 * Whenever this creature attacks, exile target card from a graveyard.
 * Unearth {1}{B}
 */
val AshnodsHarvester = card("Ashnod's Harvester") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature attacks, exile target card from a graveyard.\n" +
        "Unearth {1}{B} ({1}{B}: Return this card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. " +
        "Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val cardInGraveyard = target(TargetFilter.CardInGraveyard)
        effect = Effects.Exile(cardInGraveyard)
        description = "Whenever this creature attacks, exile target card from a graveyard."
    }

    unearth("{1}{B}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Halil Ural"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58baa977-16c1-4983-8343-dbd65e98ddb7.jpg?1783920078"
    }
}
