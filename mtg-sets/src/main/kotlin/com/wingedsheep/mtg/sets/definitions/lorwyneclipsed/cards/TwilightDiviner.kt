package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Twilight Diviner
 * {2}{B}
 * Creature — Elf Cleric
 * 3/3
 *
 * When this creature enters, surveil 2.
 * Whenever one or more other creatures you control enter, if they entered or were cast
 * from a graveyard, create a token that's a copy of one of them. This ability triggers
 * only once each turn.
 */
val TwilightDiviner = card("Twilight Diviner") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Elf Cleric"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, surveil 2. (Look at the top two cards of your " +
        "library, then put any number of them into your graveyard and the rest on top of your " +
        "library in any order.)\n" +
        "Whenever one or more other creatures you control enter, if they entered or were cast " +
        "from a graveyard, create a token that's a copy of one of them. This ability triggers " +
        "only once each turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(2)
    }

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        triggerCondition = Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard
        oncePerTurn = true
        effect = CreateTokenCopyOfTargetEffect(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Pauline Voss"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/443b6f30-1493-4d48-93d9-a91e22a7ebb3.jpg?1767952037"
        ruling("2025-11-17", "Any enters abilities of the copied creature will trigger when the token enters. Any \"as [this creature] enters\" or \"[this creature] enters with\" abilities of the copied permanent will also work.")
        ruling("2025-11-17", "The token created by Twilight Diviner's last ability copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else; see below). It doesn't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2025-11-17", "If the copied creature is copying something else, then the token enters as whatever that creature copied.")
        ruling("2025-11-17", "If the copied creature has {X} in its mana cost, X is 0.")
    }
}
