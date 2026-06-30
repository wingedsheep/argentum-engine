package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fire Nation Palace — Land — Rare
 *
 * This land enters tapped unless you control a basic land.
 * {T}: Add {R}.
 * {1}{R}, {T}: Target creature you control gains firebending 4 until end of turn.
 *   (Whenever it attacks, add {R}{R}{R}{R}. This mana lasts until end of combat.)
 *
 * The enters-tapped-unless-basic clause reuses the conditional [EntersTapped] replacement (same
 * shape as Ba Sing Se / Realm of Koh). The grant is the genuinely new piece: firebending has no
 * engine handler — the printed keyword is a display tag plus an attack-triggered combat-duration
 * mana effect — so [Effects.GrantFirebending] grants that exact attack trigger until end of turn,
 * making the affected creature add {R}{R}{R}{R} (kept through combat) when it attacks this turn.
 */
val FireNationPalace = card("Fire Nation Palace") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped unless you control a basic land.\n" +
        "{T}: Add {R}.\n" +
        "{1}{R}, {T}: Target creature you control gains firebending 4 until end of turn. " +
        "(Whenever it attacks, add {R}{R}{R}{R}. This mana lasts until end of combat.)"

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.BasicLand)
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        val creature = target(
            "target creature you control",
            TargetObject(filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.GrantFirebending(4, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "268"
        artist = "Awanqi (Angela Wang)"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c91ac3f2-9fcd-4b41-a168-ae7f70b67d3c.jpg?1764121966"
    }
}
