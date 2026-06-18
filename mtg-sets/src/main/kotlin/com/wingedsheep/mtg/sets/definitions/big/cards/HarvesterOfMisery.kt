package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harvester of Misery
 * {3}{B}{B}
 * Creature — Spirit
 * 5/4
 *
 * Menace
 * When this creature enters, other creatures get -2/-2 until end of turn.
 * {1}{B}, Discard this card: Target creature gets -2/-2 until end of turn.
 */
val HarvesterOfMisery = card("Harvester of Misery") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 5
    toughness = 4
    oracleText = "Menace\n" +
        "When this creature enters, other creatures get -2/-2 until end of turn.\n" +
        "{1}{B}, Discard this card: Target creature gets -2/-2 until end of turn."

    keywords(Keyword.MENACE)

    // When this enters, all OTHER creatures get -2/-2 until end of turn.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature, excludeSelf = true),
            Effects.ModifyStats(-2, -2, EffectTarget.Self)
        )
    }

    // {1}{B}, Discard this card (from hand): Target creature gets -2/-2 until end of turn.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.DiscardSelf)
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(-2, -2, t)
        activateFromZone = Zone.HAND
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3012af9-621d-4fae-b00d-079a89ae35fe.jpg?1739804174"
    }
}
