package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm God's Oracle — Modern Horizons 2 #213
 * {1}{U}{R} · Enchantment Creature — Human Shaman · 1 / 3
 *
 * {1}: This creature gets +1/-1 until end of turn.
 * When this creature dies, it deals 3 damage to any target.
 *
 * The pump is self-targeting ([EffectTarget.Self]), so it needs no target requirement — a
 * repeatable colorless activation that trades toughness for power, and can kill the Oracle
 * outright, which is the point: doing so sets off the death trigger.
 *
 * The dies trigger is [Triggers.Dies], the same battlefield-to-graveyard zone change Mudbutton
 * Torchrunner uses. Its damage source is resolved from the zone-change event's last-known
 * information rather than the live entity — the Oracle is already in the graveyard when the
 * trigger resolves — so the effect is a plain [Effects.DealDamage] and the engine supplies the
 * source.
 */
val StormGodsOracle = card("Storm God's Oracle") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "RU"
    typeLine = "Enchantment Creature — Human Shaman"
    power = 1
    toughness = 3
    oracleText = "{1}: This creature gets +1/-1 until end of turn.\n" +
        "When this creature dies, it deals 3 damage to any target."

    triggeredAbility {
        trigger = Triggers.Dies
        val damaged = target("any target", Targets.Any)
        effect = Effects.DealDamage(3, damaged)
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Pauline Voss"
        flavorText = "The lives of Keranos's chosen are brief and brilliant as lightning."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6d22f24-f752-4bc8-ba97-061b2c060ec8.jpg?1783926810"
    }
}
