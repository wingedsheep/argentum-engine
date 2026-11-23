package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Boros Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {R} or {W}.
 * {R}{W}: This artifact becomes a 1/1 red and white Soldier artifact creature with double strike
 * until end of turn.
 *
 * The animation is additive: `BecomeCreature` keeps the Artifact type and `addTypes` restates it,
 * matching the printed "artifact creature". The default target is the Keyrune itself and the
 * default duration is end of turn.
 */
val BorosKeyrune = card("Boros Keyrune") {
    manaCost = "{3}"
    colorIdentity = "RW"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R} or {W}.\n" +
        "{R}{W}: This artifact becomes a 1/1 red and white Soldier artifact creature with double strike until end of turn. (It deals both first-strike and regular combat damage.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{R}{W}")
        effect = Effects.BecomeCreature(
            power = 1,
            toughness = 1,
            keywords = setOf(Keyword.DOUBLE_STRIKE),
            creatureTypes = setOf("Soldier"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.RED.name, Color.WHITE.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4b65847-fee2-4e00-b598-7363059ec3ff.jpg?1783940093"
    }
}
