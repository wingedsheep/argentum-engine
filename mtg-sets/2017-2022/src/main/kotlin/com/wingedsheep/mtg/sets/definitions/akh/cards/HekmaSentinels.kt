package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hekma Sentinels — Amonkhet #56
 * {2}{U} · Creature — Human Cleric · Common
 * 2/3
 *
 * Whenever you cycle or discard a card, this creature gets +1/+1 until end of turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card, so
 * `Triggers.you.discards()` fires once per cycled card.
 */
val HekmaSentinels = card("Hekma Sentinels") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "Whenever you cycle or discard a card, this creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Whenever you cycle or discard a card, this creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "James Ryman"
        flavorText = "\"The God-Pharaoh created the Hekma to protect us from the horrors beyond and to surround us in his love.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9b7e690-e5f7-4d6e-b756-22aea959a612.jpg?1783936521"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
