package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reito Sentinel — Kamigawa: Neon Dynasty #256
 * {3} · Artifact Creature — Construct · 3/3
 *
 * Defender
 * When this creature enters, target player mills three cards.
 * {3}: Put target card from a graveyard on the bottom of its owner's library.
 */
val ReitoSentinel = card("Reito Sentinel") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 3
    oracleText = "Defender\n" +
        "When this creature enters, target player mills three cards. " +
        "(They put the top three cards of their library into their graveyard.)\n" +
        "{3}: Put target card from a graveyard on the bottom of its owner's library."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        val t = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(t, Zone.LIBRARY, ZonePlacement.Bottom)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0218923a-b685-4492-8fdd-2d2d22ef532e.jpg?1783923821"
    }
}
