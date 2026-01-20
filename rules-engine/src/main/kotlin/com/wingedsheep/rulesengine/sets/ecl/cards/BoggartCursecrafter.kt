package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnOtherCreatureWithSubtypeDies
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Boggart Cursecrafter
 *
 * {B}{R} Creature — Goblin Warlock 2/3
 * Deathtouch
 * Whenever another Goblin you control dies, this creature deals 1 damage to each opponent.
 */
object BoggartCursecrafter {
    val definition = CardDefinition.creature(
        name = "Boggart Cursecrafter",
        manaCost = ManaCost.parse("{B}{R}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.WARLOCK),
        power = 2,
        toughness = 3,
        keywords = setOf(Keyword.DEATHTOUCH),
        oracleText = "Deathtouch\nWhenever another Goblin you control dies, this creature deals 1 damage to each opponent.",
        metadata = ScryfallMetadata(
            collectorNumber = "206",
            rarity = Rarity.UNCOMMON,
            artist = "Alex Stone",
            imageUri = "https://cards.scryfall.io/normal/front/g/g/gghh9012-3456-7890-klmn-gghh90123456.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Boggart Cursecrafter") {
        keywords(Keyword.DEATHTOUCH)

        // Whenever another Goblin you control dies, this creature deals 1 damage to each opponent
        triggered(
            trigger = OnOtherCreatureWithSubtypeDies(Subtype.GOBLIN),
            effect = DealDamageEffect(
                amount = 1,
                target = EffectTarget.EachOpponent
            )
        )
    }
}
