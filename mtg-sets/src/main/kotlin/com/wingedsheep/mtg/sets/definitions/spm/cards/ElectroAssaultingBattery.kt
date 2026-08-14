package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RetainUnspentColoredMana
import com.wingedsheep.sdk.scripting.effects.MayPayXForEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Electro, Assaulting Battery — Marvel's Spider-Man #76
 * {1}{R}{R} · Legendary Creature — Human Villain · 2/3
 *
 * Flying
 * You don't lose unspent red mana as steps and phases end.
 * Whenever you cast an instant or sorcery spell, add {R}.
 * When Electro leaves the battlefield, you may pay {X}. When you do, he deals X damage to target
 * player.
 *
 * The mana-retention clause is the new `RetainUnspentColoredMana(Color.RED)` static.
 */
val ElectroAssaultingBattery = card("Electro, Assaulting Battery") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Villain"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "You don't lose unspent red mana as steps and phases end.\n" +
        "Whenever you cast an instant or sorcery spell, add {R}.\n" +
        "When Electro leaves the battlefield, you may pay {X}. When you do, he deals X damage to " +
        "target player."

    keywords(Keyword.FLYING)

    // You don't lose unspent red mana as steps and phases end.
    staticAbility {
        ability = RetainUnspentColoredMana(Color.RED)
    }

    // Whenever you cast an instant or sorcery spell, add {R}.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddMana(Color.RED)
    }

    // When Electro leaves the battlefield, you may pay {X}. When you do, he deals X damage to a player.
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        val target = target("target player", Targets.Player)
        effect = MayPayXForEffect(
            effect = Effects.DealDamage(DynamicAmount.XValue, target)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d672cfad-e656-47f8-bf93-64f262aff33e.jpg?1783905338"
    }
}
