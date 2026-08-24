package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Interdimensional Web Watch
 * {4}
 * Artifact
 * When this artifact enters, exile the top two cards of your library.
 * Until the end of your next turn, you may play those cards.
 * {T}: Add two mana in any combination of colors. Spend this mana only to cast spells from exile.
 */
val InterdimensionalWebWatch = card("Interdimensional Web Watch") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, exile the top two cards of your library. Until the end of your next turn, you may play those cards.\n{T}: Add two mana in any combination of colors. Spend this mana only to cast spells from exile."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // `Patterns.Exile.impulse` *is* these three effects, and it owns the pipeline key they share.
        // Restating them by hand named that collection something else — a difference no printed word
        // asks for, which is what Argentum Assay's differential reported once it could read the mana
        // line below and started comparing this card at all.
        effect = Patterns.Exile.impulse(count = 2, expiry = MayPlayExpiry.UntilEndOfNextTurn)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddManaInAnyCombination(2, restriction = ManaRestriction.CastFromExileOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Toni Infante"
        flavorText = "The device crackled to life, the portal opened, and Gwen's allies across the Great Web poured forth."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87a8e112-e72f-413f-88a3-e7ce72c2ec53.jpg?1757378025"
    }
}
