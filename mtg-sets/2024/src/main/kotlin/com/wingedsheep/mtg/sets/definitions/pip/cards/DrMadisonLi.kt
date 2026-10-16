package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dr. Madison Li — Fallout #3.
 * {U}{R}{W} · Legendary Creature — Human Scientist · 2/3
 *
 * Whenever you cast an artifact spell, you get {E} (an energy counter).
 * {T}, Pay {E}: Target creature gets +1/+0 and gains trample and haste until end of turn.
 * {T}, Pay {E}{E}{E}: Draw a card.
 * {T}, Pay {E}{E}{E}{E}{E}: Return target artifact card from your graveyard to the battlefield tapped.
 *
 * Each activated ability is `{T}` plus a fixed [Costs.PayPlayerCounters] energy payment (Bespoke
 * Battlewagon's shape). The reanimation uses the guarded [Effects.PutOntoBattlefieldFromGraveyard]
 * with `tapped = true`, so it does nothing if the card left the graveyard before resolution.
 */
val DrMadisonLi = card("Dr. Madison Li") {
    manaCost = "{U}{R}{W}"
    colorIdentity = "WUR"
    typeLine = "Legendary Creature — Human Scientist"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast an artifact spell, you get {E} (an energy counter).\n" +
        "{T}, Pay {E}: Target creature gets +1/+0 and gains trample and haste until end of turn.\n" +
        "{T}, Pay {E}{E}{E}: Draw a card.\n" +
        "{T}, Pay {E}{E}{E}{E}{E}: Return target artifact card from your graveyard to the battlefield tapped."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Artifact)
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
        description = "{T}, Pay {E}: Target creature gets +1/+0 and gains trample and haste until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 3))
        effect = Effects.DrawCards(1)
        description = "{T}, Pay {E}{E}{E}: Draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 5))
        val artifact = target(TargetFilter.ArtifactInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(artifact, tapped = true)
        description = "{T}, Pay {E}{E}{E}{E}{E}: Return target artifact card from your graveyard to the " +
            "battlefield tapped."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Zuzanna Wużyk"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95af426d-a8af-4314-9ad0-ac61935e8e8e.jpg?1783912422"
        ruling("2024-03-08", "Dr. Madison Li's first ability resolves before the spell that caused it to trigger.")
    }
}
