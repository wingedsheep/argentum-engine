package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dreamborn Muse
 * {2}{U}{U}
 * Creature — Spirit
 * 2/2
 * At the beginning of each player's upkeep, that player mills X cards,
 * where X is the number of cards in their hand.
 */
val DreambornMuse = card("Dreamborn Muse") {
    manaCost = "{2}{U}{U}"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "At the beginning of each player's upkeep, that player mills X cards, where X is the number of cards in their hand."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.Mill(
            DynamicAmount.Count(Player.TriggeringPlayer, Zone.HAND),
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Kev Walker"
        flavorText = "\"Her voice is insight, piercing and true.\" —Ixidor, reality sculptor"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e36cf11-5dfb-4593-8335-f739b7c7829c.jpg?1562926891"
    }
}
