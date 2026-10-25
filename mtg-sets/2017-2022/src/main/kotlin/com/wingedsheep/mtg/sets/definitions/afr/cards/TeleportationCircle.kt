package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Teleportation Circle
 * {3}{W}
 * Enchantment
 *
 * At the beginning of your end step, exile up to one target artifact or creature you control, then
 * return that card to the battlefield under its owner's control.
 *
 * "Up to one" is an optional target: with none chosen the trigger does nothing. The return is a
 * plain move to the battlefield, which puts the card under its owner's control; an exiled token
 * ceases to exist and doesn't come back.
 */
val TeleportationCircle = card("Teleportation Circle") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, exile up to one target artifact or creature you " +
        "control, then return that card to the battlefield under its owner's control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val permanent = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl()),
            optional = true,
        )
        effect = Effects.Move(permanent, Zone.EXILE) then Effects.Move(permanent, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Chris Cold"
        flavorText = "\"Hold on to your lunch!\"\n—Bartleby, gnome wizard"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90140bc0-4a9c-4422-b07c-3400c7ccde56.jpg?1783926523"
        ruling("2026-01-27", "Once a permanent exiled this way returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2026-01-27", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
