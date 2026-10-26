package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Journey to Eternity // Atzal, Cave of Eternity (Rivals of Ixalan)
 * {1}{B}{G}
 * Legendary Enchantment — Aura // Legendary Land
 *
 * Front — Enchant creature you control
 *   When enchanted creature dies, return it to the battlefield under your control, then return
 *   this card to the battlefield transformed under your control.
 *
 * Back — {T}: Add one mana of any color.
 *   {3}{B}{G}, {T}: Return target creature card from your graveyard to the battlefield.
 *
 * The dies trigger is Necrogen Communion's attached-bound return followed by the self-return
 * transformed. Both halves carry "under your control": a creature you control but don't own comes
 * back to you from its owner's graveyard (ruling), and so does this card. The Aura is put into the
 * graveyard by state-based action right after the creature dies, so by resolution both are in
 * graveyards; each half checks its own card is still there, so one being exiled in response
 * doesn't stop the other.
 */
private val JourneyToEternityFront = card("Journey to Eternity") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When enchanted creature dies, return it to the battlefield under your control, then " +
        "return this card to the battlefield transformed under your control."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true) then
            Effects.ReturnSelfFromGraveyardTransformed(underYourControl = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d81c4b3f-81c2-403b-8a5d-c9415f73a1f9.jpg?1783935280"
    }
}

private val AtzalCaveOfEternity = card("Atzal, Cave of Eternity") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Journey to Eternity.)\n" +
        "{T}: Add one mana of any color.\n" +
        "{3}{B}{G}, {T}: Return target creature card from your graveyard to the battlefield."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}{G}"), Costs.Tap)
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/back/d/8/d81c4b3f-81c2-403b-8a5d-c9415f73a1f9.jpg?1783935280"
    }
}

val JourneyToEternity: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = JourneyToEternityFront,
    backFace = AtzalCaveOfEternity,
)
