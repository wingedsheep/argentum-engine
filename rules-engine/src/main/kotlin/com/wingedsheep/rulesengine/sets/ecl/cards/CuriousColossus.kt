package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.TransformAllCreaturesEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Curious Colossus
 *
 * {5}{W}{W} Creature — Giant Warrior 7/7
 * When this creature enters, each creature target opponent controls loses all abilities,
 * becomes a Coward in addition to its other types, and has base power and toughness 1/1.
 */
object CuriousColossus {
    val definition = CardDefinition.creature(
        name = "Curious Colossus",
        manaCost = ManaCost.parse("{5}{W}{W}"),
        subtypes = setOf(Subtype.GIANT, Subtype.WARRIOR),
        power = 7,
        toughness = 7,
        oracleText = "When this creature enters, each creature target opponent controls loses all abilities, " +
                "becomes a Coward in addition to its other types, and has base power and toughness 1/1.",
        metadata = ScryfallMetadata(
            collectorNumber = "12",
            rarity = Rarity.MYTHIC,
            artist = "Raoul Vitale",
            flavorText = "Kithkin respect giants, but not usually from such close range.",
            imageUri = "https://cards.scryfall.io/normal/front/5/8/582b6e5d-0bab-471d-af4d-19438c5fd524.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Curious Colossus") {
        // ETB: Each creature target opponent controls loses all abilities,
        // becomes a Coward in addition to its other types, and has base P/T 1/1
        triggered(
            trigger = OnEnterBattlefield(),
            effect = TransformAllCreaturesEffect(
                target = EffectTarget.AllOpponentCreatures,
                loseAllAbilities = true,
                addCreatureType = "Coward",
                setBasePower = 1,
                setBaseToughness = 1
            )
        )
    }
}
