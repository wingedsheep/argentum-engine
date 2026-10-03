package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Distinguished Conjurer {1}{W}
 * Creature — Human Wizard
 * 1/2
 *
 * Whenever another creature you control enters, you gain 1 life.
 * {4}{W}, {T}: Exile another target creature you control, then return it to the
 * battlefield under its owner's control.
 */
val DistinguishedConjurer = card("Distinguished Conjurer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    oracleText = "Whenever another creature you control enters, you gain 1 life.\n{4}{W}, {T}: Exile another target creature you control, then return it to the battlefield under its owner's control."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}"), Costs.Tap)
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.Move(creature, Zone.EXILE) then
            Effects.Move(creature, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0eaeec2-d1d4-4494-9c5d-cebfd7f088de.jpg?1783919197"

        ruling("2022-12-02", "When the card exiled by the second ability returns to the battlefield, it will be a new object with no connection to the card that was exiled. Auras attached to the exiled creature will be put into their owners' graveyards. Any Equipment will become unattached and remain on the battlefield. Any counters on the exiled creature will cease to exist.")
        ruling("2022-12-02", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2022-12-02", "Distinguished Conjurer's first ability triggers whenever any creature other than itself enters the battlefield under your control, including those returned by its last ability.")
    }
}
