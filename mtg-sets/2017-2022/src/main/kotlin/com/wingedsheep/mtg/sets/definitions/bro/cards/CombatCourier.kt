package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity

/**
 * Combat Courier — The Brothers' War #77
 * {1} · Artifact Creature — Construct · Common
 * 1/1
 *
 * {2}, Sacrifice this creature: Draw a card.
 * Unearth {U}
 */
val CombatCourier = card("Combat Courier") {
    manaCost = "{1}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "{2}, Sacrifice this creature: Draw a card.\n" +
        "Unearth {U} ({U}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
        description = "{2}, Sacrifice this creature: Draw a card."
    }

    unearth("{U}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/171edf80-ffc1-4894-9be5-c3e93a96f734.jpg?1783920099"
        ruling("2022-10-14", "If you activate a card's unearth ability but that card is removed from your graveyard before the ability resolves, that unearth ability will do nothing as it resolves.")
        ruling("2022-10-14", "Unearth grants haste to the permanent that's returned to the battlefield (even if it's not a creature card). However, neither of the \"exile\" abilities is granted to that permanent. If that permanent loses all its abilities, it will still be exiled at the beginning of the next end step, and if it would leave the battlefield, it is still exiled instead.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it!")
    }
}
