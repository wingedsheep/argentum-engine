package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.GroupPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Cecil, Dark Knight // Cecil, Redeemed Paladin
 * {B} — Legendary Creature — Human Knight 2/3 // Legendary Creature — Human Knight 4/4
 *
 * Front — Cecil, Dark Knight:
 *   Deathtouch
 *   Darkness — Whenever Cecil deals damage, you lose that much life. Then if your life total is
 *   less than or equal to half your starting life total, untap Cecil and transform it.
 *
 * Back — Cecil, Redeemed Paladin:
 *   Lifelink
 *   Protect — Whenever Cecil attacks, other attacking creatures gain indestructible until end of turn.
 */
private val CecilRedeemedPaladin = card("Cecil, Redeemed Paladin") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Lifelink\n" +
        "Protect — Whenever Cecil attacks, other attacking creatures gain indestructible until end of turn."
    power = 4
    toughness = 4

    keywords(Keyword.LIFELINK)

    // Protect — Whenever Cecil attacks, other attacking creatures gain indestructible until end of turn.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = GroupPatterns.grantKeywordToAll(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(GameObjectFilter.Creature.attacking(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Josu Hernaiz"
        flavorText = "\"This is my battle, one I must fight alone to atone for my wrongs.\""
        imageUri = "https://cards.scryfall.io/normal/back/0/2/026e7167-d665-43d0-a51e-8df2d68cdb5e.jpg?1748707809"
    }
}

private val CecilDarkKnightFrontFace = card("Cecil, Dark Knight") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Deathtouch\n" +
        "Darkness — Whenever Cecil deals damage, you lose that much life. Then if your life total is less than or equal to half your starting life total, untap Cecil and transform it."
    power = 2
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    // Darkness — Whenever Cecil deals damage, you lose that much life. Then if your life
    // total is less than or equal to half your starting life total, untap Cecil and transform it.
    triggeredAbility {
        trigger = Triggers.DealsDamage
        effect = Effects.Composite(listOf(
            Effects.LoseLife(DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT), EffectTarget.Controller),
            ConditionalEffect(
                condition = Compare(
                    DynamicAmount.LifeTotal(Player.You),
                    ComparisonOperator.LTE,
                    DynamicAmount.Divide(DynamicAmounts.startingLifeTotal(Player.You), DynamicAmount.Fixed(2), roundUp = false)
                ),
                effect = Effects.Composite(listOf(
                    Effects.Untap(EffectTarget.Self),
                    TransformEffect(EffectTarget.Self)
                ))
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Josu Hernaiz"
        flavorText = "\"I suppose this is my fate as a dark knight. Soon, I won't even feel remorse for my actions.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/026e7167-d665-43d0-a51e-8df2d68cdb5e.jpg?1748707809"
    }
}

val CecilDarkKnight: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CecilDarkKnightFrontFace,
    backFace = CecilRedeemedPaladin
)
