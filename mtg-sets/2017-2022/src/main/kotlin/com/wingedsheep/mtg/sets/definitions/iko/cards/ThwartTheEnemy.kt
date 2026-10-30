package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Thwart the Enemy
 * {2}{G}
 * Instant
 *
 * Prevent all damage that would be dealt this turn by creatures your opponents control.
 *
 * Ethereal Haze's source-side group shield with no recipient clause: combat and noncombat damage
 * alike. The group is re-read from projected state when the damage would be dealt, with the
 * shield's controller as "you", so a creature an opponent gains control of later in the turn is
 * covered too.
 */
val ThwartTheEnemy = card("Thwart the Enemy") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all damage that would be dealt this turn by creatures your opponents control."

    spell {
        effect = Effects.PreventDamage(
            direction = PreventionDirection.FromTarget,
            sources = PreventionSourceFilter.Matching(GameObjectFilter.Creature.opponentControls())
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Chris Rallis"
        flavorText = "\"Your prey is in my custody now. Withdraw.\"\n—Vivien Reid"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7101f35-d8c9-4320-bffc-8bb25000d103.jpg"
    }
}
