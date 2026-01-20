package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object VenerableMonk {
    val definition = CardDefinition.creature(
        name = "Venerable Monk",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.HUMAN, Subtype.MONK, Subtype.CLERIC),
        power = 2,
        toughness = 2,
        oracleText = "When Venerable Monk enters the battlefield, you gain 2 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "35",
            rarity = Rarity.UNCOMMON,
            artist = "D. Alexander Gregory",
            flavorText = "His presence brings not only a strong arm but also renewed hope.",
            imageUri = "https://cards.scryfall.io/normal/front/7/2/72322032-c287-4a9e-9d61-a452f6c45bfb.jpg",
            scryfallId = "72322032-c287-4a9e-9d61-a452f6c45bfb",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Venerable Monk") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = GainLifeEffect(2, EffectTarget.Controller)
        )
    }
}
