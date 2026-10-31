package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Oakhame Ranger // Bring Back
 * {G/W}{G/W}{G/W}{G/W}
 * Creature — Elf Knight Ranger
 * 2/2
 *
 * {T}: Creatures you control get +1/+1 until end of turn.
 *
 * Adventure: Bring Back — {G/W}{G/W}{G/W}{G/W}, Sorcery — Adventure
 * Create two 1/1 white Human creature tokens.
 */
val OakhameRanger = card("Oakhame Ranger") {
    manaCost = "{G/W}{G/W}{G/W}{G/W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elf Knight Ranger"
    power = 2
    toughness = 2
    oracleText = "{T}: Creatures you control get +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    adventure("Bring Back") {
        manaCost = "{G/W}{G/W}{G/W}{G/W}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create two 1/1 white Human creature tokens. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateToken(
                count = 2,
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Human"),
                imageUri = "https://cards.scryfall.io/normal/front/9/4/94057dc6-e589-4a29-9bda-90f5bece96c4.jpg?1783932485",
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Mitchell Malloy & Maddie Julyk"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a665794-513f-4f78-92c9-1844ec27c79c.jpg?1783932589"
    }
}
