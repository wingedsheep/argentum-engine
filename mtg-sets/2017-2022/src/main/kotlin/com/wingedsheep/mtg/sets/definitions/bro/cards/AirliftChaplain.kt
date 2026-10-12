package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Airlift Chaplain
 * {2}{W}
 * Creature — Human Cleric
 * 1/1
 *
 * Flying
 * When this creature enters, mill three cards. You may put a Plains card or a creature card
 * with mana value 3 or less from among the cards milled this way into your hand. If you
 * don't, put a +1/+1 counter on this creature.
 *
 * "If you don't" reads the picked collection: declining, or milling no eligible card, adds
 * the counter.
 */
val AirliftChaplain = card("Airlift Chaplain") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, mill three cards. You may put a Plains card or a creature card with mana value 3 or less from among the cards milled this way into your hand. If you don't, put a +1/+1 counter on this creature. (To mill a card, put the top card of your library into your graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val milled = mill(3)
            val picked = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Land.withSubtype(Subtype.PLAINS) or
                    GameObjectFilter.Creature.manaValueAtMost(3),
                showAllCards = true,
                prompt = "You may put a Plains card or a creature card with mana value 3 or less milled this way into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(picked)
            run(Effects.If(
                condition = Conditions.Not(whenMatches(picked)),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7ad55575-6053-43b1-9496-bcb1ea25e2a1.jpg?1783920134"
    }
}
