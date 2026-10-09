package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val UrzaPlaneswalker = card("Urza, Planeswalker") {
    manaCost = ""
    meldOf("Urza, Lord Protector", "The Mightstone and Weakstone")
    colorIndicator = "WU"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Urza"
    startingLoyalty = 7
    oracleText = "You may activate the loyalty abilities of Urza twice each turn rather than only once.\n+2: Artifact, instant, and sorcery spells you cast this turn cost {2} less to cast. You gain 2 life.\n+1: Draw two cards, then discard a card.\n0: Create two 1/1 colorless Soldier artifact creature tokens.\n−3: Exile target nonland permanent.\n−10: Artifacts and planeswalkers you control gain indestructible until end of turn. Destroy all nonland permanents."

    staticAbility { ability = ExtraLoyaltyActivation(GameObjectFilter.Any.sourceItself()) }

    loyaltyAbility(+2) {
        description = "+2: Artifact, instant, and sorcery spells you cast this turn cost {2} less to cast. You gain 2 life."
        effect = Effects.ReduceSpellCosts(
            GameObjectFilter.Artifact or GameObjectFilter.Instant or GameObjectFilter.Sorcery,
            DynamicAmounts.fixed(2)
        ) then Effects.GainLife(2)
    }
    loyaltyAbility(+1) {
        description = "+1: Draw two cards, then discard a card."
        effect = Effects.DrawCards(2) then Effects.Discard(1)
    }
    loyaltyAbility(0) {
        description = "0: Create two 1/1 colorless Soldier artifact creature tokens."
        effect = Effects.CreateToken(power = 1, toughness = 1, creatureTypes = setOf("Soldier"),
            count = 2, artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/3/7/37da5b54-ec55-46e3-9f0b-565cbbe1ac7a.jpg?1783919907")
    }
    loyaltyAbility(-3) {
        description = "−3: Exile target nonland permanent."
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(permanent)
    }
    loyaltyAbility(-10) {
        description = "−10: Artifacts and planeswalkers you control gain indestructible until end of turn. Destroy all nonland permanents."
        effect = Patterns.Group.grantKeywordToAll(Keyword.INDESTRUCTIBLE,
            GroupFilter((GameObjectFilter.Artifact or GameObjectFilter.Planeswalker).youControl())) then
            Effects.DestroyAll(GameObjectFilter.NonlandPermanent)
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "238b"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40a01679-3224-427e-bd1d-b797b0ab68b7.jpg?1783920022"
    }
}
