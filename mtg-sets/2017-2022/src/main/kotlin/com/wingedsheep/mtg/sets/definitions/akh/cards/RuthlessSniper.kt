package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ruthless Sniper
 * {B}
 * Creature — Human Archer
 * 1/2
 * Whenever you cycle or discard a card, you may pay {1}. If you do, put a -1/-1 counter on target
 * creature.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, per the ruling. The target is chosen as the
 * trigger goes on the stack; the {1} is a single optional payment on resolution.
 */
val RuthlessSniper = card("Ruthless Sniper") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Archer"
    oracleText = "Whenever you cycle or discard a card, you may pay {1}. If you do, put a -1/-1 counter on target creature."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.discards()
        val creature = target(TargetFilter.Creature)
        effect = Effects.MayPay(
            ManaCost.parse("{1}"),
            Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
        )
        description = "Whenever you cycle or discard a card, you may pay {1}. If you do, put a -1/-1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Sara Winters"
        flavorText = "\"You may feel a slight sting.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d916306a-5cb3-4a38-9038-a433525c5b7b.jpg?1783936499"
        ruling("2017-04-18", "While resolving Ruthless Sniper's triggered ability, you can't pay {1} multiple times to put more counters on creatures.")
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
