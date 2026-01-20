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

object ChargingBandits {
    val definition = CardDefinition.creature(
        name = "Charging Bandits",
        manaCost = ManaCost.parse("{4}{B}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.ROGUE),
        power = 3,
        toughness = 3,
        oracleText = "Whenever Charging Bandits attacks, it gets +2/+0 until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "84",
            rarity = Rarity.UNCOMMON,
            artist = "Dermot Power",
            flavorText = "The fear in their victims' eyes is their most cherished reward.",
            imageUri = "https://cards.scryfall.io/normal/front/1/7/1721ee11-c7ee-4878-b2ab-4f090e0c5def.jpg",
            scryfallId = "1721ee11-c7ee-4878-b2ab-4f090e0c5def",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Charging Bandits") {
        triggered(
            trigger = OnAttack(),
            effect = ModifyStatsEffect(2, 0, EffectTarget.Self, untilEndOfTurn = true)
        )
    }
}
