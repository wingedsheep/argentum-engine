package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hammer Mage
 * {1}{R}
 * Creature — Human Spellshaper
 * 1 / 1
 * {X}{R}, {T}, Discard a card: Destroy all artifacts with mana value X or less.
 *
 * The X bound is a *predicate on the filter* — `manaValueAtMostX()` reads the X paid for this
 * ability's own cost. It is never a count: `Effects.DestroyAll` takes no count at all.
 */
val HammerMage = card("Hammer Mage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Spellshaper"
    oracleText = "{X}{R}, {T}, Discard a card: Destroy all artifacts with mana value X or less."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{R}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.DestroyAll(GameObjectFilter.Artifact.manaValueAtMostX())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Rebecca Guay"
        flavorText = "When he's holding the hammers, everything looks like a nail."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b959d7ad-a78e-439f-9225-4dbb89f490d7.jpg"
    }
}
