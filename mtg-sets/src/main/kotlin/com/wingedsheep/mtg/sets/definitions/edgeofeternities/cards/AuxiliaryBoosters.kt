package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Auxiliary Boosters
 * {4}{W}
 * Artifact — Equipment
 * When this Equipment enters, create a 2/2 colorless Robot artifact creature token and attach this Equipment to it.
 * Equipped creature gets +1/+2 and has flying.
 * Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)
 */
val AuxiliaryBoosters = card("Auxiliary Boosters") {
    manaCost = "{4}{W}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a 2/2 colorless Robot artifact creature token and attach this Equipment to it.\nEquipped creature gets +1/+2 and has flying.\nEquip {3}"

    // ETB: Create 2/2 Robot token and attach this Equipment to it
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(),
            creatureTypes = setOf("Robot"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
        ).then(Effects.AttachEquipment(EffectTarget.ContextTarget(0)))
    }

    // Static ability: Equipped creature gets +1/+2
    staticAbility {
        effect = Effects.ModifyStats(+1, +2)
        filter = Filters.EquippedCreature
    }

    // Static ability: Equipped creature has flying
    staticAbility {
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.EquippedCreature)
        filter = Filters.EquippedCreature
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Dmitry Burmak"
        flavorText = "An intelligent jetpack for those who find themselves lost in space."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43706295-afd6-442c-8828-8cf978152701.jpg?1752946573"
    }
}
