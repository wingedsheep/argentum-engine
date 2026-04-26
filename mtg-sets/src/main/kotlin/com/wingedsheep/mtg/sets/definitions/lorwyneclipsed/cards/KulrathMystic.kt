package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.SpellCastEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kulrath Mystic
 * {2}{U}
 * Creature — Elemental Wizard
 * 2/4
 *
 * Whenever you cast a spell with mana value 4 or greater, this creature gets +2/+0
 * and gains vigilance until end of turn.
 */
val KulrathMystic = card("Kulrath Mystic") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Elemental Wizard"
    oracleText = "Whenever you cast a spell with mana value 4 or greater, this creature gets +2/+0 " +
        "and gains vigilance until end of turn."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = TriggerSpec(
            SpellCastEvent(
                spellFilter = GameObjectFilter.Any.manaValueAtLeast(4),
                player = Player.You
            ),
            TriggerBinding.ANY
        )
        effect = CompositeEffect(
            listOf(
                Effects.ModifyStats(2, 0, EffectTarget.Self),
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Jason A. Engle"
        flavorText = "\"The difference between passion and anger is nothing more than who is in control.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/377d257c-920c-4dd4-a4b1-01cbc631ef8f.jpg?1767732526"
        ruling("2025-11-17", "If a spell has {X} in its mana cost, use the value chosen for that X to determine the mana value of that spell.")
    }
}
