package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stalking Stones — Tempest #327 (canonical printing; Mirrodin, Duel Decks, Tempest Remastered and
 * The List are later reprints)
 * Land
 *
 * {T}: Add {C}.
 * {6}: This land becomes a 3/3 Elemental artifact creature that's still a land.
 * (This effect lasts indefinitely.)
 *
 * The Mishra's Factory animate shape with two differences that matter: [Duration.Permanent] rather
 * than end-of-turn — "indefinitely" means the effect never wears off, only ends if the permanent
 * leaves — and `addTypes = ARTIFACT` so it becomes an *artifact* creature while keeping its printed
 * Land type ("that's still a land"; Layer 4 type-adding is additive).
 *
 * The animate ability costs mana only, with no {T} in it, so it can be activated the turn the land
 * enters — but the resulting creature still has summoning sickness unless its controller has
 * controlled the land since their most recent turn began (the 2008-08-01 ruling), which is what
 * stops it attacking immediately.
 */
val StalkingStones = card("Stalking Stones") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{6}: This land becomes a 3/3 Elemental artifact creature that's still a land. " +
        "(This effect lasts indefinitely.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            creatureTypes = setOf(Subtype.ELEMENTAL.value),
            addTypes = setOf(CardType.ARTIFACT.name),
            duration = Duration.Permanent,
        )
        description = "{6}: This land becomes a 3/3 Elemental artifact creature that's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "327"
        artist = "Stephen Daniele"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4d3d349-5c23-43a9-b25e-0e1a35b84673.jpg?1783946596"
        ruling(
            "2009-10-01",
            "Activating the ability that turns it into a creature while it's already a creature will " +
                "override any effects that set its power and/or toughness to a specific number. However, " +
                "any effect that raises or lowers power and/or toughness (such as the effect created by " +
                "Giant Growth, Glorious Anthem, or a +1/+1 counter) will continue to apply."
        )
        ruling(
            "2008-08-01",
            "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be " +
                "activated, only if its controller has continuously controlled that permanent since the " +
                "beginning of their most recent turn. It doesn't matter how long the permanent has been a " +
                "creature."
        )
    }
}
