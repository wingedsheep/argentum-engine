package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ill-Tempered Loner // Howlpack Avenger (Innistrad: Crimson Vow)
 * {2}{R}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Ill-Tempered Loner (3/3): "Whenever this creature is dealt damage, it deals that much damage to
 *          any target"; "{1}{R}: This creature gets +2/+0 until end of turn"; Daybound.
 * Back  — Howlpack Avenger (4/4): "Whenever a permanent you control is dealt damage, this creature deals
 *          that much damage to any target"; "{1}{R}: +2/+0 UEOT"; Nightbound.
 *
 * The front is the Screaming Nemesis rail — a self-damage retaliation trigger ([Triggers.TakesDamage],
 * a SELF-binding `DamageReceivedEvent`), reflecting the incoming amount via
 * `DynamicAmount.ContextProperty(TRIGGER_DAMAGE_AMOUNT)` back at any target. Unlike Screaming Nemesis it
 * hits *any* target (not "any **other**"), so it can even bounce at the source that struck it.
 *
 * The back widens the watch to "**a permanent you control** is dealt damage" — the Kazarov observer rail
 * ([Triggers.dealsDamage] with `recipient = RecipientFilter.PermanentYouControl` and
 * [TriggerBinding.ANY], which matches any `DealsDamageEvent` whose recipient is a permanent this creature's
 * controller controls, including the creature itself). It deals that much damage to any target, sourced
 * from itself (`damageSource = EffectTarget.Self`) so it reads as "**this creature** deals…". Each damage
 * event fires the ability separately (CR 731 werewolves don't batch), so simultaneous damage to two of
 * your permanents triggers twice.
 *
 * Both faces share the "{1}{R}: +2/+0 until end of turn" firebreathing ability. The back is a transformed
 * face with no mana cost, so its color comes from a color indicator (CR 204): `colorIndicator = "R"`.
 */

private val IllTemperedLonerFront = card("Ill-Tempered Loner") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature is dealt damage, it deals that much damage to any target.\n" +
        "{1}{R}: This creature gets +2/+0 until end of turn.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.TakesDamage
        val victim = target("any target", AnyTarget())
        effect = Effects.DealDamage(
            amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            target = victim,
            damageSource = EffectTarget.Self,
        )
        description = "This creature deals that much damage to any target."
    }
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "This creature gets +2/+0 until end of turn."
    }
    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3d1e90b-0c99-46da-b4f6-4b7be27dbd5c.jpg?1783924840"
    }
}

private val HowlpackAvenger = card("Howlpack Avenger") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Whenever a permanent you control is dealt damage, this creature deals that much damage " +
        "to any target.\n" +
        "{1}{R}: This creature gets +2/+0 until end of turn.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            recipient = RecipientFilter.PermanentYouControl,
            binding = TriggerBinding.ANY,
        )
        val victim = target("any target", AnyTarget())
        effect = Effects.DealDamage(
            amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            target = victim,
            damageSource = EffectTarget.Self,
        )
        description = "This creature deals that much damage to any target."
    }
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "This creature gets +2/+0 until end of turn."
    }
    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/back/f/3/f3d1e90b-0c99-46da-b4f6-4b7be27dbd5c.jpg?1783924840"
    }
}

val IllTemperedLoner: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = IllTemperedLonerFront,
    backFace = HowlpackAvenger,
)
