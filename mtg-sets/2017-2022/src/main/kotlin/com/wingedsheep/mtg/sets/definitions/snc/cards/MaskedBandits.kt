package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Masked Bandits
 * {3}{B}{R}{G}
 * Creature — Raccoon Rogue
 * 5 / 5
 * Vigilance
 * Menace
 * {2}, Exile this card from your hand: Target land gains "{T}: Add {B}, {R}, or {G}" until this
 * card is cast from exile. You may cast this card for as long as it remains exiled.
 *
 * The hand ability is the same shape as Glamorous Outlaw and Rakish Revelers.
 */
val MaskedBandits = card("Masked Bandits") {
    manaCost = "{3}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Creature — Raccoon Rogue"
    power = 5
    toughness = 5
    oracleText = "Vigilance\nMenace (This creature can't be blocked except by two or more creatures.)\n" +
        "{2}, Exile this card from your hand: Target land gains \"{T}: Add {B}, {R}, or {G}\" until this card " +
        "is cast from exile. You may cast this card for as long as it remains exiled."

    keywords(Keyword.VIGILANCE, Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ExileSelf)
        activateFromZone = Zone.HAND
        val land = target(TargetFilter.Land)
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = AbilityCost.Tap,
                effect = Effects.AddManaOfChoice(
                    colorSet = ManaColorSet.Specific(setOf(Color.BLACK, Color.RED, Color.GREEN))
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            target = land,
            duration = Duration.UntilSourceCastFromExile
        ) then
            Effects.Pipeline {
                val exiledBandits = gather(
                    CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.sourceItself()
                    )
                )
                run(Effects.GrantMayPlayFromExile(from = exiledBandits, expiry = MayPlayExpiry.Permanent))
            }
        description = "{2}, Exile this card from your hand: Target land gains \"{T}: Add {B}, {R}, or {G}\" " +
            "until this card is cast from exile. You may cast this card for as long as it remains exiled."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Micah Epstein"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03757415-6b58-42d3-9452-a862e90647ee.jpg?1783923080"
        ruling("2022-04-29", "You may use the mana ability Masked Bandits grants to the land while casting Masked Bandits from exile.")
        ruling("2022-04-29", "If Masked Bandits is removed from exile without being cast, the land will continue to have this ability.")
    }
}
