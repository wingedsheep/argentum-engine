package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boromir, Warden of the Tower
 * {2}{W}
 * Legendary Creature — Human Soldier
 * 3/3
 *
 * Vigilance
 * Whenever an opponent casts a spell, if no mana was spent to cast it, counter that spell.
 * Sacrifice Boromir: Creatures you control gain indestructible until end of turn. The Ring tempts you.
 *
 * The counter clause uses the new `Conditions.TriggeringSpellCastWithoutPayingMana` intervening-if
 * (reads the triggering spell's cast-mana record) + `Effects.CounterTriggeringSpell()`.
 */
val BoromirWardenOfTheTower = card("Boromir, Warden of the Tower") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever an opponent casts a spell, if no mana was spent to cast it, counter that spell.\n" +
        "Sacrifice Boromir: Creatures you control gain indestructible until end of turn. The Ring tempts you."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.OpponentCastsSpell
        triggerCondition = Conditions.TriggeringSpellCastWithoutPayingMana
        effect = Effects.CounterTriggeringSpell()
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = GrantKeywordEffect(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
        ).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        flavorText = "\"Farewell, Aragorn! Go to Minas Tirith and save my people!\""
        artist = "Yigit Koroglu"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6bc3720-2892-4dda-8f30-079a1ac8e1e2.jpg?1686967669"
    }
}
