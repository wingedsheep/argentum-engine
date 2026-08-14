package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ash, Party Crasher
 * {R}{W}
 * Legendary Creature — Human Peasant
 * 2/2
 *
 * Haste
 * Celebration — Whenever Ash attacks, if two or more nonland permanents entered the battlefield
 * under your control this turn, put a +1/+1 counter on Ash.
 *
 * The triggered half of the Celebration ability word (CR 207.2c — italic flavor, no rules meaning),
 * the [BelligerentOfTheBall] shape bound to an attack instead of the begin-combat step: an
 * intervening-'if' clause (CR 603.4), so [Conditions.Celebration] is checked both when attackers are
 * declared and again as the ability resolves.
 *
 * Untargeted — "put a +1/+1 counter on Ash" refers to the source itself, hence
 * [EffectTarget.Self] rather than a target requirement. Ash's own haste means it can attack the turn
 * it lands, and Ash itself is a nonland permanent that entered this turn, so one more permanent
 * (a token, a second creature) already turns the counter on.
 */
val AshPartyCrasher = card("Ash, Party Crasher") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Peasant"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "Celebration — Whenever Ash attacks, if two or more nonland permanents entered the " +
        "battlefield under your control this turn, put a +1/+1 counter on Ash."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.Celebration
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever Ash attacks, if two or more nonland permanents entered the " +
            "battlefield under your control this turn, put a +1/+1 counter on Ash."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Jason Rainville"
        flavorText = "\"Get up and dance! Let your feet thunder like forgehammers!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d51e6610-25b6-4d8e-92d7-c50a2ff844ff.jpg?1783915073"

        ruling(
            "2023-09-01",
            "Celebration abilities only care if two or more nonland permanents entered the " +
                "battlefield under your control in a turn. They won't get more powerful if more " +
                "than two permanents entered the battlefield under your control in a turn."
        )
        ruling(
            "2023-09-01",
            "The permanents that entered the battlefield don't need to remain on the battlefield " +
                "or under your control. Celebration abilities are checking for past events, not " +
                "the current game state."
        )
    }
}
