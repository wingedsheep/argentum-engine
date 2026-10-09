package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SaheeliFiligreeMaster = card("Saheeli, Filigree Master") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Saheeli"
    startingLoyalty = 3
    oracleText = "+1: Scry 1. You may tap an untapped artifact you control. If you do, draw a card.\n−2: Create two 1/1 colorless Thopter artifact creature tokens with flying. They gain haste until end of turn.\n−4: You get an emblem with \"Artifact creatures you control get +1/+1\" and \"Artifact spells you cast cost {1} less to cast.\""

    loyaltyAbility(+1) {
        description = "+1: Scry 1. You may tap an untapped artifact you control. If you do, draw a card."
        effect = Patterns.Library.scry(1) then Effects.Pipeline {
            val artifacts = gather(GameObjectFilter.Artifact.untapped(), player = Player.You)
            val tapped = chooseUpTo(1, artifacts,
                prompt = "You may tap an untapped artifact you control to draw a card",
                useTargetingUI = true)
            run(Effects.TapCollection(tapped))
            run(Effects.DrawCards(tapped.count))
        }
    }

    loyaltyAbility(-2) {
        description = "−2: Create two 1/1 colorless Thopter artifact creature tokens with flying. They gain haste until end of turn."
        effect = Effects.CreateToken(
            power = 1, toughness = 1, creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING), count = 2, artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/4/5/4501d15d-d306-4372-9516-bd63cf788f45.jpg?1783919902"
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
        )
    }

    loyaltyAbility(-4) {
        description = "−4: You get an emblem with \"Artifact creatures you control get +1/+1\" and \"Artifact spells you cast cost {1} less to cast.\""
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter(GameObjectFilter.ArtifactCreature.youControl()),
            powerBonus = 1,
            toughnessBonus = 1,
            ownedStaticAbilities = listOf(ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Artifact),
                modification = CostModification.ReduceGeneric(1)
            )),
            emblemDescription = "Artifact creatures you control get +1/+1. Artifact spells you cast cost {1} less to cast."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "219"
        artist = "Aurore Folny"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3657562-323c-40d0-a261-d029ed549695.jpg?1783920027"
        ruling("2022-10-14", "For the first ability, you don't choose which artifact to tap until the ability resolves.")
        ruling("2022-10-14", "If you manage to activate Saheeli's last ability multiple times, each emblem you get is applied independently. For example, if you have two of those emblems, artifact creatures you control get +2/+2 and artifact spells you cast cost {2} less.")
        ruling("2022-10-14", "The cost reduction provided by Saheeli's emblem can never reduce the colored mana costs of an artifact spell.")
    }
}
