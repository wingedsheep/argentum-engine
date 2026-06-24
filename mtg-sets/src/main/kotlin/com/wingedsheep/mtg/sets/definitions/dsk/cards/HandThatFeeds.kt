package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hand That Feeds
 * {1}{R}
 * Creature — Mutant
 * 2/2
 *
 * Delirium — Whenever this creature attacks while there are four or more card types among
 * cards in your graveyard, it gets +2/+0 and gains menace until end of turn.
 *
 * Delirium is an ability word (no rules meaning of its own); the [Triggers.Attacks] trigger
 * carries an intervening-"if" gate of [Conditions.Delirium] (four+ distinct card types in your
 * graveyard), modeled like Wickerfolk Thresher. The payoff buffs the source itself
 * ([EffectTarget.Self]): +2/+0 via [Effects.ModifyStats] plus a menace [Effects.GrantKeyword],
 * both lasting until end of turn (the facade default duration).
 */
val HandThatFeeds = card("Hand That Feeds") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Mutant"
    power = 2
    toughness = 2
    oracleText = "Delirium — Whenever this creature attacks while there are four or more card " +
        "types among cards in your graveyard, it gets +2/+0 and gains menace until end of turn. " +
        "(It can't be blocked except by two or more creatures.)"

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.Delirium()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Loïc Canavaggia"
        flavorText = "In the darkness, Lana reached for Shay's hand and squeezed it for comfort. " +
            "Then she heard Shay call to her from the next room."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/297c2860-5a68-4a18-a23a-ca5cfdfbcac8.jpg?1726286367"
    }
}
