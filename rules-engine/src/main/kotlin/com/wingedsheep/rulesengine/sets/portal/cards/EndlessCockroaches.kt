package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnDeath
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object EndlessCockroaches {
    val definition = CardDefinition.creature(
        name = "Endless Cockroaches",
        manaCost = ManaCost.parse("{1}{B}{B}"),
        subtypes = setOf(Subtype.INSECT),
        power = 1,
        toughness = 1,
        oracleText = "When Endless Cockroaches dies, return it to its owner's hand.",
        metadata = ScryfallMetadata(
            collectorNumber = "92",
            rarity = Rarity.RARE,
            artist = "Ron Spencer",
            imageUri = "https://cards.scryfall.io/normal/front/0/d/0d3d18b9-ad59-435b-934b-703e10287e32.jpg",
            scryfallId = "0d3d18b9-ad59-435b-934b-703e10287e32",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Endless Cockroaches") {
        triggered(
            trigger = OnDeath(),
            effect = ReturnToHandEffect(EffectTarget.Self)
        )
    }
}
