package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Venerable Monk
 * {2}{W}
 * Creature - Human Monk Cleric
 * 2/2
 * When Venerable Monk enters the battlefield, you gain 2 life.
 */
val VenerableMonk = card("Venerable Monk") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Monk Cleric"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = GainLifeEffect(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "D. Alexander Gregory"
        flavorText = "\"His presence brings not only a strong arm but also renewed hope.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72322032-c287-4a9e-9d61-a452f6c45bfb.jpg"
    }
}
