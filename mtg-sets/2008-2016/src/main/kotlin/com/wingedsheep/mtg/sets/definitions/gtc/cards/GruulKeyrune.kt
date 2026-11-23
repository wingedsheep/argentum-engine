package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gruul Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {R} or {G}.
 * {R}{G}: This artifact becomes a 3/2 red and green Beast artifact creature with trample
 * until end of turn.
 *
 * The animation is additive: `BecomeCreature` keeps the Artifact type and `addTypes` restates it,
 * matching the printed "artifact creature". The default target is the Keyrune itself and the
 * default duration is end of turn.
 */
val GruulKeyrune = card("Gruul Keyrune") {
    manaCost = "{3}"
    colorIdentity = "RG"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R} or {G}.\n" +
        "{R}{G}: This artifact becomes a 3/2 red and green Beast artifact creature with trample until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{R}{G}")
        effect = Effects.BecomeCreature(
            power = 3,
            toughness = 2,
            keywords = setOf(Keyword.TRAMPLE),
            creatureTypes = setOf("Beast"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.RED.name, Color.GREEN.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Daniel Ljunggren"
        flavorText = "As perilous and unpredictable as the wilds themselves."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cf96f1c-066e-4fde-acb8-4674842fb6c2.jpg?1783940092"
    }
}
