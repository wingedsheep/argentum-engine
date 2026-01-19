package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ExileEffect
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.targeting.TargetCardInGraveyard

object RooftopPercher {
    val definition = CardDefinition.creature(
        name = "Rooftop Percher",
        manaCost = ManaCost.parse("{5}"),
        subtypes = setOf(Subtype.of("Shapeshifter")),
        power = 3,
        toughness = 3,
        keywords = setOf(Keyword.CHANGELING, Keyword.FLYING),
        oracleText = "Changeling\nFlying\n" +
                "When Rooftop Percher enters the battlefield, exile up to two target cards from graveyards. You gain 3 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "2",
            rarity = Rarity.COMMON,
            artist = "Nils Hamm",
            imageUri = "https://cards.scryfall.io/normal/front/2/d/2d89595c-1542-41fe-8d23-997522922698.jpg",
            scryfallId = "2d89595c-1542-41fe-8d23-997522922698",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Rooftop Percher") {
        keywords(Keyword.CHANGELING, Keyword.FLYING)

        val graveyardCards = targets(
            TargetCardInGraveyard(
                count = 2,
                optional = true
            )
        )

        triggered(
            trigger = OnEnterBattlefield(),
            effect =
                ExileEffect(EffectTarget.ContextTarget(graveyardCards.index))
                        then GainLifeEffect(3, EffectTarget.Controller)
        )
    }
}
