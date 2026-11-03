package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dominion Assault Force
 * {3}{G}
 * Creature — Jem'Hadar Warrior
 * 4/4
 * Vigilance
 * When this creature enters, choose one —
 * • Exile target card from a graveyard.
 * • Surveil 1.
 */
val DominionAssaultForce = card("Dominion Assault Force") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Jem'Hadar Warrior"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "When this creature enters, choose one —\n" +
        "• Exile target card from a graveyard.\n" +
        "• Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Exile target card from a graveyard.") {
                val card = target(TargetFilter.CardInGraveyard)
                effect = Effects.Exile(card)
            },
            Mode(effect = Effects.Surveil(1), description = "Surveil 1."),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Ioannis Fiore"
        flavorText = "\"As of this moment, we are all dead. We go into battle to reclaim our lives.\"\n" +
            "—First Omet'iklan"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28a55f63-1cbd-4362-a192-e42a06f44686.jpg"
    }
}
