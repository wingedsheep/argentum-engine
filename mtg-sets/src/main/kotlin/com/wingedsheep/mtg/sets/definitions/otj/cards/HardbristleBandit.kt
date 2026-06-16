package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hardbristle Bandit
 * {1}{G}
 * Creature — Plant Rogue
 * 1/1
 * {T}: Add one mana of any color.
 * Whenever you commit a crime, untap this creature. This ability triggers only once each turn.
 *
 * The mana ability is a [Costs.Tap] / [Effects.AddAnyColorMana] activated ability (manaAbility = true).
 * The crime trigger untaps this creature ([Effects.Untap] on [EffectTarget.Self]) and is limited to
 * one trigger per turn via `oncePerTurn`. The crime-this-turn tracker is read at the trigger's emit
 * site by the engine's `CrimeDetector`; this card only consumes [Triggers.YouCommitCrime].
 */
val HardbristleBandit = card("Hardbristle Bandit") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Rogue"
    power = 1
    toughness = 1
    oracleText = "{T}: Add one mana of any color.\n" +
        "Whenever you commit a crime, untap this creature. This ability triggers only once each turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = Effects.Untap(EffectTarget.Self)
        description = "Whenever you commit a crime, untap this creature. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Francis Tneh"
        flavorText = "Visitors to the town of Hardbristle found the locals to be quite prickly."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbe544fb-93b7-4640-b886-cb0b3e437357.jpg"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
    }
}
