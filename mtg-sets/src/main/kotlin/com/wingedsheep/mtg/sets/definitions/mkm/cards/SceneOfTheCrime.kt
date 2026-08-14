package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Scene of the Crime — Murders at Karlov Manor #267
 * Artifact Land — Clue
 *
 * This land enters tapped.
 * {T}: Add {C}.
 * {T}, Tap an untapped creature you control: Add one mana of any color.
 * {2}, Sacrifice this land: Draw a card.
 *
 * The set's "the crime scene is itself evidence" land: an artifact land that is also a Clue, so the
 * sacrifice-to-draw is the Clue's own ability and every "sacrifice a Clue" payoff in the set can eat
 * it straight off the type line (Scryfall's ruling: "If an effect refers to a Clue, it means any
 * Clue artifact, not just a Clue artifact token").
 *
 * Both mana abilities are `manaAbility = true`, so they don't use the stack and can't be responded
 * to. The any-color one is the Springleaf Drum idiom — `Costs.Composite(Costs.Tap, TapPermanents(1,
 * Creature))`, where `Costs.Tap` taps the land itself and `TapPermanents` taps a *separate* untapped
 * creature you control. Note there's no summoning-sickness clause on the tapped creature: tapping a
 * creature as a cost only requires the creature to be untapped when the ability doesn't use that
 * creature's own {T} symbol, so a freshly-cast creature works.
 *
 * The draw is a normal (non-mana) activated ability: `{2}` plus [Costs.SacrificeSelf], matching the
 * printed Clue text.
 */
val SceneOfTheCrime = card("Scene of the Crime") {
    colorIdentity = ""
    typeLine = "Artifact Land — Clue"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {C}.\n" +
        "{T}, Tap an untapped creature you control: Add one mana of any color.\n" +
        "{2}, Sacrifice this land: Draw a card."

    replacementEffect(EntersTapped())

    // {T}: Add {C}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    // {T}, Tap an untapped creature you control: Add one mana of any color.
    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.TapPermanents(1, GameObjectFilter.Creature))
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    // {2}, Sacrifice this land: Draw a card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "267"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de039992-631b-4feb-a522-acdb0a6d1f26.jpg?1783912824"
    }
}
