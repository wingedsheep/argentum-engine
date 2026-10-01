package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glissa Sunslayer — {1}{B}{G} Legendary Creature — Phyrexian Zombie Elf 3/3 (ONE #202).
 *
 * A "choose one" modal combat-damage trigger. The third mode is [Effects.RemoveCountersUpTo]: the
 * controller picks up to three counters in total across every kind on the permanent, at resolution.
 */
val GlissaSunslayer = card("Glissa Sunslayer") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Phyrexian Zombie Elf"
    power = 3
    toughness = 3
    oracleText = "First strike, deathtouch\n" +
        "Whenever Glissa Sunslayer deals combat damage to a player, choose one —\n" +
        "• You draw a card and lose 1 life.\n" +
        "• Destroy target enchantment.\n" +
        "• Remove up to three counters from target permanent."

    keywords(Keyword.FIRST_STRIKE, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = ModalEffect.chooseOne(
            mode("You draw a card and lose 1 life") {
                effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
            },
            mode("Destroy target enchantment") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            },
            mode("Remove up to three counters from target permanent") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.RemoveCountersUpTo(3, permanent)
            },
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Krharts"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2bf633e-0470-4b87-99ed-5ad1683b0954.jpg?1783918001"

        ruling(
            "2023-02-04",
            "For the third mode, you choose which counters to remove as the ability resolves. You may remove any " +
                "three (or fewer) counters, even if the permanent has multiple kinds of counters on it."
        )
    }
}
