package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Hotshot Investigators
 * {5}{U}
 * Creature — Vedalken Detective
 * 4/4
 *
 * When this creature enters, return up to one other target creature to its owner's hand.
 * If you controlled it, investigate.
 *
 * "Up to one other target creature" is the standard optional [TargetFilter.OtherCreature] bounce
 * (Matterbending Mage). The "if you controlled it" rider has to be tested *before* the bounce —
 * once the creature is in its owner's hand it has no controller — so the conditional wraps the
 * whole resolution rather than trailing it: control check, then bounce (+ investigate) on the
 * matching branch, plain bounce otherwise. With no target chosen the condition fails and the
 * else-branch bounce is a no-op, so declining the "up to one" investigates nothing.
 */
val HotshotInvestigators = card("Hotshot Investigators") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Detective"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, return up to one other target creature to its owner's " +
        "hand. If you controlled it, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "other creature",
            TargetCreature(
                optional = true,
                filter = TargetFilter.OtherCreature
            )
        )
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.youControl()),
            effect = Effects.Composite(
                Effects.ReturnToHand(creature),
                Effects.Investigate()
            ),
            elseEffect = Effects.ReturnToHand(creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Jodie Muir"
        flavorText = "They always think they're better than the local investigators. They're usually right."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dc3b23b-04d6-4ac0-b698-596ea90b7781.jpg?1783912912"
    }
}
