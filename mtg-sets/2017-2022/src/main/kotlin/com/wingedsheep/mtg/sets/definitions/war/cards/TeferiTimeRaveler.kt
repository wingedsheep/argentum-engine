package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Teferi, Time Raveler
 * {1}{W}{U}
 * Legendary Planeswalker — Teferi
 * Starting Loyalty: 4
 *
 * Each opponent can cast spells only any time they could cast a sorcery.
 * +1: Until your next turn, you may cast sorcery spells as though they had flash.
 * −3: Return up to one target artifact, creature, or enchantment to its owner's hand. Draw a card.
 *
 * The static is a caster-relative [PlayersCantCastSpells]: an opponent is barred whenever *they*
 * couldn't cast a sorcery (CR 307.5), which also outranks any flash permission they hold.
 */
val TeferiTimeRaveler = card("Teferi, Time Raveler") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 4
    oracleText = "Each opponent can cast spells only any time they could cast a sorcery.\n" +
        "+1: Until your next turn, you may cast sorcery spells as though they had flash.\n" +
        "−3: Return up to one target artifact, creature, or enchantment to its owner's hand. Draw a card."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            condition = Conditions.Not(Conditions.CouldCastSorcery),
            conditionFromCaster = true,
        )
    }

    loyaltyAbility(+1) {
        effect = Effects.GrantFlashToSpells(spellFilter = GameObjectFilter.Sorcery, duration = Duration.UntilYourNextTurn)
    }

    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.ArtifactCreatureOrEnchantment, optional = true)
        effect = Effects.ReturnToHand(permanent) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cb76266-ae50-4bbc-8f96-d98f309b02d3.jpg?1783933384"

        ruling("2024-01-12", "If an effect allows opponents to cast a spell as though it had flash (for example, if your opponent also controls a Teferi, Time Raveler and activates his +1 loyalty ability), the restriction of Teferi's first ability takes precedence over that permission.")
        ruling("2024-01-12", "You may activate Teferi's last ability without choosing any target. You'll just draw a card. However, if you do choose a target and the target permanent is an illegal target by the time Teferi's last ability tries to resolve, the ability doesn't resolve. You don't draw a card.")
    }
}
