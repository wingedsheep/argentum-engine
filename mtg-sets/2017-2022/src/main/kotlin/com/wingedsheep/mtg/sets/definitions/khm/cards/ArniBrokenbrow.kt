package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.plus

/**
 * Arni Brokenbrow — Kaldheim #120 (canonical printing)
 * {2}{R} · Legendary Creature — Human Berserker · 3/3
 *
 * Haste
 * Boast — {1}: You may have Arni's base power become 1 plus the greatest power among other
 * creatures you control until end of turn.
 *
 * The yes/no and the greatest-power read both happen as the boast resolves, and the value is a
 * snapshot (layer 7b set-base-power, not re-evaluated): later changes to the other creature don't
 * move Arni. The aggregate reads projected (actual) power, excludes Arni itself, yields 0 with no
 * other creatures (so base power 1) and keeps negative maxima (base power can go below 1).
 */
val ArniBrokenbrow = card("Arni Brokenbrow") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Berserker"
    power = 3
    toughness = 3
    oracleText = "Haste\nBoast — {1}: You may have Arni's base power become 1 plus the greatest power " +
        "among other creatures you control until end of turn. (Activate only if this creature attacked " +
        "this turn and only once each turn.)"

    keywords(Keyword.HASTE)

    activatedAbility {
        isBoast = true
        cost = Costs.Mana("{1}")
        effect = Effects.May(
            Effects.SetBasePower(
                target = EffectTarget.Self,
                power = 1 + DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature, excludeSelf = true).maxPower(),
                duration = Duration.EndOfTurn
            ),
            prompt = "Change Arni's base power to 1 plus the greatest power among other creatures you control?"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Dmitry Burmak"
        flavorText = "\"I headbutted a troll. And won.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff334bba-0805-4d5d-86c4-99185fe9a77a.jpg?1783928237"
        ruling("2021-02-05", "The greatest power among other creatures you control is determined as the boast ability resolves. You choose at that time whether you'd like to change Arni Brokenbrow's base power. If you control no other creatures at that time, you can change Arni's base power to 1 until end of turn. Arni objects to this course of action.")
        ruling("2021-02-05", "Any effects that modify Arni Brokenbrow's power without setting it to a specific value will apply after Arni's base power is set, regardless of the order in which those effects were created. The same is true of counters that modify Arni's power.")
        ruling("2021-02-05", "Arni Brokenbrow's new base power is set to the 1 plus the actual power of the other creature you control with the highest power, not that creature's base power. If that creature's power changes after the boast ability has resolved, Arni's power is unaffected.")
        ruling("2021-02-05", "In some unfortunate cases, the greatest power among other creatures you control may be negative. For example, if that greatest power is -3, you could change Arni's base power to -2 until end of turn. Arni strenuously objects to this course of action.")
    }
}
