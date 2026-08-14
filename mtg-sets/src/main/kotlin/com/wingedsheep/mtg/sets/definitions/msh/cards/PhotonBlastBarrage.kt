package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Photon Blast Barrage — Marvel Super Heroes #147
 * {X}{R}{R} · Sorcery
 *
 * When you cast this spell, copy it X times. You may choose new targets for the copies.
 * Photon Blast Barrage deals 1 damage to target creature.
 *
 * Implementation notes:
 *  - The cast trigger is [Triggers.WhenYouCastThisSpell] (Social Snub / Sage of the Skies), which
 *    fires from the stack on this spell's own cast, so the copies are created — and resolve —
 *    before the original. [Effects.CopyTargetSpell] of [EffectTarget.TriggeringEntity] already
 *    means "copy that spell, you may choose new targets for the copy" and loops per copy,
 *    prompting for each copy's target separately (CR 707.10c).
 *  - `copies` is [DynamicAmounts.xValueOfTriggeringSpell] rather than `DynamicAmount.XValue`:
 *    inside a triggered ability, `XValue` reads the *trigger's own* announced X (an {X} cycling
 *    cost, a megamorph turn-up), which a cast trigger never has. The value announced for the
 *    triggering spell's {X} (CR 601.2b) rides on the `SpellCastEvent` and is read through
 *    `ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL` — the Geometer's Arthropod slot.
 *  - A copy is put on the stack rather than cast (CR 707.10), so it does not re-trigger this
 *    ability — X copies, not a cascade.
 */
val PhotonBlastBarrage = card("Photon Blast Barrage") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "When you cast this spell, copy it X times. You may choose new targets for the " +
        "copies.\n" +
        "Photon Blast Barrage deals 1 damage to target creature."

    // "When you cast this spell, copy it X times. You may choose new targets for the copies."
    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = Effects.CopyTargetSpell(
            target = EffectTarget.TriggeringEntity,
            copies = DynamicAmounts.xValueOfTriggeringSpell(),
        )
        description = "When you cast this spell, copy it X times. You may choose new targets " +
            "for the copies."
    }

    // "Photon Blast Barrage deals 1 damage to target creature."
    spell {
        target = Targets.Creature
        effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Immanuela Crovius"
        flavorText = "\"Try to outrun light, suckers.\"\n—Photon, Monica Rambeau"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f85a77d2-e11e-44bc-a1e7-d783cd49d714.jpg?1783902926"
    }
}
