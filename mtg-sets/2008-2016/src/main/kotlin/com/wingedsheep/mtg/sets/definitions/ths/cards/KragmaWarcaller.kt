package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kragma Warcaller
 * {3}{B}{R}
 * Creature — Minotaur Warrior
 * 2/3
 * Minotaur creatures you control have haste.
 * Whenever a Minotaur you control attacks, it gets +2/+0 until end of turn.
 *
 * Both abilities include Kragma Warcaller itself (it is a Minotaur). One trigger per attacking
 * Minotaur; the pump lands on the triggering attacker.
 */
val KragmaWarcaller = card("Kragma Warcaller") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Minotaur Warrior"
    power = 2
    toughness = 3
    oracleText = "Minotaur creatures you control have haste.\n" +
        "Whenever a Minotaur you control attacks, it gets +2/+0 until end of turn."

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(GameObjectFilter.Creature.withSubtype("Minotaur").youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Minotaur").youControl()).attacks()
        effect = Effects.ModifyStats(2, 0, EffectTarget.TriggeringEntity)
        description = "Whenever a Minotaur you control attacks, it gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Gabor Szikszai"
        flavorText = "A warcaller merely brings the herd together. After that, the meat-hunger is all " +
            "the encouragement they need."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78791fc6-054f-45eb-b8b0-28f2512d72b6.jpg?1783939730"
    }
}
