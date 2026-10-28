package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Organic Avulsion Unit
 * {2}{B}
 * Artifact Creature — Borg
 * 2/3
 *
 * {2}: Artifact creatures you control gain menace until end of turn. (A creature with menace can't be blocked except by two or more creatures.)
 */
val OrganicAvulsionUnit = card("Organic Avulsion Unit") {
    manaCost = "{2}{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    oracleText = "{2}: Artifact creatures you control gain menace until end of turn. (A creature with menace can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 3
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ForEachInGroup(
            GroupFilter((GameObjectFilter.Artifact and GameObjectFilter.Creature).youControl()),
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.IterationEntity)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"From this time forward, you will service us.\"\n—Locutus of Borg"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cc06121-6ecd-44c3-90f9-34d492b031d4.jpg?1785981505"
    }
}

