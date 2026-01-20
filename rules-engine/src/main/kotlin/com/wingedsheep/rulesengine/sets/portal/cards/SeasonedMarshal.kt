package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnAttack
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SeasonedMarshal {
    val definition = CardDefinition.creature(
        name = "Seasoned Marshal",
        manaCost = ManaCost.parse("{2}{W}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.SOLDIER),
        power = 2,
        toughness = 2,
        oracleText = "Whenever Seasoned Marshal attacks, you may tap target creature.",
        metadata = ScryfallMetadata(
            collectorNumber = "26",
            rarity = Rarity.UNCOMMON,
            artist = "Zina Saunders",
            imageUri = "https://cards.scryfall.io/normal/front/1/7/17db0060-3667-4c8c-ae9b-d62dceac64e3.jpg",
            scryfallId = "17db0060-3667-4c8c-ae9b-d62dceac64e3",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Seasoned Marshal") {
        triggered(
            trigger = OnAttack(),
            effect = TapUntapEffect(EffectTarget.TargetCreature, tap = true)
        )
    }
}
