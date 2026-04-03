package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Calamitous Tide
 * {4}{U}{U}
 * Sorcery
 *
 * Return up to two target creatures to their owners' hands. Draw two cards,
 * then discard a card.
 */
val CalamitousTide = card("Calamitous Tide") {
    manaCost = "{4}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creatures to their owners' hands. Draw two cards, then discard a card."

    spell {
        val (c1, c2) = targets("creature", TargetCreature(count = 2, optional = true))
        effect = Effects.ReturnToHand(c1)
            .then(Effects.ReturnToHand(c2))
            .then(EffectPatterns.loot(draw = 2, discard = 1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Samuele Bandini"
        flavorText = "Zoraline turned her attention to the shore. Helga and Ral's conversation quickly faded from her mind as she watched the gigantic wave approach."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/178bc8b2-ffa0-4549-aead-aacb3db3cf19.jpg?1721431233"
        ruling("2024-07-26", "If all of the targets are illegal as Calamitous Tide tries to resolve, it won't resolve and none of its effects will happen. You won't draw or discard any cards.")
    }
}
