package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object AdeptWatershaper {
    val definition = CardDefinition.creature(
        name = "Adept Watershaper",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype.of("Merfolk"), Subtype.WIZARD),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.PROWESS),
        oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
                "{2}{U}: Target creature gains flying until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "3",
            rarity = Rarity.RARE,
            artist = "Pauline Voss",
            flavorText = "\"If even the simplest land-dweller can divert the river with no more than a shovel, just imagine what I can do.\"",
            imageUri = "https://cards.scryfall.io/normal/front/6/e/6e53f246-8347-4632-9d5b-4aeb12f7b762.jpg",
            scryfallId = "6e53f246-8347-4632-9d5b-4aeb12f7b762",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Adept Watershaper") {
        // Use the macro to add keyword AND trigger logic
        prowess()

        // Activated Ability
        activated(
            cost = AbilityCost.Mana(generic = 2, blue = 1),
            effect = GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.FLYING,
                target = EffectTarget.TargetCreature
            )
        )
    }
}
