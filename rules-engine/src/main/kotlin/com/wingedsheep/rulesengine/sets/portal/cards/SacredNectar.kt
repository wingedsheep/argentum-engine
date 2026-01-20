package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object SacredNectar {
    val definition = CardDefinition.sorcery(
        name = "Sacred Nectar",
        manaCost = ManaCost.parse("{1}{W}"),
        oracleText = "You gain 4 life."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "25",
            rarity = Rarity.COMMON,
            artist = "Janine Johnston",
            flavorText = "\"For he on honey-dew hath fed, And drunk the milk of Paradise.\" —Samuel Taylor Coleridge, \"Kubla Khan\"",
            imageUri = "https://cards.scryfall.io/normal/front/4/8/484d1b31-5363-49ef-9b13-2005568636c1.jpg",
            scryfallId = "484d1b31-5363-49ef-9b13-2005568636c1",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Sacred Nectar") {
        spell(GainLifeEffect(4, EffectTarget.Controller))
    }
}
