package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Escape Protocol
 * {1}{U}
 * Enchantment
 * Whenever you cycle a card, you may pay {1}. When you do, exile target artifact or creature you
 * control, then return it to the battlefield under its owner's control.
 *
 * The optional {1} payment is the reflexive action (Passenger Ferry's shape); the target is chosen
 * only when the payment is made. The flicker is Momentary Blink's Exile-then-Move composition.
 */
val EscapeProtocol = card("Escape Protocol") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cycle a card, you may pay {1}. When you do, exile target artifact or creature " +
        "you control, then return it to the battlefield under its owner's control."

    triggeredAbility {
        trigger = Triggers.you.cycles()
        effect = Effects.ReflexiveTrigger(
            // "you may pay {1}"
            action = Effects.PayMana("{1}"),
            optional = true) {
            // "When you do, exile target artifact or creature you control, then return it ..."
            val permanent = target(TargetFilter.CreatureOrArtifact.youControl())
            effect = Effects.Exile(permanent) then Effects.Move(permanent, Zone.BATTLEFIELD)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Deruchenko Alexander"
        flavorText = "Sanctuaries harnessed the glowstones' natural reaction to monsters to create a warning system."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d3eeddd-c86d-4f35-ba57-3caa9565fcb0.jpg?1783931076"
    }
}
