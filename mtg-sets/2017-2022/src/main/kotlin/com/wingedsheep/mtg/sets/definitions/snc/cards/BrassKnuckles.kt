package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.AttachmentKind

/**
 * Brass Knuckles — Streets of New Capenna #234
 * {4} · Artifact — Equipment
 *
 * When you cast this spell, copy it. (The copy becomes a token.)
 * Equipped creature has double strike as long as two or more Equipment are attached to it.
 * Equip {1}
 *
 * The cast trigger is `Triggers.self.isCast()` copying the triggering spell — the same shape as
 * Sage of the Skies. Copying a permanent spell yields a token permanent (CR 707.10f), and the copy
 * is put on the stack rather than cast, so it never re-triggers itself.
 *
 * The double strike is gated on the number of Equipment attached to the *equipped creature*
 * (any Equipment, Brass Knuckles itself included), read through the source's attachment link with
 * [DynamicAmounts.attachmentsOnEnchantedCreature]`(AttachmentKind.EQUIPMENT)`.
 */
val BrassKnuckles = card("Brass Knuckles") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "When you cast this spell, copy it. (The copy becomes a token.)\n" +
        "Equipped creature has double strike as long as two or more Equipment are attached to it.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity, mayChooseNewTargets = false)
        description = "When you cast this spell, copy it."
    }

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.attachmentsOnEnchantedCreature(AttachmentKind.EQUIPMENT),
            ComparisonOperator.GTE,
            2
        )
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd31ee9e-1af7-4eb4-bda8-11451ebd6927.jpg?1783923065"
        ruling(
            "2022-04-29",
            "A copy of a permanent spell enters the battlefield as a token. This is not the same as an " +
                "effect that creates a token, and any effect that refers to creating a token does not " +
                "apply to copies of permanent spells."
        )
    }
}
