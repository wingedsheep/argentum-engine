package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val HarvestHandFront = card("Harvest Hand") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Scarecrow"
    oracleText = "When this creature dies, return it to the battlefield transformed under your control."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.ReturnSelfFromGraveyardTransformed(tapped = false)
        description = "When Harvest Hand dies, return it to the battlefield transformed under your control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Jason Felix"
        flavorText = "The harvest is never finished."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d200f98-3377-46a3-9197-3cbd95d03dbf.jpg?1783937712"
    }
}

private val ScroungedScythe = card("Scrounged Scythe") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "As long as equipped creature is a Human, it has menace.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
        )
        ability = GrantKeyword(Keyword.MENACE)
    }
    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/back/0/d/0d200f98-3377-46a3-9197-3cbd95d03dbf.jpg?1783937712"
    }
}

val HarvestHand: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = HarvestHandFront,
    backFace = ScroungedScythe,
)
