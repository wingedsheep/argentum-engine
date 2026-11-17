package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shaman of the Great Hunt — Fate Reforged #113
 * {3}{R} · Creature — Orc Shaman · 4/2
 *
 * The counter trigger is one observer trigger on the Shaman (not a grant to your creatures), and
 * "it" is the creature that dealt the damage — [EffectTarget.TriggeringEntity]. Ferocious is only an
 * ability word here: the draw ability has no activation gate, it just counts creatures you control
 * with power 4 or greater (projected) as it resolves, so it may draw zero.
 */
val ShamanOfTheGreatHunt = card("Shaman of the Great Hunt") {
    manaCost = "{3}{R}"
    colorIdentity = "URG"
    typeLine = "Creature — Orc Shaman"
    power = 4
    toughness = 2
    oracleText = "Haste\n" +
        "Whenever a creature you control deals combat damage to a player, put a +1/+1 counter on it.\n" +
        "Ferocious — {2}{G/U}{G/U}: Draw a card for each creature you control with power 4 or greater."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        description = "Whenever a creature you control deals combat damage to a player, put a +1/+1 counter on it."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G/U}{G/U}")
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.powerAtLeast(4)).count()
        )
        description = "Draw a card for each creature you control with power 4 or greater."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "113"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfbd8cd1-465c-4501-94ba-5a3402f30ded.jpg?1783938685"
    }
}
