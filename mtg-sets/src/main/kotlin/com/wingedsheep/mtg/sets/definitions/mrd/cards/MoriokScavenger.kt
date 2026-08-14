package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Moriok Scavenger — Mirrodin #68
 * {3}{B} · Creature — Human Rogue · 2/3
 *
 * When this creature enters, you may return target artifact creature card from your graveyard
 * to your hand.
 *
 * A [Gravedigger][com.wingedsheep.mtg.sets.definitions.por.cards.Gravedigger] narrowed to
 * Mirrodin's own currency: the graveyard card must be an *artifact creature*, not just any
 * creature, so a plain Myr-less board gets nothing back.
 *
 * The "you may" is `optional = true` on the trigger — the controller declines before the
 * return, matching CR 603.1 (the choice is made on resolution, after the target is locked in
 * at trigger time). The target itself is mandatory: the ability still needs a legal artifact
 * creature card in the graveyard to go on the stack at all.
 */
val MoriokScavenger = card("Moriok Scavenger") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you may return target artifact creature card " +
        "from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val card = target(
            "card",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.ArtifactCreature.ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Move(target = card, destination = Zone.HAND)
        description = "When this creature enters, you may return target artifact creature card " +
            "from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Puddnhead"
        flavorText = "Many go to Mephidross in search of lost riches. Most end up as part of the cache."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f27426b-3679-44e4-9249-f57b92baa3f3.jpg?1783944546"
    }
}
