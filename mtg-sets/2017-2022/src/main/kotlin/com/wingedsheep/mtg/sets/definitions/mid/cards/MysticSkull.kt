package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mystic Skull // Mystic Monstrosity (Innistrad: Midnight Hunt)
 * {2} Artifact // Artifact Creature — Construct 5/6 (transform)
 *
 * Front — Mystic Skull
 *   {1}, {T}: Add one mana of any color.
 *   {5}, {T}: Transform this artifact.
 *
 * Back — Mystic Monstrosity
 *   Lands you control have "{T}: Add one mana of any color."
 *
 * The back face's grant is Chromatic Lantern's shape: a real mana ability granted to every land
 * its controller controls for as long as the Monstrosity is on the battlefield.
 */
private val MysticSkullFront = card("Mystic Skull") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Add one mana of any color.\n{5}, {T}: Transform this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.Transform(EffectTarget.Self)
        description = "{5}, {T}: Transform this artifact."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Joe Slucher"
        flavorText = "It waited on the barn wall for decades, forgotten, looking for the perfect opportunity."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/115a9a44-131d-45f3-852a-40fd18e4afb6.jpg?1783925552"
    }
}

private val MysticMonstrosity = card("Mystic Monstrosity") {
    manaCost = ""
    typeLine = "Artifact Creature — Construct"
    power = 5
    toughness = 6
    oracleText = "Lands you control have \"{T}: Add one mana of any color.\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Land.youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Joe Slucher"
        flavorText = "Under the light of an unnatural moon, it walked out on legs of straw with joints of hinges ripped from the barn doors."
        imageUri = "https://cards.scryfall.io/normal/back/1/1/115a9a44-131d-45f3-852a-40fd18e4afb6.jpg?1783925552"
    }
}

val MysticSkull: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = MysticSkullFront,
    backFace = MysticMonstrosity,
)
