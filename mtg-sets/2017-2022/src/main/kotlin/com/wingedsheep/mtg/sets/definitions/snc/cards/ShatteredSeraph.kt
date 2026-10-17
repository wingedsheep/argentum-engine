package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
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
 * Shattered Seraph
 * {4}{W}{U}{B}
 * Creature — Angel Rogue
 * 4 / 4
 * Flying
 * When this creature enters, you gain 3 life.
 * {2}, Exile this card from your hand: Target land gains "{T}: Add {W}, {U}, or {B}" until this
 * card is cast from exile. You may cast this card for as long as it remains exiled.
 *
 * The hand ability is the same shape as Glamorous Outlaw and Rakish Revelers.
 */
val ShatteredSeraph = card("Shattered Seraph") {
    manaCost = "{4}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Creature — Angel Rogue"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, you gain 3 life.\n" +
        "{2}, Exile this card from your hand: Target land gains \"{T}: Add {W}, {U}, or {B}\" until this card " +
        "is cast from exile. You may cast this card for as long as it remains exiled."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
        description = "When this creature enters, you gain 3 life."
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
                    colorSet = ManaColorSet.Specific(setOf(Color.WHITE, Color.BLUE, Color.BLACK))
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            target = land,
            duration = Duration.UntilSourceCastFromExile
        ) then
            Effects.Pipeline {
                val exiledSeraph = gather(
                    CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.sourceItself()
                    )
                )
                run(Effects.GrantMayPlayFromExile(from = exiledSeraph, expiry = MayPlayExpiry.Permanent))
            }
        description = "{2}, Exile this card from your hand: Target land gains \"{T}: Add {W}, {U}, or {B}\" " +
            "until this card is cast from exile. You may cast this card for as long as it remains exiled."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "221"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc75ba0d-230e-4ca4-bb40-bd05af2a4457.jpg?1783923070"
        ruling("2022-04-29", "You may use the mana ability Shattered Seraph grants to the land while casting Shattered Seraph from exile.")
        ruling("2022-04-29", "If Shattered Seraph is removed from exile without being cast, the land will continue to have this ability.")
    }
}
