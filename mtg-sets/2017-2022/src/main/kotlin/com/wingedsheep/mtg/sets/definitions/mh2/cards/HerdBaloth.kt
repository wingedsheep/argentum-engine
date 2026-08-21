package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Herd Baloth — Modern Horizons 2 #165
 * {3}{G}{G} · Creature — Beast · 4 / 4
 *
 * Whenever one or more +1/+1 counters are put on this creature, you may create a 4/4 green Beast
 * creature token.
 *
 * [Triggers.countersPlacedOn] is a per-*batch* watcher, which is exactly what "one or more … are
 * put on" asks for: five counters arriving from one resolution make one token, not five.
 * [TriggerBinding.SELF] scopes it to the Baloth itself, so the event filter can stay
 * [GameObjectFilter.Any].
 *
 * `firstTimeEachTurn` **defaults to `true`** on that facade (it is derived from `batch`), which
 * would silently drop every counter batch after the first each turn — hence the explicit `false`.
 *
 * `optional = true` is the authoring shorthand for "you may": the builder lowers it into a
 * consent gate around the token creation, so the controller is asked on resolution.
 */
val HerdBaloth = card("Herd Baloth") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Whenever one or more +1/+1 counters are put on this creature, you may create a 4/4 green Beast creature token."

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Any,
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            firstTimeEachTurn = false,
            binding = TriggerBinding.SELF,
        )
        optional = true
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Nicholas Gregory"
        flavorText = "Baloth social structure is simple: the biggest one's the leader."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1e9cef5-c55f-47d9-9d2f-300dab8fcb0b.jpg?1783926829"
    }
}
