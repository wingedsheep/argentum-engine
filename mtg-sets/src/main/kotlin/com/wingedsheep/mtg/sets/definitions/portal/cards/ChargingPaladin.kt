package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charging Paladin
 * {2}{W}
 * Creature — Human Knight
 * 2/2
 * Whenever Charging Paladin attacks, it gets +0/+3 until end of turn.
 */
val ChargingPaladin = card("Charging Paladin") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(0, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Kev Walker"
        flavorText = "A true warrior's thoughts are of victory, not death."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29db1bbf-a6cf-460c-bec8-dbd682157af4.jpg"
    }
}
