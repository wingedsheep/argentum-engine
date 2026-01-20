package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.MustBeBlockedEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object AlluringScent {
    val definition = CardDefinition.sorcery(
        name = "Alluring Scent",
        manaCost = ManaCost.parse("{1}{G}{G}"),
        oracleText = "All creatures able to block target creature this turn do so."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "157",
            rarity = Rarity.RARE,
            artist = "Ted Naifeh",
            flavorText = "Doom rarely smells this sweet.",
            imageUri = "https://cards.scryfall.io/normal/front/8/7/8726242e-bfd8-4ed5-a016-ac0c82e4762b.jpg",
            scryfallId = "8726242e-bfd8-4ed5-a016-ac0c82e4762b",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Alluring Scent") {
        spell(MustBeBlockedEffect(EffectTarget.TargetCreature))
    }
}
