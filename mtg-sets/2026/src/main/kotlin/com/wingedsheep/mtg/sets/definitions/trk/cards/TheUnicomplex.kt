package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Unicomplex
 * Land
 *
 * This land enters tapped unless you control two or more other lands.
 * When this land enters untapped, choose one —
 * • Put target creature card from your graveyard on top of your library.
 * • Surveil 1.
 * {T}: Add {B}.
 *
 * Same cycle and shape as [NiVarScienceAcademy]; the first mode is Witch's Cottage's
 * graveyard-to-top-of-library effect (mandatory here, no "you may").
 */
val TheUnicomplex = card("The Unicomplex") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or more other lands.\n" +
        "When this land enters untapped, choose one —\n" +
        "• Put target creature card from your graveyard on top of your library.\n" +
        "• Surveil 1.\n" +
        "{T}: Add {B}."

    replacementEffect(EntersTapped(unlessCondition = Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        effect = ModalEffect.chooseOne(
            mode("Put target creature card from your graveyard on top of your library") {
                val creature = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.PutOnTopOfLibrary(creature)
            },
            Mode.noTarget(Patterns.Library.surveil(1), "Surveil 1"),
        )
        description = "When this land enters untapped, choose one — put target creature card from " +
            "your graveyard on top of your library; or surveil 1."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "304"
        artist = "JB Casacop"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c881efb-d55b-45fe-9e5a-8a61aec6fbbb.jpg?1792632978"
    }
}
