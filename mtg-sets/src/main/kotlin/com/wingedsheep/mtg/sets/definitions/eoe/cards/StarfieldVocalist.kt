package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Starfield Vocalist
 * {3}{U}
 * Creature — Human Bard
 * 3/4
 * If a permanent entering the battlefield causes a triggered ability of a permanent
 * you control to trigger, that ability triggers an additional time.
 * Warp {1}{U} (You may cast this card from your hand for its warp cost. Exile this
 * creature at the beginning of the next end step, then you may cast it from exile on
 * a later turn.)
 */
val StarfieldVocalist = card("Starfield Vocalist") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Bard"
    power = 3
    toughness = 4
    oracleText = "If a permanent entering the battlefield causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time.\n" +
        "Warp {1}{U} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    staticAbility {
        ability = AdditionalETBTriggers(
            enteringFilter = GameObjectFilter.Any,
            enteringMustBeYouControl = false,
            description = "If a permanent entering the battlefield causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time"
        )
    }

    warp = "{1}{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Nathaniel Himawan"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/deca0b2a-e7f3-444a-883d-7c41dd62c9cc.jpg?1752946868"
        ruling("2025-07-25", "Starfield Vocalist's first ability affects a permanent's own \"enters\" triggered abilities as well as other triggered abilities that trigger when that permanent enters the battlefield. Such triggered abilities start with \"when\" or \"whenever.\" Some keyword abilities also include a triggered ability that happens when a permanent enters the battlefield.")
        ruling("2025-07-25", "If you control two Starfield Vocalists, a permanent entering the battlefield causes abilities to trigger three times, not four. A third Starfield Vocalist causes abilities to trigger four times, a fourth causes abilities to trigger five times, and so on.")
        ruling("2025-07-25", "Abilities that apply \"as [this permanent] enters,\" such as Famished Worldsire's devour land ability, are also unaffected.")
        ruling("2025-07-25", "Replacement effects are unaffected by Starfield Vocalist's first ability. For example, a creature that enters the battlefield under your control with a +1/+1 counter on it won't receive an additional +1/+1 counter.")
        ruling("2025-07-25", "If a permanent entering the battlefield at the same time as Starfield Vocalist (including Starfield Vocalist itself) causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time.")
        ruling("2025-07-25", "Starfield Vocalist's first ability doesn't copy the triggered ability; it just causes the ability to trigger an additional time. Any choices made as you put the ability onto the stack, such as modes and targets, are made separately for each instance of the ability. Any choices made on resolution, such as whether to put counters on a permanent, are also made individually.")
    }
}
