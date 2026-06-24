package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ether
 * {3}{U}
 * Artifact
 *
 * {T}, Exile this artifact: Add {U}. When you next cast an instant or sorcery spell this turn,
 * copy that spell. You may choose new targets for the copy.
 *
 * This is a mana ability (per the official rulings) — it adds mana, has no target, and doesn't use
 * the stack. It also sets up a one-shot "copy your next instant/sorcery this turn" delayed trigger
 * via [Effects.CopyNextSpellCast].
 */
val Ether = card("Ether") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{T}, Exile this artifact: Add {U}. When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new targets for the copy."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.ExileSelf)
        manaAbility = true
        effect = Effects.Composite(
            Effects.AddMana(Color.BLUE),
            Effects.CopyNextSpellCast()
        )
        description = "{T}, Exile this artifact: Add {U}. When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Ben Wootten"
        flavorText = "Restores 20 MP."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/896ee6e9-15a9-4974-b576-50f4759fac38.jpg?1748705952"
    }
}
