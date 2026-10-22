package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hound Tamer // Untamed Pup (Innistrad: Midnight Hunt)
 * {2}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Hound Tamer (3/3): Trample. {3}{G}: Put a +1/+1 counter on target creature. Daybound.
 * Back  — Untamed Pup (4/4): Trample. Other Wolves and Werewolves you control have trample.
 *         {3}{G}: Put a +1/+1 counter on target creature. Nightbound.
 */
private val HoundTamerFront = card("Hound Tamer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "{3}{G}: Put a +1/+1 counter on target creature.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Randy Vargas"
        flavorText = "\"You and me, we're not built for captivity.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28e2119b-ed78-4b98-a956-f2b453d0b164.jpg?1783925584"
    }
}

private val UntamedPup = card("Untamed Pup") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "Other Wolves and Werewolves you control have trample.\n" +
        "{3}{G}: Put a +1/+1 counter on target creature.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF)).youControl(),
                excludeSelf = true,
            ),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/back/2/8/28e2119b-ed78-4b98-a956-f2b453d0b164.jpg?1783925584"
    }
}

val HoundTamer: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HoundTamerFront,
    backFace = UntamedPup,
)
