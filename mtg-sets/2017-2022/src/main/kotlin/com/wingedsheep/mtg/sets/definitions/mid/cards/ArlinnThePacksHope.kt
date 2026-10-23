package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arlinn, the Pack's Hope // Arlinn, the Moon's Fury (Innistrad: Midnight Hunt #211)
 * {2}{R}{G} · Legendary Planeswalker — Arlinn · Loyalty 4 (both faces)
 *
 * Front: Daybound. +1: Until your next turn, you may cast creature spells as though they had
 * flash, and each creature you control enters with an additional +1/+1 counter on it.
 * −3: Create two 2/2 green Wolf creature tokens.
 *
 * Back: Nightbound. +2: Add {R}{G}. 0: Until end of turn, Arlinn becomes a 5/5 Werewolf creature
 * with trample, indestructible, and haste.
 *
 * Implementation:
 *  - The +1 is two durational effects created by one resolving ability, so both outlive Arlinn
 *    (CR 611.2b): the flash permission is [Effects.GrantFlashToSpells] (Teferi, Time Raveler's
 *    shape), and the counter rider is an [EntersWithCounters] replacement handed to
 *    [Effects.GrantReplacementEffect] — Storyweave's channel, with `Duration.UntilYourNextTurn`
 *    instead of a one-shot. Both wear off after the untap step of Arlinn's controller's next turn.
 *  - The +2 is a loyalty ability, never a mana ability (CR 605.1a), so it uses the stack.
 *  - The 0 adds the creature type and keeps every other type, so Arlinn is still a planeswalker
 *    (Gideon, Champion of Justice's shape); damage dealt to her removes loyalty counters.
 */
private val ArlinnThePacksHopeFront = card("Arlinn, the Pack's Hope") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Arlinn"
    startingLoyalty = 4
    oracleText = "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)\n" +
        "+1: Until your next turn, you may cast creature spells as though they had flash, and each " +
        "creature you control enters with an additional +1/+1 counter on it.\n" +
        "−3: Create two 2/2 green Wolf creature tokens."

    daybound()

    loyaltyAbility(+1) {
        effect = Effects.GrantFlashToSpells(
            spellFilter = GameObjectFilter.Creature,
            duration = Duration.UntilYourNextTurn
        ) then Effects.GrantReplacementEffect(
            EntersWithCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                count = 1,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.youControl(),
                    to = Zone.BATTLEFIELD
                )
            ),
            EffectTarget.Self,
            Duration.UntilYourNextTurn
        )
        description = "Until your next turn, you may cast creature spells as though they had flash, " +
            "and each creature you control enters with an additional +1/+1 counter on it."
    }

    loyaltyAbility(-3) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            count = 2,
        )
        description = "Create two 2/2 green Wolf creature tokens."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50d4b0df-a1d8-494f-a019-70ce34161320.jpg?1783925572"
    }
}

private val ArlinnTheMoonsFury = card("Arlinn, the Moon's Fury") {
    manaCost = ""
    colorIdentity = "RG"
    colorIndicator = "RG" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Planeswalker — Arlinn"
    startingLoyalty = 4
    oracleText = "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)\n" +
        "+2: Add {R}{G}.\n" +
        "0: Until end of turn, Arlinn becomes a 5/5 Werewolf creature with trample, indestructible, and haste."

    nightbound()

    loyaltyAbility(+2) {
        effect = Effects.AddMana(Color.RED, 1) then Effects.AddMana(Color.GREEN, 1)
        description = "Add {R}{G}."
    }

    loyaltyAbility(0) {
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 5,
            keywords = setOf(Keyword.TRAMPLE, Keyword.INDESTRUCTIBLE, Keyword.HASTE),
            creatureTypes = setOf("Werewolf"),
            duration = Duration.EndOfTurn
        )
        description = "Until end of turn, Arlinn becomes a 5/5 Werewolf creature with trample, indestructible, and haste."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/back/5/0/50d4b0df-a1d8-494f-a019-70ce34161320.jpg?1783925572"
    }
}

val ArlinnThePacksHope: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ArlinnThePacksHopeFront,
    backFace = ArlinnTheMoonsFury,
)
