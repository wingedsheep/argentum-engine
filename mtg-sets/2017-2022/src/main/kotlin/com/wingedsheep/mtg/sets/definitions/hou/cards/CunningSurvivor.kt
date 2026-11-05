package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cunning Survivor
 * {1}{U}
 * Creature — Human Warrior
 * 1/3
 * Whenever you cycle or discard a card, this creature gets +1/+0 until end of turn and can't be
 * blocked this turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, and wiring `cycles()` as well would
 * double-trigger against the ruling. Same shape as Grisly Survivor.
 */
val CunningSurvivor = card("Cunning Survivor") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Warrior"
    oracleText = "Whenever you cycle or discard a card, this creature gets +1/+0 until end of turn and can't be blocked this turn."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Whenever you cycle or discard a card, this creature gets +1/+0 until end of turn and can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Zoltan Boros"
        flavorText = "Initiates who had been through the Trial of Knowledge learned to see everything as pieces of a larger puzzle."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f9093e7-c129-4efa-9f25-a2366f92e15a.jpg?1783936052"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
