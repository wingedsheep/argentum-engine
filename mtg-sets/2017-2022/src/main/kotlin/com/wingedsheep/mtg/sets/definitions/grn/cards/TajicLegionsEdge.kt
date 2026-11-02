package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tajic, Legion's Edge
 * {1}{R}{W}
 * Legendary Creature — Human Soldier
 * 3/2
 *
 * Haste
 * Mentor
 * Prevent all noncombat damage that would be dealt to other creatures you control.
 * {R}{W}: Tajic gains first strike until end of turn.
 *
 * The prevention shield is the same continuous [PreventDamage] replacement Crystal Barricade uses:
 * `amount = null` (prevent all), `DamageType.NonCombat`, recipients filtered to creatures you
 * control other than Tajic itself (`notSourceItself()`), re-evaluated per damage instance.
 */
val TajicLegionsEdge = card("Tajic, Legion's Edge") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Haste\n" +
        "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)\n" +
        "Prevent all noncombat damage that would be dealt to other creatures you control.\n" +
        "{R}{W}: Tajic gains first strike until end of turn."

    keywords(Keyword.HASTE)
    mentor()

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(
                    GameObjectFilter.Creature.youControl().notSourceItself(),
                ),
                damageType = DamageType.NonCombat,
            ),
        ),
    )

    activatedAbility {
        cost = Costs.Mana("{R}{W}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e669a01-84d3-4fcc-8396-9e987bd89b4f.jpg?1783934121"
    }
}
