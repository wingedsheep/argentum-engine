package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val BiolumeEggFront = card("Biolume Egg") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent Egg"
    oracleText = "Defender\n" +
        "When this creature enters, scry 2.\n" +
        "When you sacrifice this creature, return it to the battlefield transformed under its " +
        "owner's control at the beginning of the next end step."
    power = 0
    toughness = 4
    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Scry(2)
    }

    triggeredAbility {
        trigger = Triggers.Sacrificed
        effect = CreateDelayedTriggerEffect(
            step = Step.END,
            effect = Effects.ReturnSelfFromGraveyardTransformed(),
        )
        description = "Return Biolume Egg to the battlefield transformed under its owner's control " +
            "at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Filip Burburan"
        flavorText = "Serpents leave their eggs in the cold and inky depths..."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57039230-bf5a-4489-9dc1-37e27b17bd84.jpg?1783924907"
    }
}

private val BiolumeSerpent = card("Biolume Serpent") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Serpent"
    oracleText = "Sacrifice two Islands: This creature can't be blocked this turn."
    power = 4
    toughness = 4

    activatedAbility {
        cost = Costs.SacrificeMultiple(
            count = 2,
            filter = GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
        )
        effect = GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, EffectTarget.Self)
        description = "This creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Filip Burburan"
        flavorText = "...knowing their hatchlings are more fearsome than anything else that might come along."
        imageUri = "https://cards.scryfall.io/normal/back/5/7/57039230-bf5a-4489-9dc1-37e27b17bd84.jpg?1783924907"
    }
}

val BiolumeEgg: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BiolumeEggFront,
    backFace = BiolumeSerpent,
)
