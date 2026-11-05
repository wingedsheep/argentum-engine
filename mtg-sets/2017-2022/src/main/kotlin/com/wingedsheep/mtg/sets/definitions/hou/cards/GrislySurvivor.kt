package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grisly Survivor
 * {2}{B}
 * Creature — Minotaur Warrior
 * 2/3
 * Whenever you cycle or discard a card, this creature gets +2/+0 until end of turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, and wiring `cycles()` as well would
 * double-trigger against the ruling. Same shape as Flameblade Adept.
 */
val GrislySurvivor = card("Grisly Survivor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Minotaur Warrior"
    oracleText = "Whenever you cycle or discard a card, this creature gets +2/+0 until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Whenever you cycle or discard a card, this creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Seb McKinnon"
        flavorText = "No cartouches to earn. No afterlife to enter. Only the glory of a well-struck blow and the growing pile of felled enemies at her feet."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/035ee44d-63ef-45b7-b36b-38ac341a2ca7.jpg?1783936041"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
