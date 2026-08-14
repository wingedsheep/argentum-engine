package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Skyseer's Chariot — Aetherdrift #28.
 *
 * {1}{W} · Artifact — Vehicle · 3/3
 *   Flying
 *   As this Vehicle enters, choose a nonland card name.
 *   Activated abilities of sources with the chosen name cost {2} more to activate.
 *   Crew 2
 *
 * The naming clause is the standard as-enters replacement (CR 614.12) — [EntersWithChoice] with
 * [ChoiceType.CARD_NAME] and the [CardNamePool.NONLAND] pool, storing the pick durably on the
 * permanent's `CastChoicesComponent` under `ChoiceSlot.CARD_NAME`. The tax then reads that slot
 * through `GameObjectFilter.namedFromChosenComponent()`, the same chosen-name predicate Sorcerous
 * Spyglass locks abilities with — so the two cards agree on what "sources with the chosen name"
 * means, and control changes / copies follow the permanent's own choice.
 *
 * "Cost {2} more to activate" taxes *every* activated ability of a matching source, mana abilities
 * and crew included, and applies even to abilities with no mana in their cost ("{T}:" becomes
 * "{2}, {T}:") — see [IncreaseActivatedAbilityCost].
 */
val SkyseersChariot = card("Skyseer's Chariot") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "As this Vehicle enters, choose a nonland card name.\n" +
        "Activated abilities of sources with the chosen name cost {2} more to activate.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    // As this Vehicle enters, choose a nonland card name.
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONLAND,
        )
    )

    staticAbility {
        ability = IncreaseActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Any.namedFromChosenComponent()),
            amount = DynamicAmount.Fixed(2),
        )
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Carl Critchlow"
        flavorText = "\"It's the dawn of a new era, and we shall rise like the suns to greet it.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96ed5b66-8e74-4a90-ad4e-c39d15993994.jpg?1783907914"
    }
}
