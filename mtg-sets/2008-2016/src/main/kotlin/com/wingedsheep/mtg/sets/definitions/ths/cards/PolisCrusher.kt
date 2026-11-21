package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Polis Crusher — Theros #198
 * {2}{R}{G} · Creature — Cyclops · 4/4
 *
 * Trample, protection from enchantments
 * {4}{R}{G}: Monstrosity 3.
 * Whenever this creature deals combat damage to a player, if this creature is monstrous, destroy
 * target enchantment that player controls.
 *
 * The intervening "if" is rechecked on resolution (CR 603.4); once the ability has triggered it
 * still resolves if Polis Crusher has left the battlefield, because the monstrous designation is
 * then read off its last-known information.
 */
val PolisCrusher = card("Polis Crusher") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Cyclops"
    power = 4
    toughness = 4
    oracleText = "Trample, protection from enchantments\n" +
        "{4}{R}{G}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "Whenever this creature deals combat damage to a player, if this creature is monstrous, destroy target enchantment that player controls."

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Enchantment")))

    activatedAbility {
        cost = Costs.Mana("{4}{R}{G}")
        effect = Effects.Monstrosity(3)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        interveningIf = Conditions.SourceIsMonstrous
        val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.controlledByTriggeringPlayer()))
        effect = Effects.Destroy(enchantment)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80874006-d057-49b8-981c-1d685ad1474b.jpg?1783939728"
        ruling("2013-09-15", "“Protection from enchantments” means that Polis Crusher can't be enchanted, it can't be targeted by Aura spells or by abilities of enchantments, all damage that would be dealt to it by enchantments is prevented, and it can't be blocked by enchantment creatures. Notably, it doesn't have protection from enchanted creatures (unless those creatures are also enchantments).")
        ruling("2013-09-15", "Polis Crusher must be monstrous as it deals combat damage to a player in order for its last ability to trigger. Once the ability triggers, it resolves even if Polis Crusher isn't on the battlefield at that time.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
