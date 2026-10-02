package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RetainUnspentColoredMana
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ashling, Flame Dancer — Modern Horizons 3 #115
 * {2}{R}{R} · Legendary Creature — Elemental Shaman · 4/4
 *
 * You don't lose unspent red mana as steps and phases end.
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, discard a card, then draw a
 * card. If this is the second time this ability has resolved this turn, Ashling deals 2 damage to
 * each opponent and each creature they control. If it's the third time, add {R}{R}{R}{R}.
 *
 * Magecraft is [Triggers.you] `castsOrCopies` — one ability over two events, so the per-turn
 * resolution tally ([IncrementAbilityResolutionCountEffect] read back by
 * [Conditions.SourceAbilityResolvedNTimes]) counts casts and copies alike. Each copy an effect
 * creates triggers it once (the card's ruling).
 */
val AshlingFlameDancer = card("Ashling, Flame Dancer") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Elemental Shaman"
    power = 4
    toughness = 4
    oracleText = "You don't lose unspent red mana as steps and phases end.\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, discard a card, then draw " +
        "a card. If this is the second time this ability has resolved this turn, Ashling deals 2 damage " +
        "to each opponent and each creature they control. If it's the third time, add {R}{R}{R}{R}."

    staticAbility {
        ability = RetainUnspentColoredMana(Color.RED)
    }

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Patterns.Hand.discardCards(1) then Effects.DrawCards(1) then
            IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
                    Patterns.Group.dealDamageToAll(2, GroupFilter.AllCreaturesOpponentsControl)
            ) then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(3),
                then = Effects.AddMana(Color.RED, 4)
            )
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, discard a " +
            "card, then draw a card. If this is the second time this ability has resolved this turn, " +
            "Ashling deals 2 damage to each opponent and each creature they control. If it's the " +
            "third time, add {R}{R}{R}{R}."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Michal Ivan"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40463be5-89e2-410b-9a4b-770f70d14293.jpg?1783911273"
        ruling("2024-06-07", "If an effect creates multiple copies of an instant or sorcery spell, Ashling's last ability triggers once for each copy created by the effect.")
        ruling("2024-06-07", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies will not cause Ashling's last ability to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause Ashling's last ability to trigger.")
    }
}
