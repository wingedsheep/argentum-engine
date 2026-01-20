package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageToAllEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object DrySpell {
    val definition = CardDefinition.sorcery(
        name = "Dry Spell",
        manaCost = ManaCost.parse("{1}{B}"),
        oracleText = "Dry Spell deals 1 damage to each creature and each player."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "90",
            rarity = Rarity.UNCOMMON,
            artist = "Roger Raupp",
            flavorText = "A fist of dust to line your throat, a bowl of sand to fill your belly.",
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a142f369-8fdd-4dc8-b5d9-3493455cc588.jpg",
            scryfallId = "a142f369-8fdd-4dc8-b5d9-3493455cc588",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Dry Spell") {
        spell(DealDamageToAllEffect(1))
    }
}
