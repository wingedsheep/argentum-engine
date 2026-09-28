package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tiller of Flesh
 * {3}{W}
 * Creature — Phyrexian Knight
 * 2/4
 *
 * Whenever you cast a spell that targets one or more permanents, incubate 2.
 *
 * "Permanents" means objects on the battlefield (CR 110.1), so the target filter is pinned to the
 * battlefield: a spell that only targets a permanent *spell* on the stack, a player, or a card in a
 * graveyard doesn't trigger it. Fires once per spell regardless of how many permanents it targets.
 */
val TillerOfFlesh = card("Tiller of Flesh") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Knight"
    oracleText = "Whenever you cast a spell that targets one or more permanents, incubate 2. " +
        "(Create an Incubator token with two +1/+1 counters on it and \"{2}: Transform this token.\" " +
        "It transforms into a 0/0 Phyrexian artifact creature.)"
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.casts(
            requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Permanent.onBattlefield()))
        )
        effect = Effects.Incubate(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49e206f2-1647-4456-8f2f-b67d053413e2.jpg?1783917044"
    }
}
