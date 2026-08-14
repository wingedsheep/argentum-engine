package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val BloodlineKeeperFront = card("Bloodline Keeper") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Flying\n" +
        "{T}: Create a 2/2 black Vampire creature token with flying.\n" +
        "{B}: Transform this creature. Activate only if you control five or more Vampires."
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Tap
        effect = createVampireToken()
        description = "Create a 2/2 black Vampire creature token with flying."
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControlAtLeast(
                    5,
                    GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE),
                ),
            ),
        )
        effect = TransformEffect(EffectTarget.Self)
        description = "Transform this creature. Activate only if you control five or more Vampires."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13896468-e3d0-4bcb-b09e-b5c187aecb03.jpg?1783940965"
    }
}

private val LordOfLineage = card("Lord of Lineage") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Flying\n" +
        "Other Vampire creatures you control get +2/+2.\n" +
        "{T}: Create a 2/2 black Vampire creature token with flying."
    power = 5
    toughness = 5
    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 2,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.VAMPIRE).youControl(),
                excludeSelf = true,
            ),
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = createVampireToken()
        description = "Create a 2/2 black Vampire creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/back/1/3/13896468-e3d0-4bcb-b09e-b5c187aecb03.jpg?1783940965"
    }
}

private fun createVampireToken() = Effects.CreateToken(
    power = 2,
    toughness = 2,
    colors = setOf(Color.BLACK),
    creatureTypes = setOf("Vampire"),
    keywords = setOf(Keyword.FLYING),
    imageUri = "https://cards.scryfall.io/normal/front/c/5/c5cb4398-e180-472e-8662-2a5902bafb4f.jpg?1783907971",
)

val BloodlineKeeper: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BloodlineKeeperFront,
    backFace = LordOfLineage,
)
