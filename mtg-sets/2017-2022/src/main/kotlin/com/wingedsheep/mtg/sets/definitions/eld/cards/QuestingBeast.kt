package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Questing Beast — Throne of Eldraine #171
 * {2}{G}{G} · Legendary Creature — Beast · 4/4
 *
 * - "Combat damage that would be dealt by creatures you control can't be prevented" is a
 *   [DamageCantBePrevented] scoped to combat damage from creatures you control; a burn spell's
 *   damage, or an opponent's creature's combat damage, stays preventable.
 * - "to an opponent" ([Recipient.Opponent]) and "that player" is the damaged opponent
 *   (`controlledByTriggeringPlayer()`); "that much" is the combat damage dealt.
 */
val QuestingBeast = card("Questing Beast") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Beast"
    oracleText = "Vigilance, deathtouch, haste\n" +
        "Questing Beast can't be blocked by creatures with power 2 or less.\n" +
        "Combat damage that would be dealt by creatures you control can't be prevented.\n" +
        "Whenever Questing Beast deals combat damage to an opponent, it deals that much damage to " +
        "target planeswalker that player controls."
    power = 4
    toughness = 4

    keywords(Keyword.VIGILANCE, Keyword.DEATHTOUCH, Keyword.HASTE)

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2))
    }

    replacementEffect(
        DamageCantBePrevented(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Creature.youControl(),
                damageType = DamageType.Combat
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.Opponent)
        val t = target(TargetFilter(GameObjectFilter.Planeswalker.controlledByTriggeringPlayer()))
        effect = Effects.DealDamage(DynamicAmounts.triggerDamageAmount(), t)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "171"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e41cf82d-3213-47ce-a015-6e51a8b07e4f.jpg?1783932604"
    }
}
