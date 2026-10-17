package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
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
 * Spara's Adjudicators
 * {2}{G}{W}{U}
 * Creature — Cat Citizen
 * 4 / 4
 * When this creature enters, target creature an opponent controls can't attack or block until your
 * next turn.
 * {2}, Exile this card from your hand: Target land gains "{T}: Add {G}, {W}, or {U}" until this card
 * is cast from exile. You may cast this card for as long as it remains exiled.
 *
 * The hand ability is the same shape as Masked Bandits and Rakish Revelers.
 */
val SparasAdjudicators = card("Spara's Adjudicators") {
    manaCost = "{2}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Creature — Cat Citizen"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, target creature an opponent controls can't attack or block " +
        "until your next turn.\n" +
        "{2}, Exile this card from your hand: Target land gains \"{T}: Add {G}, {W}, or {U}\" until this card " +
        "is cast from exile. You may cast this card for as long as it remains exiled."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantAttackOrBlock(victim, Duration.UntilYourNextTurn)
        description = "When this creature enters, target creature an opponent controls can't attack or block " +
            "until your next turn."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ExileSelf)
        activateFromZone = Zone.HAND
        val land = target(TargetFilter.Land)
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = AbilityCost.Tap,
                effect = Effects.AddManaOfChoice(
                    colorSet = ManaColorSet.Specific(setOf(Color.GREEN, Color.WHITE, Color.BLUE))
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            target = land,
            duration = Duration.UntilSourceCastFromExile
        ) then
            Effects.Pipeline {
                val exiledAdjudicators = gather(
                    CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.sourceItself()
                    )
                )
                run(Effects.GrantMayPlayFromExile(from = exiledAdjudicators, expiry = MayPlayExpiry.Permanent))
            }
        description = "{2}, Exile this card from your hand: Target land gains \"{T}: Add {G}, {W}, or {U}\" " +
            "until this card is cast from exile. You may cast this card for as long as it remains exiled."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Aaron J. Riley"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29eb0e40-d184-423f-8484-ee288fda00cd.jpg?1783923068"
        ruling("2022-04-29", "You may use the mana ability Spara's Adjudicators grants to the land while casting Spara's Adjudicators from exile.")
        ruling("2022-04-29", "If Spara's Adjudicators is removed from exile without being cast, the land will continue to have this ability.")
    }
}
