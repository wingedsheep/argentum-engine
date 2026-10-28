package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lavinia, Azorius Renegade
 * {W}{U}
 * Legendary Creature — Human Soldier
 * 2/2
 *
 * Each opponent can't cast noncreature spells with mana value greater than the number of lands
 * that player controls.
 * Whenever an opponent casts a spell, if no mana was spent to cast it, counter that spell.
 *
 * "That player" is the caster: a [PlayersCantCastSpells] spell filter is evaluated with the casting
 * player as "you", so `landsYouControl()` counts the caster's lands, not Lavinia's controller's.
 * An {X} spell's mana value counts the X chosen for it (per the ruling).
 */
val LaviniaAzoriusRenegade = card("Lavinia, Azorius Renegade") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Each opponent can't cast noncreature spells with mana value greater than the number of lands that player controls.\n" +
        "Whenever an opponent casts a spell, if no mana was spent to cast it, counter that spell."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Noncreature.withCardPredicate(
                CardPredicate.Not(CardPredicate.ManaValueAtMostDynamic(DynamicAmounts.landsYouControl()))
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        interveningIf = Conditions.TriggeringSpellCastWithoutPayingMana
        effect = Effects.CounterTriggeringSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        flavorText = "\"I told Jace that Ravnica would fall apart if he didn't take his responsibilities seriously. I didn't want to be right.\""
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c497d496-1232-4614-93b0-9864fa93c29f.jpg?1783933643"
    }
}
