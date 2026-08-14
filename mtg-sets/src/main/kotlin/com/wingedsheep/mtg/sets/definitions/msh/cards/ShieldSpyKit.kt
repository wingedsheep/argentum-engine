package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * S.H.I.E.L.D. Spy Kit — Marvel Super Heroes #36
 * {W} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1.
 * Whenever equipped creature attacks alone, untap it and scry 1.
 * Equip {1}
 *
 * All three lines are stock Equipment primitives:
 *  - the pump is a [ModifyStats] scoped to [Filters.EquippedCreature];
 *  - the trigger lives on the Equipment and binds to the attached creature
 *    ([TriggerBinding.ATTACHED]) with the "attacks alone" predicate
 *    ([AttackPredicate.Alone] — `AttachmentTriggerDetector` applies the attack predicates on
 *    the ATTACHED path too, the Bilbo's Ring shape). "Untap it" is the attacking equipped
 *    creature ([EffectTarget.EquippedCreature], the Genji Glove idiom), and the scry is the
 *    Equipment controller's ([Patterns.Library.scry], CR 701.22).
 */
val ShieldSpyKit = card("S.H.I.E.L.D. Spy Kit") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature attacks alone, untap it and scry 1. (Look at the top card of " +
        "your library. You may put that card on the bottom.)\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attacks(
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ATTACHED,
        )
        effect = Effects.Untap(EffectTarget.EquippedCreature) then Patterns.Library.scry(1)
        description = "Whenever equipped creature attacks alone, untap it and scry 1."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Bachzim"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/4938e23f-b5da-493b-9904-af61a3733ba0.jpg?1783902967"
    }
}
