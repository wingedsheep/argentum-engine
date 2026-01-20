package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object Vengeance {
    val definition = CardDefinition.sorcery(
        name = "Vengeance",
        manaCost = ManaCost.parse("{3}{W}"),
        oracleText = "Destroy target tapped creature."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "36",
            rarity = Rarity.UNCOMMON,
            artist = "Andrew Robinson",
            flavorText = "Bitter as wormwood, sweet as mulled wine.",
            imageUri = "https://cards.scryfall.io/normal/front/c/9/c91c249b-157c-4f1d-8171-29d1e75b1c9f.jpg",
            scryfallId = "c91c249b-157c-4f1d-8171-29d1e75b1c9f",
            releaseDate = "1997-05-01"
        )
    )

    // Note: The targeting restriction (tapped creature) is handled by the target validation system
    val script = cardScript("Vengeance") {
        spell(DestroyEffect(EffectTarget.TargetTappedCreature))
    }
}
