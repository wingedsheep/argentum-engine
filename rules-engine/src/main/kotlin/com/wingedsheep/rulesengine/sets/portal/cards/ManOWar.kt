package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ManOWar {
    val definition = CardDefinition.creature(
        name = "Man-o'-War",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype.JELLYFISH),
        power = 2,
        toughness = 2,
        oracleText = "When Man-o'-War enters the battlefield, return target creature to its owner's hand.",
        metadata = ScryfallMetadata(
            collectorNumber = "59",
            rarity = Rarity.UNCOMMON,
            artist = "Una Fricker",
            imageUri = "https://cards.scryfall.io/normal/front/e/8/e835b618-83c1-46e2-b8bd-aec56f58ccfc.jpg",
            scryfallId = "e835b618-83c1-46e2-b8bd-aec56f58ccfc",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Man-o'-War") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = ReturnToHandEffect(EffectTarget.TargetCreature)
        )
    }
}
