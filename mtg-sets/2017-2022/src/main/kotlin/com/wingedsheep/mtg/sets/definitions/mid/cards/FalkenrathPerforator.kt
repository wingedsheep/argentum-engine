package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Falkenrath Perforator
 * {1}{R}
 * Creature — Vampire
 * 2/1
 * Whenever this creature attacks, it deals 1 damage to defending player.
 */
val FalkenrathPerforator = card("Falkenrath Perforator") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks, it deals 1 damage to defending player."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.DefendingPlayer))
        description = "Whenever this creature attacks, it deals 1 damage to defending player."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Marcela Medeiros"
        flavorText = "\"Lady Anje sends her regards. I'll carve the rest of her message into your skin.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c40998e0-d7d0-4d79-a40a-9a381debd2cb.jpg?1783925602"
    }
}
