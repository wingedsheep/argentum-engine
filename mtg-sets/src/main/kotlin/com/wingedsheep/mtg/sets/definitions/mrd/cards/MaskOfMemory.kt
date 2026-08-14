package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Mask of Memory — Mirrodin #203
 * {2} · Artifact — Equipment
 *
 * Whenever equipped creature deals combat damage to a player, you may draw two cards. If you do,
 * discard a card.
 * Equip {1}
 *
 * The "if you do" isn't a second choice — declining the draw is the only out, so the whole
 * draw-then-discard package sits inside one [MayEffect]. Accepting always costs the discard, which
 * is what makes this a filtering Equipment rather than raw card advantage.
 */
val MaskOfMemory = card("Mask of Memory") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature deals combat damage to a player, you may draw two cards. " +
        "If you do, discard a card.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            binding = TriggerBinding.ATTACHED,
        )
        effect = MayEffect(
            Effects.Composite(
                listOf(
                    Effects.DrawCards(2),
                    Effects.Discard(1),
                )
            )
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/512f8021-aaf3-4b1c-b08c-fe667ce2d8e1.jpg?1783944513"
    }
}
