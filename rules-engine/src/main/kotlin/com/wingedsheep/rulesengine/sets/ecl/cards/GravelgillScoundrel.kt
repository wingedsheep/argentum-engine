package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.OnAttack
import com.wingedsheep.rulesengine.ability.TapCreatureForEffectEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Gravelgill Scoundrel
 *
 * {1}{U} Creature — Merfolk Rogue 1/3
 * Vigilance
 * Whenever this creature attacks, you may tap another untapped creature you control.
 * If you do, this creature can't be blocked this turn.
 */
object GravelgillScoundrel {
    val definition = CardDefinition.creature(
        name = "Gravelgill Scoundrel",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype.MERFOLK, Subtype.ROGUE),
        power = 1,
        toughness = 3,
        keywords = setOf(Keyword.VIGILANCE),
        oracleText = "Vigilance\nWhenever this creature attacks, you may tap another untapped creature " +
                "you control. If you do, this creature can't be blocked this turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "53",
            rarity = Rarity.COMMON,
            artist = "John Tedrick",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb7b7b7b-7b7b-7b7b-7b7b-7b7b7b7b7b7b.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Gravelgill Scoundrel") {
        keywords(Keyword.VIGILANCE)

        // Attack trigger: may tap another creature to become unblockable
        triggered(
            trigger = OnAttack(),
            effect = TapCreatureForEffectEffect(
                innerEffect = GrantKeywordUntilEndOfTurnEffect(
                    keyword = Keyword.CANT_BE_BLOCKED,
                    target = EffectTarget.Self
                )
            )
        )
    }
}
