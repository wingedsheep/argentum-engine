package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Forge Boss
 * {2}{B}{R}
 * Creature — Human Warrior
 * 3/4
 * Whenever you sacrifice one or more other creatures, this creature deals 2 damage to each
 * opponent. This ability triggers only once each turn.
 */
val ForgeBoss = card("Forge Boss") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Warrior"
    oracleText = "Whenever you sacrifice one or more other creatures, this creature deals 2 damage to each opponent. " +
        "This ability triggers only once each turn."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.sacrificesAnother(GameObjectFilter.Creature, batch = true)
        oncePerTurn = true
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Igor Kieryluk"
        flavorText = "\"Big furnace like this can reduce anything to ash. You got something you need reduced to ash?\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5052247d-ef72-47f0-973d-2be98bf754c1.jpg?1790192811"
    }
}
