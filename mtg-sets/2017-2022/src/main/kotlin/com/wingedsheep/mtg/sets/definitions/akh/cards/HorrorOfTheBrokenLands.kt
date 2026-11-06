package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Horror of the Broken Lands
 * {4}{B}
 * Creature — Horror
 * 4/4
 * Whenever you cycle or discard another card, this creature gets +2/+1 until end of turn.
 * Cycling {B}
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, per the ruling. "Another" needs no filter:
 * the observer only functions on the battlefield, and a card is only ever discarded from hand.
 */
val HorrorOfTheBrokenLands = card("Horror of the Broken Lands") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    oracleText = "Whenever you cycle or discard another card, this creature gets +2/+1 until end of turn.\n" +
        "Cycling {B} ({B}, Discard this card: Draw a card.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.ModifyStats(2, 1, EffectTarget.Self)
        description = "Whenever you cycle or discard another card, this creature gets +2/+1 until end of turn."
    }

    keywordAbility(KeywordAbility.cycling("{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Daarken"
        flavorText = "It feeds on dry bones in the sand and desperate pleas in the wind."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35828576-124a-45d6-ad4c-af2926314953.jpg?1783936504"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
