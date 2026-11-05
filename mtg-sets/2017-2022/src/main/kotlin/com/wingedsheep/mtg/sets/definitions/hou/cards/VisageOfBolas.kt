package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Visage of Bolas — Hour of Devastation #208 (planeswalker deck)
 * {4} · Artifact
 *
 * When this artifact enters, you may search your library and/or graveyard for a card named Nicol
 * Bolas, the Deceiver, reveal it, and put it into your hand. If you search your library this way,
 * shuffle.
 * {T}: Add {U}, {B}, or {R}.
 *
 * The planeswalker-deck tutor shape (Sun-Blessed Mount, Angrath's Fury):
 * [Patterns.Library.searchLibraryAndOrGraveyard] over a name filter, optional, revealed, to hand.
 * The mana ability is Obelisk of Grixis's: three [Effects.AddMana] abilities on [Costs.Tap].
 */
val VisageOfBolas = card("Visage of Bolas") {
    manaCost = "{4}"
    colorIdentity = "UBR"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you may search your library and/or graveyard for a card " +
        "named Nicol Bolas, the Deceiver, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle.\n{T}: Add {U}, {B}, or {R}."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Nicol Bolas, the Deceiver"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this artifact enters, you may search your library and/or graveyard for a " +
            "card named Nicol Bolas, the Deceiver, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        inBooster = false
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f29bbfe2-d59f-4361-8ea4-6a8d1b909769.jpg?1783935983"
    }
}
