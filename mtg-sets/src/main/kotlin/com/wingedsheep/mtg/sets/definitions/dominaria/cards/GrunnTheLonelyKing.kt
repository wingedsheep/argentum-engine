package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grunn, the Lonely King
 * {4}{G}{G}
 * Legendary Creature — Ape Warrior
 * 5/5
 * Kicker {3}
 * If Grunn was kicked, it enters with five +1/+1 counters on it.
 * Whenever Grunn attacks alone, double its power and toughness until end of turn.
 */
val GrunnTheLonelyKing = card("Grunn, the Lonely King") {
    manaCost = "{4}{G}{G}"
    typeLine = "Legendary Creature — Ape Warrior"
    power = 5
    toughness = 5
    oracleText = "Kicker {3}\nIf Grunn, the Lonely King was kicked, it enters with five +1/+1 counters on it.\nWhenever Grunn attacks alone, double its power and toughness until end of turn."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{3}")))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 5, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.AttacksAlone
        effect = Effects.ModifyStats(DynamicAmounts.sourcePower(), DynamicAmounts.sourceToughness(), EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6a9aa5c-1501-4958-872a-39c512514033.jpg?1562741677"
        ruling("2018-04-27", "If a creature's power is less than 0 when it's doubled, instead that creature gets -X/-0, where X is how much less than 0 its power is.")
        ruling("2018-04-27", "A creature attacks alone if it's the only creature declared as an attacker during the declare attackers step (including creatures controlled by your teammates, if applicable).")
        ruling("2018-04-27", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply.")
    }
}
