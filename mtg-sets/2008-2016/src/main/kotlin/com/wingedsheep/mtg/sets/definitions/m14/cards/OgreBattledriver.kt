package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ogre Battledriver — Magic 2014 #148
 * {2}{R}{R} · Creature — Ogre Warrior · 3/3
 *
 * Whenever another creature you control enters, that creature gets +2/+0 and gains haste until
 * end of turn.
 *
 * The Surrak and Goreclaw shape: an `another(...).enters()` trigger whose effects land on the
 * entering creature via [EffectTarget.TriggeringEntity].
 */
val OgreBattledriver = card("Ogre Battledriver") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever another creature you control enters, that creature gets +2/+0 and gains " +
        "haste until end of turn. (It can attack and {T} this turn.)"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(2, 0, EffectTarget.TriggeringEntity) then
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity, Duration.EndOfTurn)
        description = "Whenever another creature you control enters, that creature gets +2/+0 and " +
            "gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Greg Staples"
        flavorText = "Ogres are driven by passion, rage, and another ogre standing behind them with a whip."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bff2d740-22cc-4719-ac58-28621951e68d.jpg?1783939911"
    }
}
