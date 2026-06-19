package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetSpell
import com.wingedsheep.sdk.model.Rarity

/**
 * Get Out
 * {U}{U}
 * Instant
 * Choose one —
 * • Counter target creature or enchantment spell.
 * • Return one or two target creatures and/or enchantments you own to your hand.
 *
 * Modal "choose one" (mirrors Amazing Acrobatics' ModalEffect shape):
 * - Mode 1 counters a target creature-or-enchantment spell on the stack
 *   (`TargetFilter.CreatureOrEnchantment` scoped to the stack).
 * - Mode 2 bounces one or two target creatures/enchantments you own
 *   (`TargetObject(count = 2, minCount = 1)` over the same type filter, owned by you),
 *   returning each via `ForEachTargetEffect(ReturnToHand)`.
 */
val GetOut = card("Get Out") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target creature or enchantment spell.\n" +
        "• Return one or two target creatures and/or enchantments you own to your hand."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode.withTarget(
                    effect = Effects.CounterSpell(),
                    target = TargetSpell(
                        filter = TargetFilter(GameObjectFilter.CreatureOrEnchantment, zone = Zone.STACK)
                    ),
                    description = "Counter target creature or enchantment spell."
                ),
                Mode.withTarget(
                    effect = ForEachTargetEffect(
                        listOf(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
                    ),
                    target = TargetObject(
                        count = 2,
                        minCount = 1,
                        filter = TargetFilter.CreatureOrEnchantment.ownedByYou()
                    ),
                    description = "Return one or two target creatures and/or enchantments you own to your hand."
                )
            ),
            chooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Mirko Failoni"
        flavorText = "Kaito's blade brought the light of another plane into Duskmourn, and with it, a brief chance to escape."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d9471c8-2db9-4ce3-a1e5-42b85134cb8e.jpg?1726286080"
    }
}
