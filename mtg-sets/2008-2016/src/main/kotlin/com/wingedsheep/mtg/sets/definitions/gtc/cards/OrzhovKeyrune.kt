package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Orzhov Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {W} or {B}.
 * {W}{B}: This artifact becomes a 1/4 white and black Thrull artifact creature with lifelink
 * until end of turn.
 *
 * The animation is additive: `BecomeCreature` keeps the Artifact type and `addTypes` restates it,
 * matching the printed "artifact creature". The default target is the Keyrune itself and the
 * default duration is end of turn.
 */
val OrzhovKeyrune = card("Orzhov Keyrune") {
    manaCost = "{3}"
    colorIdentity = "WB"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W} or {B}.\n" +
        "{W}{B}: This artifact becomes a 1/4 white and black Thrull artifact creature with lifelink until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
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
        cost = Costs.Mana("{W}{B}")
        effect = Effects.BecomeCreature(
            power = 1,
            toughness = 4,
            keywords = setOf(Keyword.LIFELINK),
            creatureTypes = setOf("Thrull"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.WHITE.name, Color.BLACK.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Daniel Ljunggren"
        flavorText = "You only need to wield it once to appreciate the power of control."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd5c0f38-916a-4f6c-b678-7447cb0709e0.jpg?1783940091"
    }
}
