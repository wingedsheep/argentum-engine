package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Rona, Disciple of Gix
 * {1}{U}{B}
 * Legendary Creature — Human Artificer
 * 2/2
 * When Rona enters, you may exile target historic card from your graveyard.
 * (Artifacts, legendaries, and Sagas are historic.)
 * You may cast spells from among cards exiled with Rona.
 * {4}, {T}: Exile the top card of your library.
 */
val RonaDiscipleOfGix = card("Rona, Disciple of Gix") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "When Rona enters, you may exile target historic card from your graveyard. (Artifacts, legendaries, and Sagas are historic.)\nYou may cast spells from among cards exiled with Rona.\n{4}, {T}: Exile the top card of your library."

    // ETB: You may exile target historic card from your graveyard (linked to Rona)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val t = target("historic card in your graveyard", TargetObject(
            filter = TargetFilter(
                baseFilter = GameObjectFilter.Historic,
                zone = Zone.GRAVEYARD
            ).ownedByYou()
        ))
        effect = Effects.Move(
            target = t,
            destination = Zone.EXILE,
            linkToSource = true
        )
    }

    // Static: You may cast spells from among cards exiled with Rona
    staticAbility {
        ability = GrantMayCastFromLinkedExile(filter = GameObjectFilter.Nonland)
    }

    // Activated: {4}, {T}: Exile the top card of your library (linked to Rona)
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "topCard"
                ),
                MoveCollectionEffect(
                    from = "topCard",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    linkToSource = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45420f5e-f7f8-4db7-9b54-e2fa1be22094.jpg?1562734878"
        ruling("2018-04-27", "If Rona leaves the battlefield, the exiled cards remain exiled. If Rona returns to the battlefield, it won't have access to cards exiled by the previous Rona.")
        ruling("2018-04-27", "The cards exiled from your library are exiled face up.")
        ruling("2018-04-27", "You must follow the normal timing permissions and restrictions of the cards you cast from exile.")
        ruling("2018-04-27", "You still pay all costs for spells cast this way, including additional costs. You may also pay alternative costs.")
    }
}
