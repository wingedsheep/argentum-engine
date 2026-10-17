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
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Glamorous Outlaw
 * {3}{U}{B}{R}
 * Creature — Vampire Rogue
 * 4 / 5
 * When this creature enters, it deals 2 damage to each opponent and you scry 2.
 * {2}, Exile this card from your hand: Target land gains "{T}: Add {U}, {B}, or {R}" until this
 * card is cast from exile. You may cast this card for as long as it remains exiled.
 *
 * The hand ability is the Emrakul, the Exigent Doom shape: [Costs.ExileSelf] from [Zone.HAND], a
 * granted mana ability for [Duration.UntilSourceCastFromExile] (the grant tracks the exact exile
 * object, so the land keeps the ability if the card leaves exile any other way — per ruling), then
 * a permanent cast-from-exile permission on the card gathered from its owner's exile. The granted
 * ability can help pay for casting the Outlaw itself (ruling): it is removed only once the card is
 * cast.
 */
val GlamorousOutlaw = card("Glamorous Outlaw") {
    manaCost = "{3}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Creature — Vampire Rogue"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, it deals 2 damage to each opponent and you scry 2.\n" +
        "{2}, Exile this card from your hand: Target land gains \"{T}: Add {U}, {B}, or {R}\" until this card " +
        "is cast from exile. You may cast this card for as long as it remains exiled."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.Scry(2)
        description = "When this creature enters, it deals 2 damage to each opponent and you scry 2."
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
                    colorSet = ManaColorSet.Specific(setOf(Color.BLUE, Color.BLACK, Color.RED))
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            target = land,
            duration = Duration.UntilSourceCastFromExile
        ) then
            Effects.Pipeline {
                val exiledOutlaw = gather(
                    CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.sourceItself()
                    )
                )
                run(Effects.GrantMayPlayFromExile(from = exiledOutlaw, expiry = MayPlayExpiry.Permanent))
            }
        description = "{2}, Exile this card from your hand: Target land gains \"{T}: Add {U}, {B}, or {R}\" " +
            "until this card is cast from exile. You may cast this card for as long as it remains exiled."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Maria Zolotukhina"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/452abab0-cf23-4b9d-831a-7b9fa1fd582a.jpg?1783923086"
        ruling("2022-04-29", "You may use the mana ability Glamorous Outlaw grants to the land while casting Glamorous Outlaw from exile.")
        ruling("2022-04-29", "If Glamorous Outlaw is removed from exile without being cast, the land will continue to have this ability.")
    }
}
