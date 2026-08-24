package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sneaky Snacker
 * {U}{B}
 * Creature — Faerie Rogue
 * 2/1
 *
 * Flying
 * When you draw your third card in a turn, return this card from your graveyard to the battlefield tapped.
 *
 * Uses [Triggers.NthCardDrawn]`(3)` (CR 121.2) for the third-draw trigger, and
 * [Effects.PutOntoBattlefieldFromGraveyard]`(Self, tapped = true)` for the recursion — the same
 * facade as Persistent Specimen / Reassembling Skeleton / Teacher's Pest, but triggered rather than
 * activated. The facade's `fromZone = GRAVEYARD` is the guard the printed line names: a Snacker
 * that has left the graveyard by the time the trigger resolves stays where it is.
 *
 * `triggerZones = {GRAVEYARD}` because the ability's effect moves the card out of the graveyard, so
 * it functions only there (CR 113.6m) — a Snacker on the battlefield doesn't see your third draw.
 */
val SneakySnacker = card("Sneaky Snacker") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen you draw your third card in a turn, return this card from your graveyard to the battlefield tapped."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.NthCardDrawn(3)
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        triggerZones = setOf(Zone.GRAVEYARD)
        description = "When you draw your third card in a turn, return this card from your graveyard to the battlefield tapped."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "205"
        artist = "Irina Nordsol"
        flavorText = "Though the High Fae gave explicit instructions not to tarry, she stayed in Sweettooth Village for seconds, thirds, and then dessert."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e3aedae-e4bb-48e3-9f8b-bea0430df306.jpg?1783911245"
    }
}
