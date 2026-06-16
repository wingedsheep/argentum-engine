package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Oubliette
 * {1}{B}{B}
 * Enchantment
 * When this enchantment enters, target creature phases out until this enchantment leaves the
 * battlefield. Tap that creature as it phases in this way. (Auras and Equipment phase out with it.
 * While permanents are phased out, they're treated as though they don't exist.)
 *
 * Modern Oracle uses phasing (the original 1993 wording removed the creature and its Auras from
 * the game). Structurally this is Banishing Light with phasing instead of exile:
 *  - ETB trigger phases the target creature out via [Effects.PhaseOutUntilLeaves] — linked to
 *    Oubliette so it stays out (skipping its untap-step phase-in), and flagged to tap on phase-in.
 *    Indirect phasing carries its Auras/Equipment out with it.
 *  - Oubliette's leaves-battlefield trigger phases the linked creature back in (tapped) via
 *    [Effects.PhaseInLinkedToSource].
 */
val Oubliette = card("Oubliette") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, target creature phases out until this enchantment " +
        "leaves the battlefield. Tap that creature as it phases in this way. (Auras and Equipment " +
        "phase out with it. While permanents are phased out, they're treated as though they don't exist.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature", TargetCreature())
        effect = Effects.PhaseOutUntilLeaves(creature, tapOnPhaseIn = true)
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.PhaseInLinkedToSource()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30d1450f-2909-410e-9920-731278fa74de.jpg?1562904037"
    }
}
