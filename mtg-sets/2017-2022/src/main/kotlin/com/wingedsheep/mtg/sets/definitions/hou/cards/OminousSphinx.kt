package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ominous Sphinx
 * {3}{U}{U}
 * Creature — Sphinx
 * 4/4
 * Flying
 * Whenever you cycle or discard a card, target creature an opponent controls gets -2/-0 until end of turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, and wiring `cycles()` as well would
 * double-trigger against the ruling. Same shape as Grisly Survivor.
 */
val OminousSphinx = card("Ominous Sphinx") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    oracleText = "Flying\nWhenever you cycle or discard a card, target creature an opponent controls gets -2/-0 until end of turn."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.discards()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(-2, 0, creature)
        description = "Whenever you cycle or discard a card, target creature an opponent controls gets -2/-0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Lars Grant-West"
        flavorText = "The sphinxes began to speak only after the Second Sun aligned. They whispered dire warnings, though no one listened."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2361bd3b-9ae0-4198-9296-6d278feff042.jpg?1783936049"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
