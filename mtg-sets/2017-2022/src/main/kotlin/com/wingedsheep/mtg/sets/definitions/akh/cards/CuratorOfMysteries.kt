package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Curator of Mysteries
 * {2}{U}{U}
 * Creature — Sphinx
 * 4/4
 * Flying
 * Whenever you cycle or discard another card, scry 1.
 * Cycling {U} ({U}, Discard this card: Draw a card.)
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card. "Another" needs no filter: the ability
 * only works while this creature is on the battlefield, and a card is only discarded from a hand,
 * so this card can never be the one discarded while its ability is active.
 */
val CuratorOfMysteries = card("Curator of Mysteries") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    oracleText = "Flying\n" +
        "Whenever you cycle or discard another card, scry 1.\n" +
        "Cycling {U} ({U}, Discard this card: Draw a card.)"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.Scry(1)
        description = "Whenever you cycle or discard another card, scry 1."
    }

    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Christine Choi"
        flavorText = "To consult a sphinx is a test in patience. Perhaps that's the point."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16caa0b6-dba9-4f93-bf8d-3b339474cbf1.jpg?1783936524"
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
