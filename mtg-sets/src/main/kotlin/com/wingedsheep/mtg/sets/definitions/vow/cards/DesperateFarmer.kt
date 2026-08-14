package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Desperate Farmer // Depraved Harvester (Innistrad: Crimson Vow)
 * {2}{B}
 * Creature — Human Peasant // Creature — Human Knight
 *
 * Front — Desperate Farmer (2/2)
 *   Lifelink
 *   When another creature you control dies, transform this creature.
 *
 * Back — Depraved Harvester (4/3)
 *   Lifelink
 *
 * Modeled as a transforming double-faced creature. The front's transform is a *per-death* trigger:
 * [Triggers.leavesBattlefield] filtered to creatures you control moving to the graveyard, bound with
 * [TriggerBinding.OTHER] so it fires on *another* creature dying (not the Farmer itself) — Voracious
 * Vermin's idiom. The back is a transformed face with no mana cost, so its color comes from a color
 * indicator (CR 204): `colorIndicator = "B"`.
 */

private val DesperateFarmerFront = card("Desperate Farmer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "When another creature you control dies, transform this creature."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER,
        )
        effect = TransformEffect(EffectTarget.Self)
        description = "When another creature you control dies, transform this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/467c566e-7f6a-40c9-8fd7-da6ae96df56c.jpg?1783924879"
    }
}

private val DepravedHarvester = card("Depraved Harvester") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 3
    oracleText = "Lifelink"

    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/back/4/6/467c566e-7f6a-40c9-8fd7-da6ae96df56c.jpg?1783924879"
    }
}

val DesperateFarmer: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = DesperateFarmerFront,
    backFace = DepravedHarvester,
)
