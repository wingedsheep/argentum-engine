package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Glasspool Mimic {2}{U} // Glasspool Shore — Zendikar Rising #60
 * Creature — Shapeshifter Rogue 0/0 // Land · Modal double-faced card
 *
 * Front — Glasspool Mimic: "You may have this creature enter as a copy of a creature you control,
 * except it's a Shapeshifter Rogue in addition to its other types."
 * Back — Glasspool Shore: "This land enters tapped. {T}: Add {U}."
 *
 * The added subtypes are a [CopyExceptions.IfCardType] creature clause: per the ruling, copying a
 * creature that is only temporarily a creature (an animated land) gives a noncreature copy that is
 * not a Shapeshifter Rogue. Creature subtypes can't sit on a noncreature anyway.
 */
private val GlasspoolMimicFront = card("Glasspool Mimic") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter Rogue"
    oracleText = "You may have this creature enter as a copy of a creature you control, except it's a " +
        "Shapeshifter Rogue in addition to its other types."
    power = 0
    toughness = 0

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature.youControl(),
            exceptions = CopyExceptions(
                conditional = listOf(
                    CopyExceptions.IfCardType(
                        CardType.CREATURE,
                        CopyExceptions(addedSubtypes = setOf(Subtype("Shapeshifter"), Subtype("Rogue"))),
                    )
                ),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Johan Grenier"
        flavorText = "\"When I touched the Glasspool, I tasted infinite possibility.\"\n—Ashen Wal, Akoum Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5adcb500-8c77-4925-8e2c-1243502827d1.jpg?1783929404"
        ruling(
            "2020-09-25",
            "If Glasspool Mimic isn't a creature, most likely because it copied a creature that was only " +
                "temporarily a creature, it won't be a Shapeshifter Rogue, even if it becomes a creature later."
        )
        ruling(
            "2020-09-25",
            "If another creature becomes a copy of Glasspool Mimic, that creature is also a Shapeshifter Rogue."
        )
        ruling(
            "2020-09-25",
            "If Glasspool Mimic somehow enters the battlefield at the same time as another creature, it can't " +
                "become a copy of that creature. You may choose only a creature that's already on the battlefield."
        )
    }
}

private val GlasspoolShoreBack = card("Glasspool Shore") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "This land enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Johan Grenier"
        flavorText = "\"Past and possibility come together in the vastness of infinite reflection.\"\n—Ashen Wal, Akoum Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/back/5/a/5adcb500-8c77-4925-8e2c-1243502827d1.jpg?1783929404"
    }
}

val GlasspoolMimic: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = GlasspoolMimicFront,
    backFace = GlasspoolShoreBack,
)
