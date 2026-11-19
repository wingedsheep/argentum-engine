package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Black Oak of Odunos
 * {2}{B}
 * Creature — Zombie Treefolk
 * 0/5
 *
 * Defender
 * {B}, Tap another untapped creature you control: This creature gets +1/+1 until end of turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val BlackOakOfOdunos = card("Black Oak of Odunos") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Treefolk"
    power = 0
    toughness = 5
    oracleText = "Defender\n" +
        "{B}, Tap another untapped creature you control: This creature gets +1/+1 until end of turn."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.TapPermanents(1, excludeSelf = true),
        )
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Chris Rahn"
        flavorText = "Phenax promised the newly dead souls they would be spared from Erebos. In this, he did not lie."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f1e9195-a711-4f4d-9d53-d5d230c092ef.jpg?1783939559"
    }
}
