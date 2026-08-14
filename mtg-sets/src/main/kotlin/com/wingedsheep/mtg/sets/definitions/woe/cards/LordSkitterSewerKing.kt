package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lord Skitter, Sewer King
 * {2}{B}
 * Legendary Creature — Rat Noble
 * 3/3
 *
 * Whenever another Rat you control enters, exile up to one target card from an opponent's
 * graveyard.
 * At the beginning of combat on your turn, create a 1/1 black Rat creature token with "This token
 * can't block."
 *
 * Two independent triggers:
 *
 * 1. The Rat-tribal graveyard hate uses [TriggerBinding.OTHER] so Lord Skitter's own arrival never
 *    fires it ("another Rat"), filtered to Rats you control. "Up to one target" is an optional
 *    target, so the ability still resolves doing nothing when no card is chosen or the chosen card
 *    has already left the graveyard. [Effects.Exile] is gated on [Zone.GRAVEYARD] per CR 400.7 —
 *    a card that changed zones in response is a new object and must not be exiled.
 *
 * 2. The begin-combat trigger makes WOE's shared type-named Rat token via [woeRatToken], the same
 *    1/1 black Rat with "This token can't block" that Edgewall Pack and Harried Spearguard create.
 *    Each token is itself "another Rat you control" entering, so it feeds trigger 1 on the next
 *    turn's combat — and any other Rat-enters payoff immediately.
 */
val LordSkitterSewerKing = card("Lord Skitter, Sewer King") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Rat Noble"
    power = 3
    toughness = 3
    oracleText = "Whenever another Rat you control enters, exile up to one target card from an " +
        "opponent's graveyard.\n" +
        "At the beginning of combat on your turn, create a 1/1 black Rat creature token with " +
        "\"This token can't block.\""

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.RAT).youControl(),
            binding = TriggerBinding.OTHER,
        )
        val card = target(
            "up to one target card in an opponent's graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Any.ownedByOpponent(), zone = Zone.GRAVEYARD),
            ),
        )
        effect = Effects.Exile(card, fromZone = Zone.GRAVEYARD)
        description = "Whenever another Rat you control enters, exile up to one target card from " +
            "an opponent's graveyard."
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = woeRatToken()
        description = "At the beginning of combat on your turn, create a 1/1 black Rat creature " +
            "token with \"This token can't block.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/729877be-4894-4ef5-9e60-de8a8fb2bdc0.jpg?1783915106"
    }
}
