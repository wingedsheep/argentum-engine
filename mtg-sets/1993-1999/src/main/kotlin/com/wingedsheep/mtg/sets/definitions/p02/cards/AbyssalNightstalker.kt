package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Abyssal Nightstalker
 * {3}{B}
 * Creature — Nightstalker
 * 2/2
 * Whenever this creature attacks and isn't blocked, defending player discards a card.
 */
val AbyssalNightstalker = card("Abyssal Nightstalker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightstalker"
    oracleText = "Whenever this creature attacks and isn't blocked, defending player discards a card."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacksAndIsntBlocked()
        effect = Effects.Discard(1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Sam Wood"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b688039-7441-490b-ad06-8a72086c4023.jpg?1783946481"
    }
}
