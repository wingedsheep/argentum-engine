package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Tempest
 * {1}{R}
 * Enchantment
 * Whenever a creature you control with flying enters, it gains haste until end of turn.
 * Whenever a Dragon you control enters, it deals X damage to any target, where X is the number of
 * Dragons you control.
 *
 * Both triggers are ANY-bound enters triggers handing the entering permanent on as the triggering
 * entity. The damage is dealt *by the Dragon* (`damageSource = TriggeringEntity`, as Warstorm Surge),
 * so it still deals damage from last-known information if it left before resolution; X counts the
 * Dragons you control as the ability resolves (2017-11-17 rulings).
 */
val DragonTempest = card("Dragon Tempest") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control with flying enters, it gains haste until end of turn.\n" +
        "Whenever a Dragon you control enters, it deals X damage to any target, where X is the number " +
        "of Dragons you control."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl()).enters()
        effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Dragon").youControl()).enters()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Dragon")).count(),
            target = any,
            damageSource = EffectTarget.TriggeringEntity,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cefac283-8491-4d11-9a40-0e90cbeac19f.jpg?1783938589"
        ruling("2017-11-17", "The amount of damage dealt by the Dragon that entered the battlefield is based on the number of Dragons you control when the ability resolves.")
        ruling("2017-11-17", "If a Dragon entering the battlefield causes Dragon Tempest's second ability to trigger but leaves the battlefield before that ability resolves, that Dragon still deals damage.")
        ruling("2017-11-17", "The two abilities aren't mutually exclusive. If a Dragon with flying (which is most of them) enters the battlefield under your control, both abilities will trigger.")
    }
}
