package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Peerless Ropemaster
 * {4}{U}
 * Creature — Human Rogue
 * 4/4
 *
 * When this creature enters, return up to one target tapped creature to its owner's hand.
 *
 * "Up to one target" is an optional single-target return ([TargetCreature] with `optional = true`),
 * so the controller may decline to target anything (no creature is returned and the ability still
 * resolves). The target must be tapped when chosen; if it stops being legal before resolution the
 * ability fizzles.
 */
val PeerlessRopemaster = card("Peerless Ropemaster") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, return up to one target tapped creature to its owner's hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("creature", TargetCreature(filter = TargetFilter.TappedCreature, optional = true))
        effect = Effects.ReturnToHand(t)
        description = "When this creature enters, return up to one target tapped creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Wayne Wu"
        flavorText = "\"Murder someone and you'll have a gaggle of folks lining up for revenge. " +
            "Humiliate them, and they won't dare bother you again.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae044509-2f31-4506-a124-0e445b7181a2.jpg?1712355469"
    }
}
