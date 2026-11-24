package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Selesnya Keyrune
 * {3}
 * Artifact
 *
 * {T}: Add {G} or {W}.
 * {G}{W}: This artifact becomes a 3/3 green and white Wolf artifact creature until end of turn.
 *
 * Same shape as the other Keyrunes: the animation is additive, `addTypes` restates the printed
 * "artifact creature", and the default target/duration is the Keyrune itself until end of turn.
 */
val SelesnyaKeyrune = card("Selesnya Keyrune") {
    manaCost = "{3}"
    colorIdentity = "GW"
    typeLine = "Artifact"
    oracleText = "{T}: Add {G} or {W}.\n" +
        "{G}{W}: This artifact becomes a 3/3 green and white Wolf artifact creature until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
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
        cost = Costs.Mana("{G}{W}")
        effect = Effects.BecomeCreature(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Wolf"),
            addTypes = setOf("ARTIFACT"),
            colors = setOf(Color.GREEN.name, Color.WHITE.name)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Daniel Ljunggren"
        flavorText = "A mark of unity when you want one; a fierce guardian when you need one."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1645eafd-cd17-463a-9d7e-42f5f7b5196f.jpg?1783940323"
    }
}
