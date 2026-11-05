package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gilded Cerodon — Hour of Devastation #94 (canonical printing)
 * {4}{R} · Creature — Beast · 4/4
 *
 * Whenever this creature attacks, if you control a Desert or there is a Desert card in your
 * graveyard, target creature can't block this turn.
 *
 * The Desert clause is an intervening-if (CR 603.4): checked as the attack trigger fires and
 * again on resolution, and either half may satisfy each check.
 */
val GildedCerodon = card("Gilded Cerodon") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature attacks, if you control a Desert or there is a Desert card in your graveyard, " +
        "target creature can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.Any(
            Conditions.ControlPermanentOfType(Subtype.DESERT),
            Conditions.GraveyardContainsSubtype(Subtype.DESERT),
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Matt Stewart"
        flavorText = "The plating of its horns is a token of honor from Hazoret, who favors creatures with great hearts."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f68c8fbd-9223-447d-a85c-fa6222c75277.jpg?1783936030"
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn’t matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
        ruling("2017-07-14", "For abilities that trigger only if you control a Desert or there is a Desert card in your graveyard, one condition must be true as the ability triggers and one must be true as the ability resolves. They don’t have to be the same condition, though. For example, you could sacrifice your only Desert after the ability triggers but before it has resolved.")
    }
}
