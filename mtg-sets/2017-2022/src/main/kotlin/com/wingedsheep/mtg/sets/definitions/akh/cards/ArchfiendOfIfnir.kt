package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Archfiend of Ifnir
 * {3}{B}{B}
 * Creature — Demon
 * 5/4
 * Flying
 * Whenever you cycle or discard another card, put a -1/-1 counter on each creature your opponents control.
 * Cycling {2}
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, per the ruling. "Another" needs no filter:
 * the observer only functions on the battlefield, and a card is only ever discarded from hand.
 */
val ArchfiendOfIfnir = card("Archfiend of Ifnir") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "Flying\n" +
        "Whenever you cycle or discard another card, put a -1/-1 counter on each creature your opponents control.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.opponentControls()),
            Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "Whenever you cycle or discard another card, put a -1/-1 counter on each creature your opponents control."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33176679-d571-47a1-ae05-bed9b748491d.jpg?1783936512"
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
    }
}
