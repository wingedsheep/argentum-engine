package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Retreat to Coralhelm — Battle for Zendikar #82
 * {2}{U} · Enchantment
 *
 * Landfall — Whenever a land you control enters, choose one —
 * • You may tap or untap target creature.
 * • Scry 1.
 *
 * The Retreat to Hagra modal landfall trigger (mode chosen as the trigger goes on the stack,
 * CR 603.3c). The first mode is the Pestermite idiom inside it: the target is locked in with the
 * mode, then on resolution an [Effects.May] around a non-spell two-mode [Effects.Modal] picks tap or
 * untap — so an opponent tapping the creature in response just means you pick untap.
 */
val RetreatToCoralhelm = card("Retreat to Coralhelm") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, choose one —\n" +
        "• You may tap or untap target creature.\n" +
        "• Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = ModalEffect.chooseOne(
            mode("You may tap or untap target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.May(
                    Effects.Modal(
                        modes = listOf(
                            Mode.noTarget(Effects.Tap(creature), "Tap that creature"),
                            Mode.noTarget(Effects.Untap(creature), "Untap that creature")
                        ),
                        chooseCount = 1,
                        countsAsModalSpell = false
                    )
                )
            },
            Mode.noTarget(Effects.Scry(1), "Scry 1"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68684c8e-dc19-4a56-b511-4e21140b137f.jpg?1783938208"
    }
}
