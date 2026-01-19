package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Flock Impostor
 *
 * {2}{W} Creature — Shapeshifter 2/2
 * Changeling (This card is every creature type.)
 * Flash
 * Flying
 * When this creature enters, return up to one other target creature
 * you control to its owner's hand.
 */
object FlockImpostor {
    val definition = CardDefinition.creature(
        name = "Flock Impostor",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.SHAPESHIFTER),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.CHANGELING, Keyword.FLASH, Keyword.FLYING),
        oracleText = "Changeling (This card is every creature type.)\n" +
                "Flash\n" +
                "Flying\n" +
                "When this creature enters, return up to one other target creature " +
                "you control to its owner's hand.",
        metadata = ScryfallMetadata(
            collectorNumber = "16",
            rarity = Rarity.UNCOMMON,
            artist = "Ilse Gort",
            imageUri = "https://cards.scryfall.io/normal/front/d/3/d32d0336-5140-41f9-bc67-f3d743b9231d.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Flock Impostor") {
        keywords(Keyword.CHANGELING, Keyword.FLASH, Keyword.FLYING)

        // ETB: Return up to one other target creature you control to hand
        triggered(
            trigger = OnEnterBattlefield(),
            effect = ReturnToHandEffect(
                target = EffectTarget.TargetControlledCreature
            ),
            optional = true  // "up to one" means it's optional
        )
    }
}
