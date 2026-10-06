package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Pyreling
 * {1}{R}
 * Creature — Elemental Lizard
 * 1/3
 * Whenever a source you control deals noncombat damage to an opponent, this creature gets +1/+0
 * and gains double strike until end of turn.
 *
 * "A source you control" is the Virtue of Courage / Niv-Mizzet, Visionary trigger shape.
 */
val ChandrasPyreling = card("Chandra's Pyreling") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Lizard"
    power = 1
    toughness = 3
    oracleText = "Whenever a source you control deals noncombat damage to an opponent, this creature " +
        "gets +1/+0 and gains double strike until end of turn. (It deals both first-strike and " +
        "regular combat damage.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl())
            .dealsDamage(Recipient.Opponent, damageType = DamageType.NonCombat)
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
        description = "Whenever a source you control deals noncombat damage to an opponent, this " +
            "creature gets +1/+0 and gains double strike until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Josu Hernaiz"
        flavorText = "\"I think Chandra just wanted something to blame random scorch marks on.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7744fcf-2336-489d-bc05-f3fce78713a9.jpg?1783930694"

        ruling(
            "2020-06-23",
            "Combat damage is the damage that's dealt automatically by attacking and blocking " +
                "creatures. Any other damage is noncombat damage, even if it's dealt during a " +
                "combat phase by an attacking or blocking creature."
        )
        ruling(
            "2020-06-23",
            "The ability of Chandra's Pyreling triggers just once per event for each opponent who " +
                "is dealt noncombat damage by a source you control, regardless of how much damage " +
                "that player is dealt. For example, if a sorcery you control deals 2 damage to each " +
                "opponent in a four-player game, the ability will trigger three times."
        )
    }
}
