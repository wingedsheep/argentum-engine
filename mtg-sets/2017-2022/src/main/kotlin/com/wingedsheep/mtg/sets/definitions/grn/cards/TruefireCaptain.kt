package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity

/**
 * Truefire Captain
 * {R}{R}{W}{W}
 * Creature — Human Knight
 * 4/3
 *
 * Mentor
 * Whenever this creature is dealt damage, it deals that much damage to target player.
 *
 * Same shape as Coalhauler Swine: the any-source `isDealtDamage()` trigger, with the amount read off the
 * trigger context — here aimed at a single target player rather than each player.
 */
val TruefireCaptain = card("Truefire Captain") {
    manaCost = "{R}{R}{W}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 3
    oracleText = "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)\n" +
        "Whenever this creature is dealt damage, it deals that much damage to target player."

    mentor()

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        val player = target(Targets.Player)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = player,
        )
        description = "Whenever this creature is dealt damage, it deals that much damage to target player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/6099a230-9298-4649-b257-243af40d0625.jpg?1783934118"
    }
}
