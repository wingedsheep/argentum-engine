package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Goldmeadow Nomad
 *
 * {W} Creature — Kithkin Scout 1/2
 * {W}, Exile this card from your graveyard: Create a 1/1 green and white
 * Kithkin creature token. Activate only as a sorcery.
 */
object GoldmeadowNomad {
    val definition = CardDefinition.creature(
        name = "Goldmeadow Nomad",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.KITHKIN, Subtype.SCOUT),
        power = 1,
        toughness = 2,
        oracleText = "{W}, Exile this card from your graveyard: Create a 1/1 green and white " +
                "Kithkin creature token. Activate only as a sorcery.",
        metadata = ScryfallMetadata(
            collectorNumber = "18",
            rarity = Rarity.COMMON,
            artist = "Paolo Parente",
            flavorText = "During my time on the road, the emptiness I felt after severing myself from the thoughtweft has faded with every story heard and every tale spread.",
            imageUri = "https://cards.scryfall.io/normal/front/0/0/00ddbe6c-11de-4bc6-aabe-d6d8385a838a.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Goldmeadow Nomad") {
        // Graveyard activated ability: {W}, Exile this card from graveyard
        // Note: Graveyard activation requires special handling - the cost includes
        // exiling this card from the graveyard, which is tracked separately
        // For now, we model this as an activated ability with the mana cost
        activated(
            cost = AbilityCost.Mana(white = 1),
            effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Kithkin")
            ),
            timing = TimingRestriction.SORCERY
            // Note: Full implementation needs "exile from graveyard" as part of cost
            // and ability should only be activatable from graveyard zone
        )
    }
}
