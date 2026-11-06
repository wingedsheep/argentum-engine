package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pitiless Vizier
 * {3}{B}
 * Creature — Minotaur Cleric
 * 4/2
 * Whenever you cycle or discard a card, this creature gains indestructible until end of turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card, so
 * `Triggers.you.discards()` fires once per cycled card, per the ruling.
 */
val PitilessVizier = card("Pitiless Vizier") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Minotaur Cleric"
    oracleText = "Whenever you cycle or discard a card, this creature gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it. If its toughness is 0 or less, it still dies.)"
    power = 4
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        description = "Whenever you cycle or discard a card, this creature gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22eb0615-d1b2-4128-bad5-bcff83672f6b.jpg?1783936501"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
