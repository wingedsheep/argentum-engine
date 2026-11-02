package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Miles O'Brien, Transporter Ops
 * {2}{W}
 * Legendary Creature — Human Artificer
 * 3/3
 *
 * When Miles O'Brien enters, return up to one other target creature or artifact you control to
 * its owner's hand.
 * At the beginning of each end step, if a permanent was returned to your hand from the
 * battlefield this turn, draw a card.
 *
 * The end-step gate reads owner-keyed turn history: any permanent you own counts, whoever bounced
 * it and whoever controlled it, and it need not still be in your hand.
 */
val MilesOBrienTransporterOps = card("Miles O'Brien, Transporter Ops") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Artificer"
    power = 3
    toughness = 3
    oracleText = "When Miles O'Brien enters, return up to one other target creature or artifact you control to " +
        "its owner's hand.\n" +
        "At the beginning of each end step, if a permanent was returned to your hand from the battlefield " +
        "this turn, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.CreatureOrArtifact.youControl().other(), optional = true)
        effect = Effects.ReturnToHand(permanent)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.PermanentPutIntoZoneFromBattlefieldThisTurn(Zone.HAND)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "John Tyler Christopher"
        flavorText = "\"The confinement beam subsystems check out, so do the phase transition coils.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4f041b3-e785-45ba-9a7e-33878614d3b0.jpg?1793361164"
    }
}
