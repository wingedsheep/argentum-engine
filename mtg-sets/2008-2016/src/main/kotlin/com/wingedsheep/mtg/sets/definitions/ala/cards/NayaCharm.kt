package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Naya Charm
 * {R}{G}{W}
 * Instant
 *
 * Choose one —
 * • Naya Charm deals 3 damage to target creature.
 * • Return target card from a graveyard to its owner's hand.
 * • Tap all creatures target player controls.
 *
 * A plain choose-one `modal(chooseCount = 1)`. The recursion mode targets any graveyard's card
 * ([TargetFilter.CardInGraveyard]) and moves it to its owner's hand; the tap mode iterates the
 * target player's creatures (Rustler Rampage's `targetPlayerControls` group).
 */
val NayaCharm = card("Naya Charm") {
    manaCost = "{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Naya Charm deals 3 damage to target creature.\n" +
        "• Return target card from a graveyard to its owner's hand.\n" +
        "• Tap all creatures target player controls."

    spell {
        modal(chooseCount = 1) {
            mode("Naya Charm deals 3 damage to target creature") {
                val t = target(TargetFilter.Creature)
                effect = Effects.DealDamage(3, t)
            }
            mode("Return target card from a graveyard to its owner's hand") {
                val t = target(TargetFilter.CardInGraveyard)
                effect = Effects.Move(t, Zone.HAND)
            }
            mode("Tap all creatures target player controls") {
                val player = target(Targets.Player)
                effect = Patterns.Group.tapAll(
                    GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Jesper Ejsing"
        flavorText = "Deep in nature's core lies a potential unsullied by greed or civilization."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47748c3a-c60a-4f2e-9eb8-2675c067bb9c.jpg?1783942543"
        ruling("2008-10-01", "You can choose a mode only if you can choose legal targets for that mode. If you can't choose legal targets for any of the modes, you can't cast the spell.")
        ruling("2008-10-01", "While the spell is on the stack, treat it as though its only text is the chosen mode. The other two modes are treated as though they don't exist. You don't choose targets for those modes.")
        ruling("2008-10-01", "If this spell is copied, the copy will have the same mode as the original.")
    }
}
