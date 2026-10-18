package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Light the Way — Kamigawa: Neon Dynasty #24 (canonical printing)
 * {W} · Instant
 *
 * Choose one —
 * • Put a +1/+1 counter on target creature or Vehicle. Untap it.
 * • Return target permanent you control to its owner's hand.
 *
 * "Creature or Vehicle" is [GameObjectFilter.CreatureOrVehicle], so an uncrewed Vehicle is a legal
 * target and keeps its counter for when it next becomes a creature.
 */
val LightTheWay = card("Light the Way") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Put a +1/+1 counter on target creature or Vehicle. Untap it.\n" +
        "• Return target permanent you control to its owner's hand."

    spell {
        modal {
            mode("Put a +1/+1 counter on target creature or Vehicle. Untap it.") {
                val t = target(TargetFilter(GameObjectFilter.CreatureOrVehicle))
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then Effects.Untap(t)
            }
            mode("Return target permanent you control to its owner's hand.") {
                val t = target(TargetFilter.PermanentYouControl)
                effect = Effects.ReturnToHand(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Benjamin Ee"
        flavorText = "\"That way lies death. This way, a chance.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a2fd960-c4ec-4dfa-a61d-f8577b9d3519.jpg?1783923919"
    }
}
