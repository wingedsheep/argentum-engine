package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Avabruck Caretaker // Hollowhenge Huntmaster (Innistrad: Crimson Vow)
 * {4}{G}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Avabruck Caretaker (4/4): Hexproof; "At the beginning of combat on your turn, put two +1/+1
 *          counters on another target creature you control"; Daybound.
 * Back  — Hollowhenge Huntmaster (6/6): Hexproof; "Other permanents you control have hexproof"; "At the
 *          beginning of combat on your turn, put two +1/+1 counters on each creature you control";
 *          Nightbound.
 *
 * Both faces have hexproof as a printed keyword. The front's combat trigger ([Triggers.BeginCombat],
 * which only fires on your turn) targets **another** creature you control ([Targets.OtherCreatureYouControl])
 * and adds two +1/+1 counters. The night face upgrades the trigger to *each* creature you control
 * ([Effects.ForEachInGroup] over `GameObjectFilter.Creature.youControl()`, the Cathars' Crusade rail),
 * dropping the target, and additionally grants hexproof to your other permanents (the Privileged Position
 * rail — [GrantKeyword] over a `GroupFilter` with `excludeSelf = true`, so it doesn't redundantly re-grant
 * its own printed hexproof).
 *
 * The back is a transformed face with no mana cost, so its color comes from a color indicator (CR 204):
 * `colorIndicator = "G"`.
 */

private val AvabruckCaretakerFront = card("Avabruck Caretaker") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 4
    toughness = 4
    oracleText = "Hexproof\n" +
        "At the beginning of combat on your turn, put two +1/+1 counters on another target creature " +
        "you control.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val creature = target("another target creature you control", Targets.OtherCreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, creature)
        description = "Put two +1/+1 counters on another target creature you control."
    }
    daybound()

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "187"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0c358b4-5af2-438f-8bd5-beb0ee6b518b.jpg?1783924827"
    }
}

private val HollowhengeHuntmaster = card("Hollowhenge Huntmaster") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 6
    oracleText = "Hexproof\n" +
        "Other permanents you control have hexproof.\n" +
        "At the beginning of combat on your turn, put two +1/+1 counters on each creature you control.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.HEXPROOF)

    staticAbility {
        ability = GrantKeyword(
            Keyword.HEXPROOF,
            GroupFilter(GameObjectFilter.Permanent.youControl(), excludeSelf = true),
        )
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
        )
        description = "Put two +1/+1 counters on each creature you control."
    }
    nightbound()

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "187"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/back/c/0/c0c358b4-5af2-438f-8bd5-beb0ee6b518b.jpg?1783924827"
    }
}

val AvabruckCaretaker: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = AvabruckCaretakerFront,
    backFace = HollowhengeHuntmaster,
)
