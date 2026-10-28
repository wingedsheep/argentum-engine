package com.wingedsheep.mtg.sets.definitions.`40k`.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ghyrson Starn, Kelermorph — Warhammer 40,000 Commander #124
 * {1}{U}{R} · Legendary Creature — Tyranid Human · 3/2
 *
 * Ward {2}
 * Three Autostubs — Whenever another source you control deals exactly 1 damage to a permanent or
 * player, Ghyrson Starn deals 2 damage to that permanent or player.
 *
 * A source-filtered damage observer: "another" is `notSourceItself()` in the source filter (read as
 * the source last existed when a token died to the damage it dealt), and "that permanent or player"
 * is `EffectTarget.DamageRecipient`. The recipient is `Recipient.PermanentOrPlayer`, not `Any`, so a
 * source dealing 1 to several recipients at once triggers once per recipient (ruling) rather than
 * folding into one trigger carrying the total.
 */
val GhyrsonStarnKelermorph = card("Ghyrson Starn, Kelermorph") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Tyranid Human"
    power = 3
    toughness = 2
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, " +
        "counter it unless that player pays {2}.)\n" +
        "Three Autostubs — Whenever another source you control deals exactly 1 damage to a permanent or player, " +
        "Ghyrson Starn deals 2 damage to that permanent or player."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl().notSourceItself()).dealsDamage(Recipient.PermanentOrPlayer)
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.triggerDamageAmount(),
            ComparisonOperator.EQ,
            DynamicAmounts.fixed(1),
        )
        effect = Effects.DealDamage(2, EffectTarget.DamageRecipient)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Aaron J. Riley"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa7349d9-c82f-4cf8-a852-92168d1f4966.jpg?1783920850"
    }
}
