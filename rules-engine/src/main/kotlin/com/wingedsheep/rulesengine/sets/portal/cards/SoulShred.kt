package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DrainEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object SoulShred {
    val definition = CardDefinition.sorcery(
        name = "Soul Shred",
        manaCost = ManaCost.parse("{2}{B}"),
        oracleText = "Soul Shred deals 3 damage to target nonblack creature. You gain 3 life."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "112",
            rarity = Rarity.COMMON,
            artist = "Alan Rabinowitz",
            flavorText = "It would be a shame to let life slip away to nothing.",
            imageUri = "https://cards.scryfall.io/normal/front/9/9/990902d2-9594-4963-807c-48a90324d487.jpg",
            scryfallId = "990902d2-9594-4963-807c-48a90324d487",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Soul Shred") {
        spell(DrainEffect(3, EffectTarget.TargetNonblackCreature))
    }
}
