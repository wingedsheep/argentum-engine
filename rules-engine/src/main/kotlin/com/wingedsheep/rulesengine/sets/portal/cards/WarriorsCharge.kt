package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CreatureGroupFilter
import com.wingedsheep.rulesengine.ability.ModifyStatsForGroupEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object WarriorsCharge {
    val definition = CardDefinition.sorcery(
        name = "Warrior's Charge",
        manaCost = ManaCost.parse("{2}{W}"),
        oracleText = "Creatures you control get +1/+1 until end of turn."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "38",
            rarity = Rarity.COMMON,
            artist = "Ted Naifeh",
            flavorText = "It is not the absence of fear that makes a warrior, but its domination.",
            imageUri = "https://cards.scryfall.io/normal/front/8/6/8668e4af-ae89-4fab-8015-8dc643c6cd36.jpg",
            scryfallId = "8668e4af-ae89-4fab-8015-8dc643c6cd36",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Warrior's Charge") {
        spell(ModifyStatsForGroupEffect(1, 1, CreatureGroupFilter.AllYouControl))
    }
}
