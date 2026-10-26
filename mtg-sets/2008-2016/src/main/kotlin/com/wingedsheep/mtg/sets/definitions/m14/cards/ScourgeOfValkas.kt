package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
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
 * Scourge of Valkas
 * {2}{R}{R}{R}
 * Creature — Dragon
 * 4/4
 * Flying
 * Whenever this creature or another Dragon you control enters, it deals X damage to any target,
 * where X is the number of Dragons you control.
 * {R}: This creature gets +1/+0 until end of turn.
 *
 * "It" is the Dragon that entered, so the damage source is the triggering entity (Warstorm Surge's
 * shape) — its last-known information if it has already left. X counts Dragons on resolution.
 */
val ScourgeOfValkas = card("Scourge of Valkas") {
    manaCost = "{2}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever this creature or another Dragon you control enters, it deals X damage to any target, " +
        "where X is the number of Dragons you control.\n" +
        "{R}: This creature gets +1/+0 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Dragon").youControl()).enters()
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Dragon")).count(),
            target = any,
            damageSource = EffectTarget.TriggeringEntity,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "151"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27ce2b55-45bf-4852-a74a-d0b17c6c9c3f.jpg?1783939910"
        ruling("2017-11-17", "The amount of damage dealt by the Dragon that entered the battlefield is based on the number of Dragons you control when the ability resolves.")
        ruling("2017-11-17", "If a Dragon entering the battlefield causes Scourge of Valkas's ability to trigger but leaves the battlefield before that ability resolves, that Dragon still deals damage.")
    }
}
