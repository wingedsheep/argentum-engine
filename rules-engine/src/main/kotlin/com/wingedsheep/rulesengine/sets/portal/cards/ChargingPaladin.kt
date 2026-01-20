package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.OnAttack
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ChargingPaladin {
    val definition = CardDefinition.creature(
        name = "Charging Paladin",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.KNIGHT),
        power = 2,
        toughness = 2,
        oracleText = "Whenever Charging Paladin attacks, it gets +0/+3 until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "11",
            rarity = Rarity.UNCOMMON,
            artist = "Kev Walker",
            flavorText = "A true warrior's thoughts are of victory, not death.",
            imageUri = "https://cards.scryfall.io/normal/front/2/9/29db1bbf-a6cf-460c-bec8-dbd682157af4.jpg",
            scryfallId = "29db1bbf-a6cf-460c-bec8-dbd682157af4",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Charging Paladin") {
        triggered(
            trigger = OnAttack(),
            effect = ModifyStatsEffect(0, 3, EffectTarget.Self, untilEndOfTurn = true)
        )
    }
}
