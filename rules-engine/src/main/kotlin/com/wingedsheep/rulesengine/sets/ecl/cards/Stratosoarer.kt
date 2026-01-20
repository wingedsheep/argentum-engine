package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Stratosoarer
 *
 * {4}{U} Creature — Elemental 3/5
 * Flying
 * When this creature enters, target creature gains flying until end of turn.
 * Basic landcycling {1}{U}
 */
object Stratosoarer {
    val definition = CardDefinition.creature(
        name = "Stratosoarer",
        manaCost = ManaCost.parse("{4}{U}"),
        subtypes = setOf(Subtype.ELEMENTAL),
        power = 3,
        toughness = 5,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhen this creature enters, target creature gains flying until end of turn.\n" +
                "Basic landcycling {1}{U}",
        metadata = ScryfallMetadata(
            collectorNumber = "72",
            rarity = Rarity.COMMON,
            artist = "John Tedrick",
            imageUri = "https://cards.scryfall.io/normal/front/d/d/dd4d4d4d-4d4d-4d4d-4d4d-4d4d4d4d4d4d.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Stratosoarer") {
        keywords(Keyword.FLYING)

        // ETB: Target creature gains flying until end of turn
        triggered(
            trigger = OnEnterBattlefield(),
            effect = GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.FLYING,
                target = EffectTarget.TargetCreature
            )
        )

        // Basic landcycling {1}{U}
        basicLandcycling(AbilityCost.Mana(blue = 1, generic = 1))
    }
}
