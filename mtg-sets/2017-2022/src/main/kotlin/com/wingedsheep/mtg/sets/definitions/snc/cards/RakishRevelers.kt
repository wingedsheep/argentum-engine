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
 * Rakish Revelers
 * {2}{R}{G}{W}
 * Creature — Elf Druid Rogue
 * 5 / 3
 * When this creature enters, create a 1/1 green and white Citizen creature token.
 * {2}, Exile this card from your hand: Target land gains "{T}: Add {R}, {G}, or {W}" until this
 * card is cast from exile. You may cast this card for as long as it remains exiled.
 *
 * The hand ability is the same shape as Glamorous Outlaw: [Costs.ExileSelf] from [Zone.HAND], a
 * granted mana ability for [Duration.UntilSourceCastFromExile], then a permanent cast-from-exile
 * permission on the card gathered from its owner's exile.
 */
val RakishRevelers = card("Rakish Revelers") {
    manaCost = "{2}{R}{G}{W}"
    colorIdentity = "GRW"
    typeLine = "Creature — Elf Druid Rogue"
    power = 5
    toughness = 3
    oracleText = "When this creature enters, create a 1/1 green and white Citizen creature token.\n" +
        "{2}, Exile this card from your hand: Target land gains \"{T}: Add {R}, {G}, or {W}\" until this card " +
        "is cast from exile. You may cast this card for as long as it remains exiled."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Citizen"),
        )
        description = "When this creature enters, create a 1/1 green and white Citizen creature token."
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
                    colorSet = ManaColorSet.Specific(setOf(Color.RED, Color.GREEN, Color.WHITE))
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            target = land,
            duration = Duration.UntilSourceCastFromExile
        ) then
            Effects.Pipeline {
                val exiledRevelers = gather(
                    CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.sourceItself()
                    )
                )
                run(Effects.GrantMayPlayFromExile(from = exiledRevelers, expiry = MayPlayExpiry.Permanent))
            }
        description = "{2}, Exile this card from your hand: Target land gains \"{T}: Add {R}, {G}, or {W}\" " +
            "until this card is cast from exile. You may cast this card for as long as it remains exiled."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "214"
        artist = "Alessandra Pisano"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c74197a-367c-40a4-9eb5-6943490bba09.jpg?1783923073"
        ruling("2022-04-29", "You may use the mana ability Rakish Revelers grants to the land while casting Rakish Revelers from exile.")
        ruling("2022-04-29", "If Rakish Revelers is removed from exile without being cast, the land will continue to have this ability.")
    }
}
