package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Bishop of Binding
 * {3}{W}
 * Creature — Vampire Cleric
 * 1/1
 * When this creature enters, exile target creature an opponent controls until this creature
 * leaves the battlefield.
 * Whenever this creature attacks, target Vampire gets +X/+X until end of turn, where X is the
 * power of the exiled card.
 *
 * The "until" duration is the linked-exile pair ([Effects.ExileUntilLeaves] plus a leaves trigger
 * returning the pile). X reads the power of the card in that pile ([EffectTarget.LinkedExiledCard])
 * as the attack trigger resolves — 0 when nothing is exiled with it. "Target Vampire" is the bare
 * tribal noun, so any Vampire permanent is a legal target.
 */
val BishopOfBinding = card("Bishop of Binding") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Cleric"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, exile target creature an opponent controls until this creature " +
        "leaves the battlefield.\n" +
        "Whenever this creature attacks, target Vampire gets +X/+X until end of turn, where X is the power " +
        "of the exiled card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ExileUntilLeaves(t)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val vampire = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE)))
        val x = DynamicAmounts.propertyOf(EffectTarget.LinkedExiledCard(), EntityNumericProperty.Power)
        effect = Effects.ModifyStats(x, x, vampire)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaadceb3-8a57-45de-b3f1-aca94f20de18.jpg?1783935342"
    }
}
